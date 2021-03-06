package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/11/17.
 * This thread is for auto correction process:
 * 1. Fixing corrupted data.
 * 2. Help preserving the data for cashed BackupServer.
 */
public class PingImpl implements Runnable {

    public void run() {
        HashSet<Integer> randomPorts;
        Socket serverSocket;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        ArrayList<Long> timestamps;
        ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit;
        String msg;
        ArrayList<Integer> listPorts;
        HashSet<Integer> errorPorts;

        // --- This is for pinging process:
        // --- We randomly pick 3 port in this five node scenario.
        // --- If there is one identical git version as well as the timestamp,
        // --- we claimed that we have the latest data in cache (MetaServer)
        // --- Therefore, we can use the cached file to restore the corrupted data
        // --- On the other hand, if we can not find any matching git and timestamp
        // --- in this case, we have to ask all the node in this system to find the majority data
        while (true){
            if(!MetaServer.get_userPush()){
                try{
                    msg = "Pinging ... \n";
                    System.out.print(msg);
                    // ping random 3 ports, and save the gits and timestamps
                    listGit = new ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>>();
                    timestamps = new ArrayList<Long>();
                    randomPorts = MetaServer.RandomPorts(HostPort.count, false);
                    listPorts = new ArrayList<Integer>();
                    errorPorts = new HashSet<Integer>();

                    for (HostPort port : HostPort.values()) {
                        if( randomPorts.contains( port.getValue()) && MetaServer.hostAvailabilityCheck(port.getValue())){
                            //System.out.print("ports: " + port.getValue()+"\n");
                            serverSocket = new Socket("localhost", port.getValue());
                            oos = new ObjectOutputStream(serverSocket.getOutputStream());
                            ois = new ObjectInputStream(serverSocket.getInputStream());
                            oos.writeInt(RequestType.INITIALRETRIEVE);
                            oos.flush();
                            Long tempTime = ois.readLong();
                            Object tempObj = ois.readObject();
                            timestamps.add(tempTime);
                            listGit.add((ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)tempObj);
                            listPorts.add(port.getValue());
                            ois.close();
                            oos.close();
                            serverSocket.close();
                        }
                    }


                    // Try to find the match
                    int index = 0;
                    Long tempTimestamp = MetaServer.get_timestamp().get_time();
                    ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> tempGit = MetaServer.get_git();

                    for(ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git : listGit){
                        if(MetaServer.get_timestamp().get_time().equals(timestamps.get(index))  && MetaServer.gitMatch(MetaServer.get_git(), git)){
                            tempGit = git;
                            tempTimestamp = timestamps.get(index);
                        }else{
                            errorPorts.add(listPorts.get(index));
                        }
                        index++;
                    }


                    // If we can find the correct git & timestamp, update the rest of the data (1-2)
                    // There is no such matching, ping all and find the majority
                    if(errorPorts.size() != 3){
                        // --- correct
                        for (HostPort port : HostPort.values()) {
                            if(errorPorts.contains( port.getValue()) && MetaServer.hostAvailabilityCheck(port.getValue())){
                                Socket backupServerSocket = new Socket("localhost", port.getValue());
                                ObjectOutputStream oosBackupServer = new ObjectOutputStream(backupServerSocket.getOutputStream());
                                oosBackupServer.writeInt(RequestType.SET);
                                oosBackupServer.writeLong(tempTimestamp);
                                oosBackupServer.writeObject(tempGit);

                                oosBackupServer.close();
                                backupServerSocket.close();
                            }
                        }
                    }else{
                        listGit = new ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>>();
                        timestamps = new ArrayList<Long>();
                        for (HostPort port : HostPort.values()) {
                            if( MetaServer.hostAvailabilityCheck(port.getValue())){
                                serverSocket = new Socket("localhost", port.getValue());
                                oos = new ObjectOutputStream(serverSocket.getOutputStream());
                                ois = new ObjectInputStream(serverSocket.getInputStream());
                                oos.writeInt(RequestType.INITIALRETRIEVE);
                                oos.flush();
                                Long tempTime = ois.readLong();
                                Object tempObj = ois.readObject();
                                timestamps.add(tempTime);
                                listGit.add((ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)tempObj);
                                ois.close();
                                oos.close();
                                serverSocket.close();
                            }
                        }
                        timestamps.add(MetaServer.get_timestamp().get_time());
                        listGit.add(MetaServer.get_git());

                        System.out.print("listGit.size(): " + listGit.size()+"\n");
                        outerloop:
                        for(int i = 0; i<listGit.size()-1;i++){
                            int cnt = 0;
                            for(int j = i+1; j<listGit.size(); j++){
                                if( timestamps.get(i).equals(timestamps.get(j)) &&
                                        MetaServer.gitMatch(listGit.get(i), listGit.get(j))){
                                    cnt++;
                                    if(cnt == 2){
                                        MetaServer.set_git(listGit.get(i));
                                        MetaServer.set_timestamp(new TimeStamp(timestamps.get(i)));
                                        System.out.print("Find the correct git!\n");
                                        System.out.print("MetaServer TIMESTAMP fixing: "+ MetaServer.get_timestamp().get_time()+"\n");
                                        System.out.print("MetaServer Git fixing: \n");
                                        MetaServer.printAllGit(MetaServer.get_git());
                                        pushToBackupServer(new TimeStamp(timestamps.get(i)), listGit.get(i));
                                        break outerloop;
                                    }
                                }
                            }
                        }
                    }

                    Thread.sleep(5000);

                }catch (java.lang.InterruptedException e){
                    System.out.print("InterruptedException: " + e.toString() + "\n");
                }catch (java.io.IOException e){
                    System.out.print("Can not send msg to " + e.toString() + "\n");
                }catch (java.lang.ClassNotFoundException e){
                    System.out.print("ClassNotFoundException. \n");
                }
            }
        }
    }

    // --- pushToBackupServer is to push the current git file to the BackupServer
    public static void pushToBackupServer(TimeStamp timestamp, ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git){
        for (HostPort port : HostPort.values()) {
            if (MetaServer.hostAvailabilityCheck(port.getValue())) {
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
