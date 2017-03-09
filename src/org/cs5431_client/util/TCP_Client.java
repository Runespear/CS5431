package org.cs5431_client.util;

/**
 * Created by zilong on 8/3/17.
 */

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;

public class TCP_Client{

    private Socket s;

    public TCP_Client(String hostIP, int port, String[] fileNames){
        try{
            this.s = new Socket(hostIP,port);
            sendFile(fileNames[0]);
            sendFile(fileNames[1]);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /*
    public void getFile(String fileName) throws IOException{
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        FileInputStream fis = new FileInputStream(fileName);
        byte[] buffer = new byte[4096];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }

        fis.close();
        dos.close();
    }
    */
    public void sendFile(String file) throws IOException {

        System.out.println("Sending stuff");
        String dir = "/send/";
        String absPath = System.getProperty("user.dir") + dir;

        File directory = new File(absPath);
        directory.mkdirs();

        File toSend = new File(absPath + file);
        System.out.println(toSend.getAbsolutePath());

        if (Objects.equals(file,"cats.txt")){
            toSend.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(toSend.getAbsolutePath()));
            out.write( "Make sure all printed\n" );
            out.write("Cats are cute\n");
            out.write("Keegan has too much free time\n");
            DateFormat df = DateFormat.getDateTimeInstance (DateFormat.MEDIUM, DateFormat.MEDIUM,
                    new Locale ("en", "US"));
            String formattedDate = df.format (new Date ());
            out.write(formattedDate+"\n");
            out.close();
        }

        System.out.println(toSend.length());

        //Send to server the name and the size of the file
        OutputStream ostream = s.getOutputStream( );
        PrintWriter pwrite = new PrintWriter(ostream, true);
        //These 2 lines send command to server
        pwrite.println(file + " " + toSend.length());


        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        FileInputStream fis = new FileInputStream(toSend);
        byte[] buffer = new byte[4096];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);

        }
        System.out.println("Done");

        dos.flush();

        //fis.close();
        //dos.close();
    }

    public static void main(String[] args) {
        String[] fileNames = new String[2];
        fileNames = new String[] {"data_generation_Fall16.xls", "Opt2.pdf"};
        TCP_Client fc = new TCP_Client("localhost", 8080, fileNames);

    }

}
