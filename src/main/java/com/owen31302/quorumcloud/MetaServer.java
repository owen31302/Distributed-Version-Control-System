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
        String msg;
        String filename;

        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit;
        ArrayList<Long> timestamps;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        HashSet<Integer> randomPorts;

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
            git = checkLatestGit(timestamps, listGit);
        }
        for(String key : git.keySet()){
            System.out.print("Key: " + key + "\n");
        }
        System.out.print("Git size: " + git.size()+ "\n");

        int serverPort = 12345;
        System.out.print("Server ini process.\n");
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while(!serverSocket.isClosed()){
                // Wait and accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.print("I got a client\n");

                // Get a communication stream associated with the socket
                ois = new ObjectInputStream(clientSocket.getInputStream());
                oos = new ObjectOutputStream(clientSocket.getOutputStream());

                // --- @GET: Send the whole list of versions to Client
                // --- @PUSH: Received push request, check if the version is the latest one, otherwise, reject the update
                System.out.print("Q1\n");
                int action = ois.readInt();
                System.out.print("Q2\n");
                switch (action){
                    case RequestType.GET:
                        System.out.print("Q3\n");
                        filename = ois.readUTF();
                        System.out.print("filename: "+ filename +"\n");
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
                        break;
                }
            }
        }catch (java.io.IOException e){

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
        }
        catch (UnknownHostException e)
        {
            return false;
        }
        catch (IOException e) {
            // io exception, service probably not running
            return false;
        }
        catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public static ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> checkLatestGit(ArrayList<Long> timestamps, ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit){
        if(listGit.size() == 0){
            return new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        }else if(listGit.size() == 1){
            return listGit.get(0);
        }

        Long maxTimestamp = new Long(0);
        int index = 0;
        for(int i = 0; i<listGit.size(); i++){
            if(timestamps.get(i) > maxTimestamp){
                maxTimestamp = timestamps.get(i);
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
                    System.out.print("Version " + cnt + " Value: " + version.get(i).get_val() +" , " +
                            "Timestamp: " +version.get(i).get_timestamp() +
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
}
