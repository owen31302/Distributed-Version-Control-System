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
public class BackupServer implements TestCase{

    public static void main(String[] args){
        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = null;

        int serverPort = 10000+Integer.parseInt(args[0]);
        Long timestamp = new Long(0);

        // --- Setup Server --- //
        // Register service on specific port
        // --- @CHECKCONNECTION: Receive the ping from MetaServer
        // --- @GET: Send the whole git to the MetaServer
        // --- @SET: Set the git
        System.out.print("Server ini process.\n");
        try {

            ServerSocket serverSocket = new ServerSocket(serverPort);
            while(!serverSocket.isClosed()){
                // Wait and accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.print("I got a client\n");

                // Get a communication stream associated with the socket
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());

                int action = ois.readInt();
                switch (action){
                    case RequestType.CHECKCONNECTION:
                        break;

                    case RequestType.INITIALRETRIEVE:
                        oos.writeLong(timestamp);
                        oos.writeObject(git);
                        oos.flush();
                        break;

                    case RequestType.SET:
                        timestamp = ois.readLong();
                        Object obj = ois.readObject();

                        if(obj instanceof ConcurrentHashMap){
                            git = (ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>)obj;
                        }else{
                            System.out.print("ConcurrentHashMap conversion error.\n");
                        }
                        System.out.print("Timestamp: " + timestamp+ "\n");
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

    public void corruptValue() {

    }

    public void corruptTimestamp() {

    }

    public void shutDown() {

    }
}
