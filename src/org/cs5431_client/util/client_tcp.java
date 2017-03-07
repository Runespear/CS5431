package org.cs5431_client.util;


import java.net.*;
import java.io.*;
import java.util.*;

public class client_tcp extends Thread{

    public Socket socket;
    /**
     * @param s passes socket for file transfer etc.
     * @return 0 if exit, 1 if transfer, 666 if unrecognised
     */
    public int waitforuser(Socket s){
        Scanner scanner = new Scanner (System.in);
        System.out.println("Enter 'e' to exit:");
        System.out.println("Enter 't filename' to request file:");
        System.out.println("Enter 'h' to request hard coded:");
        // Use nextLine() instead of next to get whitespace
        String command = scanner.nextLine(); // Obtain user's command

        String[] elements = command.trim().split("\\s+");

        System.out.println(Arrays.toString(elements));

        //TODO: Use case switch

        if (Objects.equals("e",elements[0])){
            System.out.println("Bye bye");
            return 0;
        }

        else if (Objects.equals("t",elements[0])){
            // No restrictions on file to be transferred yet
            String hardFile = "cats.txt";
            requestFromServer(hardFile,s);
            //

            //System.out.println("Requesting file "+ elements[1]);
            //requestFromServer(elements[1]);


            return 1;
        }

        else if(Objects.equals("h",elements[0])){
            requestHardCodedFile();
            return 2;
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
        String hardDir = System.getProperty("user.home")+"/Desktop/receive/";
        System.out.println("Writing to directory: " + hardDir);
        try{
            //Send file name over
            ostream = s.getOutputStream( );
            pwrite = new PrintWriter(ostream, true);
            //These 2 lines send command to server
            pwrite.println("transfer");
            pwrite.println(fileName);

            //Get file from server
            istream = s.getInputStream();



            fos = new FileOutputStream(hardDir + fileName);
            bos = new BufferedOutputStream(fos);

            //No of bytes read in one read() call
            int bytesRead = 0;
            byte[] contents = new byte[4096];

            while((bytesRead=istream.read(contents))!=-1){
                bos.write(contents, 0, bytesRead);
                System.out.println(bytesRead);
            }
            System.out.println("Is it done?");
            bos.flush();
            System.out.println("Done");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        // Only need to flush if opened
        finally {
            try{
                if (fos!=null) fos.flush();
                if (bos!=null) bos.flush();

                if (pwrite != null) pwrite.flush();
                if (ostream!=null) ostream.flush();
                //if (s != null) s.close();
            }
            catch (Exception e){
                //e.printStackTrace();
            }
        }
    }

    /**
     * Requests a hard coded file from specific directory from server
     * Writes to hard coded location
     * Hard coded folder is current working directory/receive
     * Test file is cats.txt
     * If using cmd line, invoke using "h"
     */
    public void requestHardCodedFile(){
        Socket s = this.socket;
        OutputStream ostream = null;
        PrintWriter pwrite = null;

        InputStream istream = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        //Hard code the directory
        new File(System.getProperty("user.dir")+"/receive").mkdirs();

        //Hard code the file
        String fileName = System.getProperty("user.dir")+"/receive/cats.txt";

        try{
            //String serverAddress = "localhost"; // to be filled in
            //int socket = 10000; //to be filled in

            //Making the connection
            //s = new Socket(serverAddress, socket);

            //Send file name over
            ostream = s.getOutputStream( );
            pwrite = new PrintWriter(ostream, true);
            //These 2 lines send command to server
            pwrite.println("hard transfer");
            pwrite.println(fileName);

            //Get file from server
            istream = s.getInputStream();

            fos = new FileOutputStream( fileName);
            bos = new BufferedOutputStream(fos);

            //No of bytes read in one read() call
            int bytesRead;
            byte[] contents = new byte[4096];

            while((bytesRead=istream.read(contents))!=-1){
                bos.write(contents, 0, bytesRead);
                System.out.println(bytesRead);
            }
            System.out.println("Is it done?");
            bos.flush();
            System.out.println("Done");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        // Only need to flush if opened
        finally {
            try{
                if (fos!=null) fos.flush();
                if (bos!=null) bos.flush();

                if (pwrite != null) pwrite.flush();
                if (ostream!=null) ostream.flush();
                //if (s != null) s.close();
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
        this.socket = s;
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

        PrintWriter Out = new PrintWriter(s.getOutputStream(), true);

        Out.println("Hello from the client\n");

        String server_msg = in.readLine();
        System.out.println(server_msg); //printing out server message

        while (true){
            System.out.println("Input a command");
            switch (waitforuser(s)) {
                case 0: Out.println("exit");
                    System.exit(0);
                case 1:  System.out.println("Test transfer");
                    break;
                case 2:  System.out.println("Requesting hard coded file");
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
