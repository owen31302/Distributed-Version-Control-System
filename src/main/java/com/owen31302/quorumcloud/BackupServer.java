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

                System.out.print("Q1\n");
                int action = ois.readInt();
                if(action == RequestType.CHECKCONNECTION){
                    continue;
                }
                System.out.print("Q2\n");
                // --- @GET: Send the whole git to the MetaServer
                // --- @SET: Set the git
                switch (action){
                    case RequestType.GET:
                        System.out.print("Q3\n");
                        oos.writeObject(git);
                        break;
                    case RequestType.SET:
                        System.out.print("Q4\n");
                        Object obj = ois.readObject();

                        if(obj instanceof ConcurrentHashMap){
                            System.out.print("Q5\n");
                            git = (ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)obj;
                        }else{
                            System.out.print("ConcurrentHashMap conversion error.\n");
                        }

                        System.out.print("Q7\n");
                        for(Map.Entry files:git.entrySet()){
                            System.out.print("Filename: " + files.getKey() + "\n");

                            Object obj2 = files.getValue();
                            Stack<List<VersionData>> versions = null;
                            if(obj2 instanceof Stack){
                                versions = (Stack<List<VersionData>>)obj2;
                            }else{
                                System.out.print("Stack conversion error.\n");
                            }

                            int cnt = 1;
                            for(List<VersionData> version:versions){
                                for (int i = 0; i<versions.size(); i++){
                                    System.out.print("Version " + cnt + " Value: " + version.get(i).get_val() +" , " +
                                            "Timestamp: " +version.get(i).get_timestamp() +
                                            "\n");
                                }
                                cnt++;
                            }
                        }
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
