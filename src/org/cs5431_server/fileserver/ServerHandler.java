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

    /* Records the action taken:PLACEHOLDER FOR NOW */
    private void record(String action){
        System.out.printf(action);
    }


    /**
     * Transfer file from client to the server here
     * @param fileName file requested by client
     */
    public void sendToClient(String fileName){

        int SOCKET_PORT = s.getPort();

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        ServerSocket servsock = null;
        Socket sock = null;

        try{
            servsock = new ServerSocket(SOCKET_PORT);
            while(true){
                System.out.println("Waiting...");
                try{
                    sock = servsock.accept();
                    System.out.println("Connected to:" + sock);

                    //Send the file to client
                    File myFile = new File(fileName);

                    //Read the file into byte array
                    byte[] mybytearray = new byte[(int) myFile.length()];
                    fis = new FileInputStream(myFile);
                    bis = new BufferedInputStream(fis);
                    bis.read(mybytearray,0,mybytearray.length);

                    os = sock.getOutputStream();
                    System.out.println("Sending "+fileName+" of size "+ mybytearray.length + " bytes.");
                    os.write(mybytearray,0,mybytearray.length);
                    os.flush();
                    System.out.println("Done");
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                finally{
                    try{
                        if (bis != null) bis.close();
                        if (fis != null) fis.close();
                        if (os != null) os.close();
                        if (sock != null) sock.close();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                if (servsock != null) servsock.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    //sending a welcome message to client when the thread runs
    //TODO: Call transfer() when appropriate?
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            PrintWriter Out = new PrintWriter(s.getOutputStream(), true);

            Out.println("Hello from the server"); //sending a hello message to client

            while (true){
                String client_msg = in.readLine();
                System.out.println(client_msg); //printing out client message

            }

        }catch (IOException error){
            System.out.println("Closing client...\n");
        }

        finally{
            try {
                s.close(); //closing socket as clean up code
                System.out.println("Connection Successfully Closed\n");
            }catch (IOException error){
                System.out.println("placeholder error 2\n");
            }
        }
    }

}
