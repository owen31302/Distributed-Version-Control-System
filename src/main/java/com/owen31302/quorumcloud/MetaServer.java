package com.owen31302.quorumcloud;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/9/17.
 */
public class MetaServer implements Serializable {
    public static void main(String[] args){
        Scanner userInput = new Scanner(System.in);
        UIFSM fsm  = UIFSM.GET;
        String msg;
        String filename;

        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit;
        ArrayList<Long> timestamps;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        HashSet<Integer> randomPorts;

        // --- Communication between MetaServer and BackupServer
        // -IDLE: MetaServer wait for client request & ping the BackupServer cluster for restore server when crash exist(When BackupServer doesn't response)
        // -GET: MetaServer send the request for the latest version in the BackupServer when MetaServer crashes amd need to reboot a new MetaServer.
        // -SET: MetaServer send the latest modified version to the BackupServer
        while(true){
            switch (fsm){
                case IDLE:
                    msg = "Please select the option:\n" +
                            "Enter 1 to perform GET\n" +
                            "Enter 2 to perform SET\n" +
                            "Enter 3 to perform PRINTALL\n";

                    System.out.print(msg);
                    int userChoice = Integer.parseInt(userInput.next());
                    if(userChoice == 1){
                        fsm = UIFSM.GET;
                    }else if (userChoice == 2){
                        fsm = UIFSM.SET;
                    }else if(userChoice == 3){
                        fsm = UIFSM.PRINTALL;
                    }else{
                        System.out.print("Please enter 1 or 2 or 3.\n");
                    }
                    break;
                case GET:
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
                                oos.writeInt(RequestType.GET);
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
                    fsm = UIFSM.IDLE;
                    break;
                case SET:
                    msg = "SET: Please enter the name of the file:\n";
                    System.out.print(msg);
                    filename = userInput.next();
                    msg = "Please enter the value\n";
                    System.out.print(msg);
                    int value = userInput.nextInt();
                    long timestamp = System.currentTimeMillis();
                    randomPorts = RandomPorts(HostPort.count, false);
                    for (HostPort port : HostPort.values()) {
                        if( randomPorts.contains( port.getValue()) && hostAvailabilityCheck(port.getValue())){
                            try{
                                Socket serverSocket = new Socket("localhost", port.getValue());
                                oos = new ObjectOutputStream(serverSocket.getOutputStream());
                                oos.writeInt(RequestType.SET);
                                oos.writeLong(timestamp);

                                VersionData data = new VersionData(value, timestamp);
                                LinkedList<VersionData> datas = new LinkedList<VersionData>();
                                datas.add(data);
                                Stack<LinkedList<VersionData>> versionDatas = new Stack<LinkedList<VersionData>>();
                                versionDatas.add(datas);
                                git.put(filename, versionDatas);
                                oos.writeObject(git);

                                oos.close();
                                serverSocket.close();
                            }catch (java.io.IOException e){
                                System.out.print("Can not send msg to " + port.getValue() + "\n");
                            }
                        }
                    }
                    fsm = UIFSM.IDLE;
                    break;
                case PRINTALL:
                    MetaServer.printAllGit(git);
                    fsm = UIFSM.IDLE;
                    break;
            }
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
