package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;

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
                        serverSocket = new Socket("localhost", 10000);
                        msg = "CORRUPT_TIMESTAMP\n";
                        System.out.print(msg);
                        oos = new ObjectOutputStream(serverSocket.getOutputStream());
                        oos.writeInt(RequestType.CORRUPT_TIMESTAMP);
                        oos.flush();
                        oos.close();
                        serverSocket.close();
                        fsm = UIFSM.IDLE;
                        break;
                    case CORRUPT_VALUE:
                        fsm = UIFSM.IDLE;
                        break;
                    case SHUTDOWN:
                        fsm = UIFSM.IDLE;
                        break;
                }
            }
        }catch (java.io.IOException e){
            System.out.print("IOException: " + e.toString() +"\n");
        }
        //catch (java.lang.ClassNotFoundException e){
        //    System.out.print("ClassNotFoundException: " + e.toString() +"\n");
        //}
    }
}
