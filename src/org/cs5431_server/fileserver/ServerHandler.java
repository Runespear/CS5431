package org.cs5431_server.fileserver;


import java.net.*;
import java.io.*;

/**
 * Created on 26/2/2017.
 */
//This class handles each individual connection

public class ServerHandler extends Thread{
    protected Socket s;
    public ServerHandler(Socket s) throws IOException{

        this.s = s;

    }

    //sending a welcome message to client when the thread runs
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            PrintWriter Out = new PrintWriter(s.getOutputStream(), true);

            Out.println("Hello"); //sending a hello message to client

            while (true){
                String client_msg = in.readLine();
                System.out.printf(client_msg); //printing out client message
            }

        }catch (IOException error){
            System.out.printf("placeholder error");
        }

        finally{
            try {
                s.close(); //closing socket as clean up code
            }catch (IOException error){
                System.out.printf("placeholder error");
            }
        }
    }

}
