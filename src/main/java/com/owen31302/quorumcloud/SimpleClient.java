package com.owen31302.quorumcloud;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by owen on 3/9/17.
 */
public class SimpleClient {
    public static void main(String[] args){
        Scanner userInput = new Scanner(System.in);
        UIFSM fsm  = UIFSM.IDLE;
        String msg;
        String filename;
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
                                DataOutputStream dos = new DataOutputStream(serverSocket.getOutputStream());
                                dos.writeInt(RequestType.GET);
                                dos.writeUTF(String.valueOf(filename));

                                DataInputStream dis = new DataInputStream(serverSocket.getInputStream());
                                Long returnTimestamp = dis.readLong();

                                dos.close();
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
                                DataOutputStream dos = new DataOutputStream(serverSocket.getOutputStream());
                                dos.writeInt(RequestType.SET);
                                dos.writeUTF(String.valueOf(filename));
                                dos.writeInt(value);
                                dos.writeLong(timestamp);

                                dos.close();
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
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
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
