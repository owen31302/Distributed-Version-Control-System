package com.owen31302.quorumcloud.past;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by owen on 3/8/17.
 */
public class Dispatcher implements Runnable {

    ServerSocket _serverSocket;

    public Dispatcher(ServerSocket serverSocket){
        this._serverSocket = serverSocket;
    }

    public void run() {
        try{
            while(!_serverSocket.isClosed()){
                // Wait and accept a connection
                Socket clientSocket = _serverSocket.accept();
                System.out.print("I got a client\n");

                Thread serverSide = new Thread(new ServerWorker(clientSocket));
                serverSide.start();
            }
        }catch (java.io.IOException e){
            System.out.print("Dispatcher error: "+e.toString()+"\n");
        }

    }
}
