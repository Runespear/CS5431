package org.cs5431_server.fileserver;


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


    /**
     * Transfer file from client to the server here
     * @param dirToSaveTo Saves file to the directory, checks if "/" is included at the end as well
     */
    public void transfer(String dirToSaveTo){
        try {
            DataInputStream input = new DataInputStream(s.getInputStream());
            DataInputStream clientData = new DataInputStream(input);
            //Read the filename
            String fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream(fileName);
            //Get the size of the file
            long size = clientData.readLong();
            byte[] buffer = new byte[1024]; // Our byte array that will be our file
            // Transfer the file from client
            int bytesRead;
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1){
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            //Check to see if directory path has '/' at the very end
            char lastChar = dirToSaveTo.charAt(dirToSaveTo.length() - 1));
            if ( lastChar != '/'){
                dirToSaveTo = dirToSaveTo + "/";
            }
            //Now save the file
            FileOutputStream fos = new FileOutputStream(dirToSaveTo+fileName);
            fos.write(buffer);
            //close all streams
            input.close();
            clientData.close();
            output.close();
            fos.close();

        }
        catch(IOException e){
            System.out.println("Connection:"+e.getMessage());
        }
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
