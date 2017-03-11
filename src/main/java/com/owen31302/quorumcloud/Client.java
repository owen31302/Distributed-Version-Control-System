package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

/**
 * Created by owen on 3/10/17.
 */
public class Client {
    public static void main(String[] args){
        Scanner userInput = new Scanner(System.in);
        UIFSM fsm  = UIFSM.IDLE;
        Stack<LinkedList<VersionData>> files = null;
        String fileName;
        String msg;
        ObjectOutputStream oos;
        ObjectInputStream ois;

        while(true){
            switch (fsm){
                case IDLE:
                    msg = "Please select the option:\n" +
                            "Enter 1 to perform GET\n" +
                            "Enter 2 to perform WRITE\n" +
                            "Enter 3 to perform PUSH\n" +
                            "Enter 4 to perform PRINTALL\n";

                    System.out.print(msg);
                    int userChoice = Integer.parseInt(userInput.next());
                    if(userChoice == 1){
                        fsm = UIFSM.GET;
                    }else if (userChoice == 2){
                        fsm = UIFSM.WRITE;
                    }else if (userChoice == 3){
                        fsm = UIFSM.PUSH;
                    }else if (userChoice == 4){
                        fsm = UIFSM.PRINTALL;
                    }else{
                        System.out.print("Please enter 1 - 4.\n");
                    }
                    break;
                case GET:
                    try{
                        Socket serverSocket = new Socket("localhost", 12345);
                        msg = "Please enter the filename:\n";
                        System.out.print(msg);

                        String filename = userInput.next();

                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        ois = new ObjectInputStream(serverSocket.getInputStream());
                        oos.writeInt(RequestType.GET);
                        oos.writeUTF(filename);
                        oos.flush();
                        if(ois.readBoolean()){
                            files = (Stack<LinkedList<VersionData>>)ois.readObject();
                            System.out.print("File name existed. \n");
                        }else{
                            System.out.print("File does not exist!\n");
                        }
                    }catch (java.io.IOException e){
                        System.out.print("IOException\n");
                    }catch (java.lang.ClassNotFoundException e){
                        System.out.print("ClassNotFoundException\n");
                    }
                    fsm = UIFSM.IDLE;
                    break;
                case WRITE:
                    /*msg = "SET: Please enter the name of the file:\n";
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
                    }*/
                    break;
                case PUSH:
                    break;
                case PRINTALL:
                    int cnt = 1;
                    for(List<VersionData> file:files){
                        for (int i = 0; i<files.size(); i++){
                            System.out.print("Version " + cnt + " Value: " + file.get(i).get_val() +" , " +
                                    "Timestamp: " +file.get(i).get_timestamp() +
                                    "\n");
                        }
                        cnt++;
                    }
                    fsm = UIFSM.IDLE;
                    break;
            }
        }
    }
}
