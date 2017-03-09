package com.owen31302.quorumcloud;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/8/17.
 */
public class ServerWorker implements Runnable{

    private Socket _clientSocket;
    public static ConcurrentHashMap<String, SmallFile> _dataFile = new ConcurrentHashMap<String, SmallFile>();

    public ServerWorker(Socket clientSocket){
        this._clientSocket = clientSocket;
    }

    public void run() {
        try{
            // Get a communication stream associated with the socket
            DataOutputStream dos = new DataOutputStream (_clientSocket.getOutputStream());
            DataInputStream dis = new DataInputStream (_clientSocket.getInputStream());

            String fileName = dis.readUTF();
            System.out.print("I got: "+fileName+"\n");
            int value = Integer.parseInt(dis.readUTF());
            System.out.print("value: "+value+"\n");
            int timestamp = Integer.parseInt(dis.readUTF());
            System.out.print("timestamp: "+timestamp+"\n");

            // Add the value and timestamp to hashtable
            if(_dataFile.contains(fileName)){
                _dataFile.put(fileName, new SmallFile(value, timestamp));
            }else{

            }


            //switch (choice){
            //    case
            //}

        }catch (java.io.IOException e){
            System.out.print("ServerWorker error: "+e.toString()+"\n");
            System.out.print("ServerWorker cannot initiate.\n");
        }

    }

    public static boolean hostAvailabilityCheck(int port)
    {
        Socket s;
        try {
            s = new Socket("localhost", port);
            if (s.isConnected())
            {
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
