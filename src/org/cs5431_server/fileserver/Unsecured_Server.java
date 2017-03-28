package org.cs5431_server.fileserver.Unsecured_Server_Handler;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Brandon on 28/3/2017.
 */
public class Unsecured_Server {

    public static final int PORT = 44444;


    public static void main(String[] args){
        try {
            //Setup unsecured server socket
            ServerSocket ss = new ServerSocket(PORT);

            while (true){
                System.out.println("Waiting for client to connect.");
                Socket s = ss.accept();
                new Unsecured_Server_Handler(s).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
