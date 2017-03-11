package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/**
 * Created by owen on 3/10/17.
 */
public class Client {
    public static void main(String[] args){
        Scanner userInput = new Scanner(System.in);
        UIFSM fsm  = UIFSM.IDLE;
        Stack<LinkedList<VersionData>> files = null;
        String fileName = null;
        String msg;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        Socket serverSocket;
        int metaServerPort = 12345;
        boolean metaServer = false;
        int userChoice;

        if (MetaServer.hostAvailabilityCheck(metaServerPort)){
            metaServer = true;
        }else{
            msg = "MetaServer is not responding.\nPlease contact the administrator or check the settings again. \n";
            System.out.print(msg);
        }

        while(metaServer){
            switch (fsm){
                case IDLE:
                    msg = "Please select the option:\n" +
                            "Enter 1 to perform GET\n" +
                            "Enter 2 to perform WRITE\n" +
                            "Enter 3 to perform PUSH\n" +
                            "Enter 4 to perform PRINTALL\n" +
                            "Enter 5 to perform MERGE\n";

                    System.out.print(msg);
                    userChoice = Integer.parseInt(userInput.next());
                    if(userChoice == 1){
                        fsm = UIFSM.GET;
                    }else if (userChoice == 2){
                        fsm = UIFSM.WRITE;
                    }else if (userChoice == 3){
                        if(fileName != null){
                            fsm = UIFSM.PUSH;
                        }else{
                            msg = "There is no file.\nPlease create a file or GET from the server\n";
                            System.out.print(msg);
                        }
                    }else if (userChoice == 4){
                        fsm = UIFSM.PRINTALL;
                    }else if (userChoice == 5){
                        fsm = UIFSM.MERGE;
                    }else{
                        System.out.print("Please enter 1 - 4.\n");
                    }
                    break;
                case GET:
                    try{
                        serverSocket = new Socket("localhost", metaServerPort);
                        msg = "Please enter the filename:\n";
                        System.out.print(msg);
                        String serverFileName = userInput.next();

                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        ois = new ObjectInputStream(serverSocket.getInputStream());
                        oos.writeInt(RequestType.GET);
                        oos.writeUTF(serverFileName);
                        oos.flush();
                        if(ois.readBoolean()){
                            files = (Stack<LinkedList<VersionData>>)ois.readObject();
                            fileName = serverFileName;
                            System.out.print("File name existed. \n");
                        }else{
                            System.out.print("File does not exist!\n");
                        }
                        oos.close();
                        ois.close();
                        serverSocket.close();
                    }catch (java.io.IOException e){
                        System.out.print("IOException\n");
                    }catch (java.lang.ClassNotFoundException e){
                        System.out.print("ClassNotFoundException\n");
                    }
                    fsm = UIFSM.IDLE;
                    break;
                case WRITE:
                    if(fileName == null){
                        msg = "New file.\n";
                        System.out.print(msg);
                        userChoice = 0;
                    }else{
                        msg = "0 New a file.\n1 Write existed file\n";
                        System.out.print(msg);
                        userChoice = userInput.nextInt();
                    }
                    switch (userChoice){
                        case 0:
                            msg = "Please enter the file name\n";
                            System.out.print(msg);
                            fileName = userInput.next();
                            msg = "Please enter the value\n";
                            System.out.print(msg);
                            int value = userInput.nextInt();

                            // --- Setup data
                            VersionData data = new VersionData(value);
                            LinkedList<VersionData> list = new LinkedList<VersionData>();
                            list.add(data);
                            files = new Stack<LinkedList<VersionData>>();
                            files.push(list);

                            fsm = UIFSM.IDLE;
                            break;
                        case 1:

                            msg = "Please enter the value\n";
                            System.out.print(msg);
                            value = userInput.nextInt();

                            // --- Setup data
                            data = new VersionData(value);
                            list = new LinkedList<VersionData>();
                            list.add(data);
                            files.push(list);

                            fsm = UIFSM.IDLE;
                            break;
                        default:
                            msg = "Please enter 0 or 1.\n";
                            System.out.print(msg);
                            break;
                    }
                    break;
                case PUSH:
                    msg = "Pushing the file... \n";
                    System.out.print(msg);

                    try{
                        serverSocket = new Socket("localhost", metaServerPort);
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        oos.writeInt(RequestType.PUSH);
                        oos.writeUTF(fileName);
                        oos.writeObject(files);
                        oos.flush();

                        ois = new ObjectInputStream(serverSocket.getInputStream());
                        if(ois.readBoolean()){
                            msg = "Push success.\n";
                            System.out.print(msg);
                        }else{
                            msg = "Push fail.\n";
                            System.out.print(msg);
                        }
                        ois.close();
                        oos.close();
                        serverSocket.close();
                    }catch (java.io.IOException e){
                        System.out.print("IOException: " + e.toString() + "\n");
                    }
                    fsm = UIFSM.IDLE;
                    break;
                case PRINTALL:
                    if(files == null){
                        System.out.print("No file existed.\n");
                        fsm = UIFSM.IDLE;
                        break;
                    }

                    int cnt = 1;
                    for(List<VersionData> file:files){
                        for (int i = 0; i<file.size(); i++){
                            System.out.print("Version " + cnt + " Value: " + file.get(i).get_val() + "\n");
                        }
                        cnt++;
                    }
                    fsm = UIFSM.IDLE;
                    break;
                case MERGE:
                    try {
                        // --- Get the latest version from the server
                        serverSocket = new Socket("localhost", metaServerPort);
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        oos.writeInt(RequestType.GET);
                        oos.writeUTF(fileName);
                        oos.flush();

                        Stack<LinkedList<VersionData>> tempFiles = new Stack<LinkedList<VersionData>>();
                        ois = new ObjectInputStream(serverSocket.getInputStream());
                        if(ois.readBoolean()){
                            tempFiles = (Stack<LinkedList<VersionData>>)ois.readObject();
                        }else{
                            System.out.print("File does not exist!\n");
                            fsm = UIFSM.IDLE;
                            oos.close();
                            ois.close();
                            serverSocket.close();
                            break;
                        }
                        oos.close();
                        ois.close();
                        serverSocket.close();

                        // --- Merge step (Assume there is only one version difference)
                        // Find the breaking point and append the difference
                        // Create a latest version at the end
                        // Just append the value and choose one of them to be the latest
                        Iterator newIterator = files.iterator();
                        Iterator latestIterator = tempFiles.iterator();
                        LinkedList<VersionData> latestList = null;
                        LinkedList<VersionData> newList = null;
                        while (latestIterator.hasNext() && newIterator.hasNext()){
                            newList = (LinkedList<VersionData>)newIterator.next();
                            latestList = (LinkedList<VersionData>)latestIterator.next();
                            if(!(newList.getFirst().get_val() == latestList.getFirst().get_val())){
                                latestList.add(newList.get(0));
                            }
                        }
                        tempFiles.push(newList);
                        files = tempFiles;
                        msg = "Merge finished!\n";
                        System.out.print(msg);

                    }catch (java.io.IOException e){
                        System.out.print("IOException: " + e.toString() + "\n");
                    }catch (java.lang.ClassNotFoundException e){
                        System.out.print("ClassNotFoundException\n");
                    }
                    fsm = UIFSM.IDLE;
                    break;
            }
        }
    }
}
