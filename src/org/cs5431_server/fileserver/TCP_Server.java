package org.cs5431_server.fileserver;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;

/**
 * Created by zilong on 8/3/17.
 */
public class TCP_Server extends Thread {
    private ServerSocket ss;

    public TCP_Server(int port){
        try{
            ss = new ServerSocket(port);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void saveFile(Socket clientSocket) throws IOException{

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String client_msg = in.readLine(); // fileName + space + filesize

        String[] elements = client_msg.trim().split("\\s+");

        System.out.println(Arrays.toString(elements));

        String fileName = elements[0];
        int filesize = Integer.parseInt(elements[1]);



        System.out.println("Incoming File");

        //String fileName = "cats.txt";
        //String fileName = "Opt2.pdf";

        String dir = "/receive/";
        String absPath = System.getProperty("user.dir") + dir;

        File directory = new File(absPath);
        directory.mkdirs();

        File toSend = new File(absPath + fileName);

        /*
        if (Objects.equals(fileName,"cats.txt")){
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
        */

        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
        FileOutputStream fos = new FileOutputStream(toSend);
        BufferedOutputStream bos = new BufferedOutputStream(fos);





        byte[] buffer = new byte[4096];
        //int filesize = 807784; // Send file size in separate msg
        int read = 0;
        int totalRead = 0;
        int remaining = filesize;

        System.out.println("Saving "+toSend.getAbsolutePath() + " of size "+ filesize + " bytes.");

        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            System.out.println("read " + totalRead + " bytes.");
            fos.write(buffer, 0, read);
        }

        System.out.println("Done");

        fos.flush();
        bos.flush();

        /*
        bos.close();
        fos.close();
        dis.close();
        */

    }

    public void run(){
        while(true){
            try{
                Socket clientSocket = ss.accept();
                // Then read the name of the file and the size

                saveFile(clientSocket);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        TCP_Server fs = new TCP_Server(8080);
        fs.start();
    }

}
