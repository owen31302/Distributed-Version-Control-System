package com.owen31302.quorumcloud.past;
import com.owen31302.quorumcloud.VersionData;

import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by owen on 3/8/17.
 */
public class ServerWorker implements Runnable{

    private Socket _clientSocket;
    public static ConcurrentHashMap<String, VersionData> _dataFile = new ConcurrentHashMap<String, VersionData>();

    public ServerWorker(Socket clientSocket){
        this._clientSocket = clientSocket;
    }

    public void run() {
        try{
            // Get a communication stream associated with the socket
            DataOutputStream dos = new DataOutputStream (_clientSocket.getOutputStream());
            DataInputStream dis = new DataInputStream (_clientSocket.getInputStream());

            String fileName = String.valueOf(dis.readUTF());
            //switch (action){
            //    case UIFSM.GET:
            //        break;
            //    case UIFSM.SET.getValue():
            //        break;
            //}
            System.out.print("I got: "+fileName+"\n");
            int value = Integer.parseInt(dis.readUTF());
            System.out.print("value: "+value+"\n");
            long timestamp = Long.parseLong(dis.readUTF());
            System.out.print("timestamp: "+timestamp+"\n");

            // Add the value and timestamp to hashtable
            _dataFile.put(fileName, new VersionData(value, timestamp));



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
