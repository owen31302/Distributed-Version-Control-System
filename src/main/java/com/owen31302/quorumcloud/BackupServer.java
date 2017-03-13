package com.owen31302.quorumcloud;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/9/17.
 *
 * BackupServer is to store the git(ConcurrentHashMap) file on this local server.
 * As well as hand the request from MetaServer.
 */
public class BackupServer{

    public static void main(String[] args){
        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
        int arg = Integer.valueOf(args[0]);
        Long _timestamp = new Long(0);
        int serverPort = 10000 + arg;


        // --- Setup Server --- //
        // Register service on specific port
        // --- @CHECKCONNECTION  : Receive the ping from MetaServer
        // --- @GET              : Send the whole git to the MetaServer
        // --- @SET              : Set the git
        // --- @CORRUPT_VALUE    : Set the corrupted value to its git
        // --- @SHUTDOWN         : Simulate the crash mode and restart as a empty BackupServer
        // --- @CORRUPT_TIMESTAMP: Set the corrupted timestamp
        System.out.print("Server running on port " + serverPort + "\n");
        try {
            while (true){
                ServerSocket serverSocket = new ServerSocket(serverPort);
                while(!serverSocket.isClosed()){
                    // Wait and accept a connection
                    Socket clientSocket = serverSocket.accept();
                    //System.out.print("I got a client\n");

                    // Get a communication stream associated with the socket
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());

                    int action = ois.readInt();
                    switch (action){
                        case RequestType.CHECKCONNECTION:
                            break;

                        case RequestType.INITIALRETRIEVE:
                            oos.writeLong(_timestamp);
                            oos.writeObject(git);
                            oos.flush();
                            break;

                        case RequestType.SET:
                            System.out.print("Server running on port " + serverPort + " Timestamp: " + _timestamp+ "\n");
                            _timestamp = ois.readLong();
                            Object obj = ois.readObject();
                            if(obj instanceof ConcurrentHashMap){
                                git = (ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)obj;
                            }else{
                                System.out.print("ConcurrentHashMap conversion error.\n");
                            }
                            System.out.print("Server running on port " + serverPort + " Timestamp: " + _timestamp+ "\n");
                            MetaServer.printAllGit(git);
                            break;

                        case RequestType.CORRUPT_VALUE:
                            obj = ois.readObject();
                            git = (ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)obj;
                            System.out.print("CORRUPT_VALUE:\n");
                            MetaServer.printAllGit(git);
                            break;

                        case RequestType.SHUTDOWN:
                            serverSocket.close();
                            System.out.print("SHUTDOWN @"+serverPort + "\n");
                            break;

                        case RequestType.CORRUPT_TIMESTAMP:
                            _timestamp = new Long((int)(10000*Math.random()));
                            System.out.print("CORRUPT_TIMESTAMP: "+ _timestamp+"\n");
                            break;
                    }
                }

                // --- SHUTDOWN Mode
                System.out.print("Restarting after 5s... \n");
                Thread.sleep(5000);
                _timestamp = new Long(0);
                git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();
                System.out.print("BackupServer @port " + serverPort+" back!\n");
            }
        }catch (java.io.IOException e){
            System.out.print("Server error: "+e.toString()+"\n");
        }catch (java.lang.ClassNotFoundException e){
            System.out.print("Class not found exception.\n");
        }catch (java.lang.InterruptedException e){
            System.out.print("InterruptedException.\n");
        }
    }
}
