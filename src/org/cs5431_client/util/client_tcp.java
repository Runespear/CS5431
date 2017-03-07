package org.cs5431_client.util;


import java.net.*;
import java.io.*;
import java.util.*;

public class client_tcp extends Thread{

    /**
     * @param s passes socket for file transfer etc.
     * @return 0 if exit, 1 if transfer, 666 if unrecognised
     */
    public int waitforuser(Socket s){
        Scanner scanner = new Scanner (System.in);
        System.out.println("Enter 'e' to exit:");
        System.out.println("Enter 't filename' to request file:");
        // Use nextLine() instead of next to get whitespace
        String command = scanner.nextLine(); // Obtain user's command

        String[] elements = command.trim().split("\\s+");
        System.out.println(Arrays.toString(elements));

        if (Objects.equals("e",elements[0])){
            System.out.println("Bye bye");
            return 0;
        }

        else if (Objects.equals("t",elements[0])){
            // No restrictions on file to be transferred yet
            String hardFile = "~/Desktop/cats.txt";
            requestFromServer(hardFile,s);
            //

            //System.out.println("Requesting file "+ elements[1]);
            //requestFromServer(elements[1]);


            return 1;
        }

        else{
            System.out.println("Please enter valid command");
            return 666;
        }
    }



    /**
     * Requests file from server
     * @param fileName Path to file to be transferred
     * @param s Socket
     * Obviously very vulnerable right now (Can overwrite important binaries)
     * http://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
     * http://way2java.com/networking/sending-file-contents-two-way-communication/
     *
     * */
    public void requestFromServer(String fileName,Socket s){

        OutputStream ostream = null;
        PrintWriter pwrite = null;

        InputStream istream = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;


        //Hard code the directory
        String hardDir = "~/Desktop/";
        try{
            //String serverAddress = "localhost"; // to be filled in
            //int socket = 10000; //to be filled in

            //Making the connection
            //s = new Socket(serverAddress, socket);

            //Send file name over
            ostream = s.getOutputStream( );
            pwrite = new PrintWriter(ostream, true);
            pwrite.println(fileName);

            //Get file from server
            istream = s.getInputStream();
            fos = new FileOutputStream(fileName);
            bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[4096];

            int bytesRead;
            while ((bytesRead = istream.read(buffer)) > 0) {
                bos.write(buffer, 0, bytesRead);
            }
        }
        catch (IOException readException){
            //readException.printStackTrace();
        }
        // Only need to close if opened
        finally {
            try{
                if (fos!=null) fos.close();
                if (bos!=null) bos.close();
                if (istream !=null ) istream.close();

                if (pwrite != null) pwrite.close();
                if (ostream!=null) ostream.close();
                if (s != null) s.close();
            }
            catch (Exception e){
                //e.printStackTrace();
            }
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
            switch (waitforuser(s)) {
                case 0: Out.println("exit");
                    System.exit(0);
                case 1:  Out.println("transfer");
                    break;
                case 2:  System.out.println("Enter valid command please");
                    break;
                default: System.out.println("Enter valid command please");
                    break;
            }
            break;
        }

    }

    /* RUNNING THE CLIENT APPLICATION*/
    public static void main (String[] args) throws Exception{
        client_tcp client = new client_tcp();
        client.connectToServer();

    }
}
