package com.owen31302.quorumcloud;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/9/17.
 */
public class MetaServer implements Serializable {
    public static void main(String[] args){
        Scanner userInput = new Scanner(System.in);
        UIFSM fsm  = UIFSM.IDLE;
        String msg;
        String filename;

        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<List<VersionData>>> git = new ConcurrentHashMap<String, Stack<List<VersionData>>>();
        ObjectOutputStream oos;
        ObjectInputStream ois;

        // --- Communication between MetaServer and BackupServer
        // -IDLE: MetaServer wait for client request & ping the BackupServer cluster for restore server when crash exist(When BackupServer doesn't response)
        // -GET: MetaServer send the request for the latest version in the BackupServer when MetaServer crashes amd need to reboot a new MetaServer.
        // -SET: MetaServer send the latest modified version to the BackupServer
        while(true){
            switch (fsm){
                case IDLE:
                    msg = "Please select the option:\n" +
                            "Enter 1 to perform GET\n" +
                            "Enter 2 to perform SET\n";
                    System.out.print(msg);
                    String userChoice = userInput.next();
                    if(Integer.parseInt(userChoice) == 1){
                        fsm = UIFSM.GET;
                    }else if (Integer.parseInt(userChoice) == 2){
                        fsm = UIFSM.SET;
                    }else{
                        System.out.print("Please enter 1 or 2.\n");
                    }
                    break;
                case GET:
                    msg = "GET: Please enter the name of the file:\n";
                    System.out.print(msg);
                    filename = userInput.next();
                    for (HostPort port : HostPort.values()) {
                        if( hostAvailabilityCheck(port.getValue())){
                            try{
                                Socket serverSocket = new Socket("localhost", port.getValue());
                                oos = new ObjectOutputStream(serverSocket.getOutputStream());
                                oos.writeInt(RequestType.GET);
                                oos.writeUTF(String.valueOf(filename));

                                ois = new ObjectInputStream(serverSocket.getInputStream());
                                Long returnTimestamp = ois.readLong();

                                ois.close();
                                oos.close();
                                serverSocket.close();

                                System.out.print("Received from port " + port.getValue() + ", and timestamp is "+returnTimestamp+"\n");
                            }catch (java.io.IOException e){
                                System.out.print("Can not send msg to " + port.getValue() + "\n");
                            }
                        }else{
                            System.out.print("Cannot connect to " + port.getValue() + "\n");
                        }
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
                    for (HostPort port : HostPort.values()) {
                        if( hostAvailabilityCheck(port.getValue())){
                            try{
                                Socket serverSocket = new Socket("localhost", port.getValue());
                                oos = new ObjectOutputStream(serverSocket.getOutputStream());
                                oos.writeInt(RequestType.SET);

                                VersionData data = new VersionData(value, timestamp);
                                List<VersionData> datas = new LinkedList<VersionData>();
                                datas.add(data);
                                Stack<List<VersionData>> versionDatas = new Stack<List<VersionData>>();
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
}
