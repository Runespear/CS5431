package org.cs5431_server.fileserver;


import java.net.*;
import java.io.*;
/**
 * Created on 26/2/2017.
 */

//This server class accepts multiple connections from clients and launches threads to handle them

public class Server {
    public Server (int port_number) throws IOException{
        ServerSocket server = new ServerSocket(port_number);
        try {
            while (true) {
                System.out.println("Waiting... ");
                Socket client = server.accept();
                System.out.println("New Client Connected from " + client.getInetAddress());
                ServerHandler_New handler = new ServerHandler_New(client);
                System.out.println("Starting handler");
                handler.start();
            }
        }finally{
            server.close(); //probably not necessary
        }

    }

}
