package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/9/17.
 */
public class BackupServer{

    public static void main(String[] args){
        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();

        int _serverPort = 10000+Integer.parseInt(args[0]);
        Long timestamp = new Long(0);

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
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());


                // --- @CHECKCONNECTION: Receive the ping from MetaServer
                // --- @GET: Send the whole git to the MetaServer
                // --- @SET: Set the git
                int action = ois.readInt();
                switch (action){
                    case RequestType.CHECKCONNECTION:
                        break;

                    case RequestType.GET:
                        oos.writeLong(timestamp);
                        oos.writeObject(git);
                        break;

                    case RequestType.SET:
                        timestamp = ois.readLong();
                        Object obj = ois.readObject();

                        if(obj instanceof ConcurrentHashMap){
                            git = (ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)obj;
                        }else{
                            System.out.print("ConcurrentHashMap conversion error.\n");
                        }
                        MetaServer.printAllGit(git);
                        break;
                }
            }

        }catch (java.io.IOException e){
            System.out.print("Server error: "+e.toString()+"\n");
        }catch (java.lang.ClassNotFoundException e){
            System.out.print("Class not found exception.\n");
        }
    }
}
