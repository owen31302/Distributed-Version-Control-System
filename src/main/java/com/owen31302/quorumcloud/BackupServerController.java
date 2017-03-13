package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/12/17.
 */
public class BackupServerController {
    public static void main(String[] args){
        UIFSM fsm  = UIFSM.IDLE;
        String msg;
        Scanner userInput = new Scanner(System.in);
        ObjectOutputStream oos;
        ObjectInputStream ois;
        Socket serverSocket;

        Thread[] backupServers = new Thread[5];
        for(int i = 0; i<backupServers.length; i++){
            backupServers[i] = new Thread(new BackupServer(i));
            backupServers[i].start();
        }

        try{
            while (true){
                switch (fsm){
                    case IDLE:
                        msg = "Please select the option:\n" +
                                "Enter 1 to perform CORRUPT_TIMESTAMP\n" +
                                "Enter 2 to perform CORRUPT_VALUE\n" +
                                "Enter 3 to perform SHUTDOWN\n";

                        System.out.print(msg);
                        int userChoice = Integer.parseInt(userInput.next());
                        if(userChoice == 1){
                            fsm = UIFSM.CORRUPT_TIMESTAMP;
                        }else if(userChoice == 2){
                            fsm = UIFSM.CORRUPT_VALUE;
                        }else  if(userChoice == 3){
                            fsm = UIFSM.SHUTDOWN;
                        }else{
                            msg = "Please enter 1-3.\n";
                            System.out.print(msg);
                        }
                        break;
                    case CORRUPT_TIMESTAMP:
                        int port = ((int)(Math.random()*10))%6;
                        serverSocket = new Socket("localhost", 10000 + port);
                        msg = "CORRUPT_TIMESTAMP at port: " + 1000+port +"\n";
                        System.out.print(msg);
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        oos.writeInt(RequestType.CORRUPT_TIMESTAMP);
                        oos.flush();
                        oos.close();
                        serverSocket.close();
                        fsm = UIFSM.IDLE;
                        break;
                    case CORRUPT_VALUE:
                        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> fakeGit = getCorruptHashMap();
                        port = ((int)(Math.random()*10))%6;
                        serverSocket = new Socket("localhost", 10000 + port);
                        msg = "CORRUPT_VALUE at port: " + 1000+port +"\n";
                        System.out.print(msg);
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        oos.writeInt(RequestType.CORRUPT_VALUE);
                        oos.writeObject(fakeGit);
                        oos.flush();
                        oos.close();
                        serverSocket.close();
                        fsm = UIFSM.IDLE;
                        break;
                    case SHUTDOWN:
                        port = ((int)(Math.random()*10))%6;
                        serverSocket = new Socket("localhost", 10000 + port);
                        msg = "SHUTDOWN at port: " + 1000+port +"\n";
                        System.out.print(msg);
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        oos.writeInt(RequestType.SHUTDOWN);
                        oos.flush();
                        oos.close();
                        serverSocket.close();
                        fsm = UIFSM.IDLE;
                        break;
                }
            }
        }catch (java.io.IOException e){
            System.out.print("IOException: " + e.toString() +"\n");
        }
    }

    public static ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> getCorruptHashMap(){
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
        int random = ((int) Math.random()*10)%2;
        if(random == 1){
            return git1;
        }else{
            return git2;
        }

    }
}
