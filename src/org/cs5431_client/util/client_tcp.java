package org.cs5431_client.util;


import java.net.*;
import java.io.*;
import java.util.*;

public class client_tcp extends Thread{

    public boolean waitforuser(){
        Scanner scanner = new Scanner (System.in);
        System.out.println("Enter 'e' to exit:");
        String command = scanner.next(); // Obtain user's command

        if (Objects.equals("e",command)){
            return true;
        }
        else{
            return false;
        }
    }


    public void connectToServer() throws IOException{
        String serverAddress = "localhost"; // to be filled in
        int socket = 10000; //to be filled in

        //Making the connection
        Socket s = new Socket(serverAddress, socket);

        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

        PrintWriter Out = new PrintWriter(s.getOutputStream(), true);

        Out.println("Hello from the client\n");

        String server_msg = in.readLine();
        System.out.println(server_msg); //printing out server message

        while (true){
            if (waitforuser()==true){
                break;
            }
        }
        System.exit(0);

    }


    /* RUNNING THE CLIENT APPLICATION*/
    public static void main (String[] args) throws Exception{
        client_tcp client = new client_tcp();
        client.connectToServer();

    }
}
