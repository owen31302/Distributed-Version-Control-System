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
public class BackupServer implements Runnable{
    public int _arg;
    public Long _timestamp = new Long(0);

    public BackupServer(int arg){
        _arg = arg;
        _timestamp = new Long(0);
    }

    public void run() {
        // --- Set ConcurrentHashMap as version control database
        ConcurrentHashMap<String, Stack<LinkedList<VersionData>>> git = new ConcurrentHashMap<String, Stack<LinkedList<VersionData>>>();

        int serverPort = 10000 + _arg;


        // --- Setup Server --- //
        // Register service on specific port
        // --- @CHECKCONNECTION: Receive the ping from MetaServer
        // --- @GET: Send the whole git to the MetaServer
        // --- @SET: Set the git
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
