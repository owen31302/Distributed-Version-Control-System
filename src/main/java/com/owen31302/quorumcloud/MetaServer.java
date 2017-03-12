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
public class MetaServer implements Serializable,TestCase {
    private static ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> _git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
    private static TimeStamp _timestamp = new TimeStamp(new Long(0));

    public static void main(String[] args){
        String msg;
        String filename;
        int metaServerPort = 12345;

        // --- Set ConcurrentHashMap as version control database

        ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit;
        ArrayList<Long> timestamps;
        ObjectOutputStream oos;
        ObjectInputStream ois;

        // -- MetaServer Setup
        msg = "GET the git from the BackupServer.\n";
        System.out.print(msg);
        listGit = new ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>>();
        timestamps = new ArrayList<Long>();

        // --- When MetaServer starts, it will asking all Quorum for the latest git file
        for (HostPort port : HostPort.values()) {
            if( hostAvailabilityCheck(port.getValue())){
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
            }
        }
        _git = checkLatestGit(timestamps, listGit, _timestamp);
        if(_git == null){
            _git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
            _timestamp.set_time(System.currentTimeMillis());
        }
        System.out.print("Print all git\n");
        System.out.print("Timestamp: " + _timestamp.get_time()+ "\n");
        printAllGit(_git);

        // --- Start ping logic
        Thread pingThread = new Thread(new PingImpl());
        pingThread.start();

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
                        if(_git.containsKey(filename)){
                            System.out.print("File name existed. \n");
                            oos.writeBoolean(true);
                            oos.writeObject(_git.get(filename));
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
                        if(_git.containsKey(filename)){
                            System.out.print("Update a version list.\n");

                            // --- Check if the new list can be accepted to save to the main branch.
                            // --- Use iterator to if new list contains all of the latest nodes.
                            Iterator latestIterator = _git.get(filename).iterator();
                            Iterator newIterator = versionList.iterator();
                            boolean mismatch = false;
                            while (latestIterator.hasNext() && newIterator.hasNext()){
                                LinkedList<VersionData> newList = (LinkedList<VersionData>)newIterator.next();
                                LinkedList<VersionData> latestList = (LinkedList<VersionData>)latestIterator.next();
                                if(!(newList.getFirst().get_val() == latestList.getFirst().get_val())){
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
                                _git.put(filename, versionList);

                                // --- Push to BackupServer
                                pushToBackupServer(_timestamp, _git);
                                oos.writeBoolean(true);
                                oos.flush();
                                oos.close();
                            }
                        }else {
                            System.out.print("Got a new version list.\n");
                            _git.put(filename, versionList);
                            oos.writeBoolean(true);
                            oos.flush();

                            // --- Push to BackupServer
                            pushToBackupServer(_timestamp, _git);
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

    // --- hostAvailabilityCheck is to check the status of the server
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

    // --- checkLatestGit is to get the latest git file
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

    // --- printAllGit is to print all the files in the git file
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
                for (int i = 0; i<version.size(); i++){
                    System.out.print("Version " + cnt + " Value: " + version.get(i).get_val()+
                            //"Timestamp: " +version.get(i).get_timestamp() +
                            "\n");
                }
                cnt++;
            }
        }
    }

    public static void Test(){
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git1 = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git2 = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        VersionData v1 = new VersionData(11);
        VersionData v2 = new VersionData(12);
        VersionData v3 = new VersionData(12);
        LinkedList<VersionData> version1  = new LinkedList<VersionData>();
        version1.add(v1);
        version1.add(v2);
        LinkedList<VersionData> version2  = new LinkedList<VersionData>();
        version2.add(v3);
        Stack<LinkedList<VersionData>> versions1 = new Stack<LinkedList<VersionData>>();
        versions1.push(version1);
        versions1.push(version2);
        git1.put("owen", versions1);

        v1 = new VersionData(11);
        v3 = new VersionData(13);
        version1  = new LinkedList<VersionData>();
        version1.add(v1);
        version2  = new LinkedList<VersionData>();
        version2.add(v3);
        versions1 = new Stack<LinkedList<VersionData>>();
        versions1.push(version1);
        versions1.push(version2);

        git2.put("owen", versions1);
        if(gitMatch(git1, git2)){
            System.out.print("YA\n");
        }else{
            System.out.print("No\n");
        }
    }

    public static boolean gitMatch(ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git1, ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git2){
        if(git1.size()!=git2.size()){
            return false;
        }
        for(int i =0; i<git1.keySet().size(); i++){
            if(!git1.keySet().toArray()[i].equals(git2.keySet().toArray()[i])){
                return false;
            }
        }
        for(String filename: git1.keySet()){
            Stack<LinkedList<VersionData>> versions1 = git1.get(filename);
            Stack<LinkedList<VersionData>> versions2 = git2.get(filename);
            if(versions1.size() != versions2.size()){
                return false;
            }
            Iterator stackIter1 = versions1.iterator();
            Iterator stackIter2 = versions2.iterator();
            while(stackIter1.hasNext()){
                LinkedList<VersionData> version1 = (LinkedList<VersionData>)stackIter1.next();
                LinkedList<VersionData> version2 = (LinkedList<VersionData>)stackIter2.next();
                if(version1.size() != version2.size()){
                    return false;
                }
                Iterator versionData1 = version1.iterator();
                Iterator versionData2 = version2.iterator();
                while (versionData1.hasNext()){
                    VersionData data1 = (VersionData)versionData1.next();
                    VersionData data2 = (VersionData)versionData2.next();
                    if(data1.get_val() != data2.get_val()){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // --- RandomPorts is to randomly choose half ports in the network
    // --- Write quorum needs at least n/2 +1
    // --- Read quorum needs at least n/2
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

    // --- pushToBackupServer is to push the current git file to the BackupServer
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

    public ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> get_git(){
        return _git;
    }

    public void set_git(ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git){
        _git = git;
    }

    public TimeStamp get_timestamp(){
        return _timestamp;
    }

    public void set_timestamp(TimeStamp timestamp){
        _timestamp = timestamp;
    }

    public void corruptValue() {

    }

    public void corruptTimestamp() {

    }

    public void shutDown() {

    }
}
