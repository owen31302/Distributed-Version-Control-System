package com.owen31302.quorumcloud;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Created by owen on 3/9/17.
 */
public class SimpleServer {
    public static void main(String[] args){
        HashMap<String, SmallFile> files = new HashMap<String, SmallFile>();

        int _serverPort = 10000+Integer.parseInt(args[0]);

        // --- Setup Server --- //
        System.out.print("Server ini process.\n");
        try {
            // Register service on specific port
            ServerSocket serverSocket = new ServerSocket(_serverPort);
            while(!serverSocket.isClosed()){
                // Wait and accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.print("I got a client\n");


                // Get a communication stream associated with the socket
                DataOutputStream dos = new DataOutputStream (clientSocket.getOutputStream());
                DataInputStream dis = new DataInputStream (clientSocket.getInputStream());
                int action = dis.readInt();
                if(action == RequestType.CHECKCONNECTION){
                    continue;
                }
                String fileName = dis.readUTF();
                switch (action){
                    case RequestType.GET:
                        if(files.containsKey(fileName)){
                            long timestamp = files.get(fileName).get_timestamp();
                            dos.writeLong(timestamp);
                            System.out.print("Send timestamp: " + timestamp+"\n");
                        }else{
                            dos.writeLong(-1);
                            System.out.print("Send timestamp: -1\n");
                        }
                        break;
                    case RequestType.SET:
                        int value = dis.readInt();
                        Long timestamp = dis.readLong();
                        System.out.print("Set " + fileName + ", val = " + value + ", timestamp =  " + timestamp + "\n");
                        files.put(fileName,new SmallFile(value, timestamp));
                        break;
                }
            }

        }catch (java.io.IOException e){
            System.out.print("Server error: "+e.toString()+"\n");
        }
    }
}
