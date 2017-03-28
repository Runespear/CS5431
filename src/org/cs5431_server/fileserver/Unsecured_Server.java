package org.cs5431_server.fileserver;

import java.net.ServerSocket;
import java.net.Socket;

public class Unsecured_Server {

    public static final int PORT = 44444;


    public static void main(String[] args){
        try {
            //Setup unsecured server socket
            ServerSocket ss = new ServerSocket(PORT);

            while (true){
                System.out.println("Waiting for client to connect.");
                Socket s = ss.accept();
                new Unsecured_Server_Handler(s, null, "").start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
