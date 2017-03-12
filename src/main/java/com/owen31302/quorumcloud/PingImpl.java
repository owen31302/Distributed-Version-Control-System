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
 */
public class PingImpl implements Runnable {

    public void run() {
        HashSet<Integer> randomPorts = null;
        Socket serverSocket;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        ArrayList<Long> timestamps;
        ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>> listGit;

        while (true){
            try{
                // --- When MetaServer starts, it will asking all Read Quorum for the latest git file
                // - Step1: Check if the latest timestamp exists.
                //           No, set them all
                //           Yes, go to Step2
                // - Step2: Check if these git file (with the latest timestamp) contains corrupted content
                // - Step2.1: if there is only one latest file and its content is different from MetaServer
                //             Yes, ping all.
                //             No, (same as MetaServer) set the rest of the BackupSever
                // - Step2.2: if there is two latest file, then we can get the correct git file from at least two same copy
                // - Step2.2.1: if one file is corrupted, then, will find the correct one, set the wrong ones
                // - Step2.2.2: if both of them are the same, set the third one.
                // - Step2.3: if all three are the same as MetaServer, do nothing.
                //             Otherwise, update the different one.
                // - Step3: Recover the shutdown server
                listGit = new ArrayList<ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>>();
                timestamps = new ArrayList<Long>();
                for (HostPort port : HostPort.values()) {
                    if( randomPorts.contains( port.getValue()) && MetaServer.hostAvailabilityCheck(port.getValue())){
                        serverSocket = new Socket("localhost", port.getValue());
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        ois = new ObjectInputStream(serverSocket.getInputStream());
                        oos.writeInt(RequestType.INITIALRETRIEVE);
                        oos.flush();
                        timestamps.add(ois.readLong());
                        listGit.add((ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)ois.readObject());
                        ois.close();
                        oos.close();
                        serverSocket.close();
                    }
                }


                Thread.sleep(1000);

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
