package com.owen31302.quorumcloud;
import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * Created by owen on 3/8/17.
 */
public class Client implements Runnable {
    private int _hostPort;


    public Client(int hostPort){
        this._hostPort = hostPort;
    }

    public void run() {
        System.out.print("Got port: " + _hostPort +"\n");
        try {

            // Open your connection to a server
            Socket serverSocket = new Socket("localhost", _hostPort);
            System.out.print("Port initiate success at " + _hostPort +"\n");

            DataOutputStream dos = new DataOutputStream(serverSocket.getOutputStream());

            while(true){
                //String message = _reader.next();
                //dos.writeUTF(message);
            }

            // Get an input file handle from the socket and read the input
            /*DataInputStream dis = new DataInputStream(serverSocket.getInputStream());
            System.out.print("Blocking\n");
            String st = new String(dis.readUTF());
            System.out.print("unBlocking\n");

            System.out.println(st);

            // When done, just close the connection and exit
            dis.close();
            serverSocket.close();*/

        }catch (java.io.IOException e){
            System.out.print("Client error: "+e.toString()+"\n");
            System.out.print("Socket cannot initiate.\n");
        }
    }
}
