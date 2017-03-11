package com.owen31302.quorumcloud;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/9/17.
 */
public class MetaServer implements Serializable {
    public static void main(String[] args){

        UIFSM fsm  = UIFSM.INITIALRETRIEVE;
        TimeStamp timestamp = new TimeStamp(new Long(0));
        String msg;
        String filename;
        int metaServerPort = 12345;

        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit;
        ArrayList<Long> timestamps;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        HashSet<Integer> randomPorts;
        ObjectOutputStream oosBackupServer;

        // -- MetaServer Setup
        msg = "GET the git from the BackupServer.\n";
        System.out.print(msg);
        listGit = new ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>>();
        timestamps = new ArrayList<Long>();

        // --- Select half of the random ports and get their value
        randomPorts = RandomPorts(HostPort.count, false);
        for (HostPort port : HostPort.values()) {
            if( randomPorts.contains( port.getValue()) && hostAvailabilityCheck(port.getValue())){
                try{
                    Socket serverSocket = new Socket("localhost", port.getValue());
                    oos = new ObjectOutputStream(serverSocket.getOutputStream());
                    ois = new ObjectInputStream(serverSocket.getInputStream());
                    oos.writeInt(RequestType.INITIALRETRIEVE);
                    oos.flush();
                    timestamps.add(ois.readLong());
                    listGit.add((ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)ois.readObject());
                    ois.close();
                    oos.close();
                    serverSocket.close();
                }catch (java.io.IOException e){
                    System.out.print("Can not send msg to " + port.getValue() + "\n");
                }catch (java.lang.ClassNotFoundException e){
                    System.out.print("ClassNotFoundException. \n");
                }
            }else{
                System.out.print("Cannot connect to " + port.getValue() + "\n");
            }
        }
        git = checkLatestGit(timestamps, listGit, timestamp);
        if(git == null){
            git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
            timestamp.set_time(System.currentTimeMillis());
        }
        System.out.print("Print all git\n");
        System.out.print("Timestamp: " + timestamp.get_time()+ "\n");
        printAllGit(git);

        // --- MetaServer is for handing user request and save the latest version list to the BackupServer
        // --- @GET: Send the whole list of versions to Client
        // --- @PUSH: Received push request, check if the version is the latest one, otherwise, reject the update
        System.out.print("Server ini process.\n");
        try {
            ServerSocket serverSocket = new ServerSocket(metaServerPort);
            while(!serverSocket.isClosed()){
                // Wait and accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.print("I got a client\n");

                // Get a communication stream associated with the socket
                ois = new ObjectInputStream(clientSocket.getInputStream());
                oos = new ObjectOutputStream(clientSocket.getOutputStream());

                int action = ois.readInt();
                switch (action){
                    case RequestType.CHECKCONNECTION:
                        break;

                    case RequestType.GET:
                        filename = ois.readUTF();
                        if(git.containsKey(filename)){
                            System.out.print("File name existed. \n");
                            oos.writeBoolean(true);
                            oos.writeObject(git.get(filename));
                            oos.flush();
                            oos.close();
                            clientSocket.close();
                        }else{
                            System.out.print("No such file name existed. \n");
                            oos.writeBoolean(false);
                            oos.flush();
                            oos.close();
                            clientSocket.close();
                        }
                        break;
                    case RequestType.PUSH:
                        filename = ois.readUTF();
                        Stack<LinkedList<VersionData>> versionList = (Stack<LinkedList<VersionData>>)ois.readObject();
                        if(git.containsKey(filename)){
                            System.out.print("Update a version list.\n");

                            // --- Check if the new list can be accepted to save to the main branch.
                            // --- Use iterator to if new list contains all of the latest nodes.
                            Iterator latestIterator = git.get(filename).iterator();
                            Iterator newIterator = versionList.iterator();
                            boolean mismatch = false;
                            while (latestIterator.hasNext() && newIterator.hasNext()){
                                LinkedList<VersionData> newList = (LinkedList<VersionData>)newIterator.next();
                                LinkedList<VersionData> latestList = (LinkedList<VersionData>)newIterator.next();
                                if(!(newList.getFirst().get_timestamp() == latestList.getFirst().get_timestamp())){
                                    mismatch = true;
                                    break;
                                }
                            }
                            if(mismatch){
                                System.out.print("Version conflict.\n");
                                oos.writeBoolean(false);
                                oos.flush();
                                oos.close();
                            }else{
                                System.out.print("Pushed to the main branch.\n");
                                git.put(filename, versionList);

                                // --- Push to BackupServer
                                pushToBackupServer(timestamp, git);
                            }
                        }else {
                            System.out.print("Got a new version list.\n");
                            git.put(filename, versionList);
                            oos.writeBoolean(true);
                            oos.flush();

                            // --- Push to BackupServer
                            pushToBackupServer(timestamp, git);
                        }
                        break;
                }
            }
        }catch (java.io.IOException e){
            System.out.print("IOException: " + e.toString() +"\n");
        }catch (java.lang.ClassNotFoundException e){
            System.out.print("ClassNotFoundException: " + e.toString() +"\n");
        }
    }

    public static boolean hostAvailabilityCheck(int port)
    {
        Socket s;
        try {
            s = new Socket("localhost", port);
            if (s.isConnected())
            {
                ObjectOutputStream dos = new ObjectOutputStream(s.getOutputStream());
                dos.writeInt(RequestType.CHECKCONNECTION);
                dos.close();
                s.close();
            }
        }catch (UnknownHostException e){
            return false;
        }catch (IOException e) {
            // io exception, service probably not running
            return false;
        }catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public static ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> checkLatestGit(ArrayList<Long> timestamps, ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit, TimeStamp timestamp){
        if(listGit.size() == 0){
            return null;
        }else if(listGit.size() == 1){
            return listGit.get(0);
        }

        int index = 0;
        for(int i = 0; i<listGit.size(); i++){
            if(timestamps.get(i) > timestamp.get_time()){
                timestamp.set_time(timestamps.get(i));
                index = i;
            }
        }

        return listGit.get(index);
    }

    public static void printAllGit(ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git){
        for(Map.Entry files:git.entrySet()){
            System.out.print("Filename: " + files.getKey() + "\n");

            Object obj2 = files.getValue();
            Stack<LinkedList<VersionData>> versions = null;
            if(obj2 instanceof Stack){
                versions = (Stack<LinkedList<VersionData>>)obj2;
            }else{
                System.out.print("Stack conversion error.\n");
            }

            int cnt = 1;
            for(List<VersionData> version:versions){
                for (int i = 0; i<versions.size(); i++){
                    System.out.print("Version " + cnt + " Value: " + version.get(i).get_val()+
                            //"Timestamp: " +version.get(i).get_timestamp() +
                            "\n");
                }
                cnt++;
            }
        }
    }

    public static HashSet<Integer> RandomPorts(int size, boolean write){
        int half;
        if(write){
            half = size/2 +1;
        }else{
            half = size/2 + size%2;
        }

        HashSet<Integer> nums = new HashSet<Integer>((int)(Math.random()*10)%size);

        while(nums.size()<half){
            nums.add(10000 + (int)(Math.random()*10)%size);
        }
        return nums;
    }

    public static void pushToBackupServer(TimeStamp timestamp, ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git){
        timestamp.set_time(System.currentTimeMillis());
        HashSet<Integer> randomPorts = RandomPorts(HostPort.count, true);
        for (HostPort port : HostPort.values()) {
            if (randomPorts.contains(port.getValue()) && hostAvailabilityCheck(port.getValue())) {
                try {
                    Socket backupServerSocket = new Socket("localhost", port.getValue());
                    ObjectOutputStream oosBackupServer = new ObjectOutputStream(backupServerSocket.getOutputStream());
                    oosBackupServer.writeInt(RequestType.SET);
                    oosBackupServer.writeLong(timestamp.get_time());
                    oosBackupServer.writeObject(git);

                    oosBackupServer.close();
                    backupServerSocket.close();
                } catch (java.io.IOException e) {
                    System.out.print("Can not send msg to " + port.getValue() + "\n");
                }
            }
        }
    }
}