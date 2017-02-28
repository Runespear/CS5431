package org.cs5431_server.fileserver;


import javax.xml.crypto.Data;
import java.net.*;
import java.io.*;

/**
 * Created on 26/2/2017.
 */
//This class handles each individual connection

public class ServerHandler extends Thread{
    protected Socket s; //client socket

    DataInputStream clientData;
    DataOutputStream output;

    public ServerHandler(Socket s) throws IOException{

        this.s = s;

    }



    public void transfer(){
        try {
            DataInputStream input = new DataInputStream(s.getInputStream());
            DataInputStream clientData = new DataInputStream(input);
            //Read the filename
            String fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream(fileName);
            //Get the size of the file
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            // Transfer the file from client
            int bytesRead;
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1){
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            //close all streams
            input.close();
            clientData.close();
            output.close();
        }
        catch(IOException e){
            System.out.println("Connection:"+e.getMessage());
        }
        // Closing the FileOutputStream handle

    }

    //sending a welcome message to client when the thread runs
    //TODO: Call transfer() when appropriate?
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
