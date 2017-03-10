package com.owen31302.quorumcloud;

import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by owen on 3/8/17.
 */
public class SetupServer {


    public static void main(String[] args) throws InterruptedException {
        int _serverPort = 10000+Integer.parseInt(args[0]);

        // --- Setup Server --- //
        System.out.print("Server ini process.\n");
        try {
            // Register service on specific port
            ServerSocket serverSocket = new ServerSocket(_serverPort);
            Thread dispatcherThread = new Thread(new Dispatcher(serverSocket));
            dispatcherThread.start();
        }catch (java.io.IOException e){
            System.out.print("Server error: "+e.toString()+"\n");
        }


        // --- Setup Client --- //
        Scanner userInput = new Scanner(System.in);
        String userChoice;
        String msg;
        UIFSM fsm  = UIFSM.IDLE;
        while(true){
            switch (fsm){
                case IDLE:
                    msg = "Please select the option:\n" +
                            "Enter 1 to perform GET\n" +
                            "Enter 2 to perform SET\n" +
                            "Enter 3 to perform PRINTALL\n";
                    System.out.print(msg);
                    userChoice = userInput.next();
                    if(Integer.parseInt(userChoice) == 1){
                        fsm = UIFSM.GET;
                    }else if (Integer.parseInt(userChoice) == 2){
                        fsm = UIFSM.SET;
                    }else if(Integer.parseInt(userChoice) == 3){
                        if(ServerWorker._dataFile.isEmpty()){
                            System.out.print("There is no files in this computer.\n");
                        }else{
                            fsm = UIFSM.PRINTALL;
                        }
                    }else{
                        System.out.print("Please enter 1 or 2.\n");
                    }
                    break;
                case GET:
                    msg = "Please enter the name of the file, or enter -1 back to the main menu:\n";
                    System.out.print(msg);
                    String filename = userInput.next();
                    if (Integer.parseInt(filename) == -1){
                        fsm = UIFSM.IDLE;
                    }else if(ServerWorker._dataFile.contains(filename)){
                        // TODO
                    }else{
                        msg = "File does not exist in the system.\n";
                        System.out.print(msg);
                    }
                    break;
                case SET:
                    msg = "Please enter the name of the file, or enter -1 back to the main menu:\n";
                    System.out.print(msg);
                    userChoice = userInput.next();
                    msg = "Please enter the value\n";
                    System.out.print(msg);
                    int value = Integer.parseInt(userInput.next());
                    long timestamp = System.currentTimeMillis();
                    for (HostPort port : HostPort.values()) {
                        if( port.getValue() !=_serverPort && ServerWorker.hostAvailabilityCheck(port.getValue())){
                            try{
                                Socket serverSocket = new Socket("localhost", port.getValue());
                                DataOutputStream dos = new DataOutputStream(serverSocket.getOutputStream());
                                dos.writeUTF(userChoice);
                                dos.writeUTF(String.valueOf(value));
                                dos.writeUTF(String.valueOf(timestamp));
                            }catch (java.io.IOException e){
                                System.out.print("Can not send msg to " + port.getValue() + "\n");
                            }
                        }
                    }
                    fsm = UIFSM.IDLE;
                    break;
                case PRINTALL:
                    for (Map.Entry<String, SmallFile> file: ServerWorker._dataFile.entrySet()){
                        System.out.print("File name : " + file.getKey()+"\n");
                        System.out.print("Value : " + file.getValue().get_val()+"\n");
                        System.out.print("timestamp : " + file.getValue().get_timestamp()+"\n");
                    }
                    fsm = UIFSM.IDLE;
                    break;
            }
        }
    }
}
