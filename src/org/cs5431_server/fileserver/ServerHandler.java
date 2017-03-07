package org.cs5431_server.fileserver;


import java.net.*;
import java.io.*;
import java.util.*;

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
        String hardDir = System.getProperty("user.home") + "/Desktop/send/";

        fileName = hardDir + fileName;

        System.out.println("Sending "+fileName + " to client.");

        int SOCKET_PORT = s.getPort();

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        ServerSocket servsock = null;
        Socket sock = null;

        try{
            //servsock = new ServerSocket(SOCKET_PORT);
            System.out.println("Waiting...");
            try{
                //sock = servsock.accept();
                System.out.println("Connected to:" + s);

                //Send the file to client
                File myFile = new File(fileName);

                //Read the file into byte array
                byte[] mybytearray = new byte[(int) myFile.length()];

                System.out.println(myFile.getAbsolutePath());

                fis = new FileInputStream(myFile);
                bis = new BufferedInputStream(fis);
                bis.read(mybytearray,0,mybytearray.length);

                os = s.getOutputStream();
                System.out.println("Sending "+fileName+" of size "+ mybytearray.length + " bytes.");
                os.write(mybytearray,0,mybytearray.length);
                System.out.println("Done");
                os.flush();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally{
                try{
                    if (os != null) os.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                //if (servsock != null) servsock.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    /**
     * Sends cats.txt to server
     */
    public void sendHardClient(){
        String fileName = "cats.txt";
        String hardDir = System.getProperty("user.dir") + "/send";

        new File(hardDir).mkdirs();

        System.out.println("Sending "+fileName + " to client.");

        FileInputStream fis;
        BufferedInputStream bis;
        OutputStream os = null;

        try{
            System.out.println("Waiting...");
            try{
                System.out.println("Connected to:" + s);

                //Send the file to client
                File myFile = new File(hardDir + "/" + fileName);
                System.out.println(myFile.getAbsolutePath());
                myFile.createNewFile();
                // if file already exists will do nothing

                BufferedWriter out = new BufferedWriter(new FileWriter(myFile.getAbsolutePath()));
                out.write( "Make sure all printed\n" );
                out.write("Cats are cute\n");
                out.write("Keegan has too much free time\n");
                out.close();

                //Read the file into byte array
                byte[] mybytearray = new byte[(int) myFile.length()];

                System.out.println(myFile.getAbsolutePath());

                fis = new FileInputStream(myFile);
                bis = new BufferedInputStream(fis);
                bis.read(mybytearray,0,mybytearray.length);

                os = s.getOutputStream();
                System.out.println("Sending "+fileName+" of size "+ mybytearray.length + " bytes.");
                os.write(mybytearray,0,mybytearray.length);
                System.out.println("Done");
                os.flush();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally{
                try{
                    if (os != null) os.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                //if (servsock != null) servsock.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //sending a welcome message to client when the thread runs
    // Overwrites default run
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            PrintWriter Out = new PrintWriter(s.getOutputStream(), true);

            Out.println("Hello from the server"); //sending a hello message to client

            while (true){
                String client_msg = in.readLine();
                if (client_msg != null){
                    System.out.println(client_msg); //printing out client message
                    switch (client_msg){
                        case "exit": System.out.println("Client disconnected");
                            break;
                        case "transfer": System.out.println("Transferring file to client");
                            sendToClient(in.readLine());
                            break;
                        case "hard transfer": System.out.println("Transferring hard coded file");
                            sendHardClient();
                        default:
                            break;
                    }
                }
            }

        }catch (IOException error){
            error.printStackTrace();
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
