package SSL_CmdLine_Test;

import org.cs5431.TransmittedFile;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class SSL_Server_Methods {
    public static void receive_Request(Socket s) throws Exception{
        //Socket s = ss.accept();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(s.getInputStream()));
        String str = null;
        while ((str = r.readLine()) != null) {
            System.out.println(str);
            System.out.flush();
        }
    }

    public static void receiveFile(Socket s, String filepath) throws Exception{
        ObjectInputStream object_in = new ObjectInputStream(s.getInputStream());
        TransmittedFile received = (TransmittedFile) object_in.readObject();

        FileOutputStream fos = new FileOutputStream(filepath + received.filename);
        fos.write(received.file);
        System.out.println("Done");
        fos.close();
    }

    public static void sendFile(Socket s, String filepath, String filename) throws Exception{

        ObjectOutputStream out_to_Client = new ObjectOutputStream(s.getOutputStream());

        //Get the file and store in filebytes array
        FileInputStream inputStream = new FileInputStream(filepath);
        byte[] filebytes = new byte[inputStream.available()];
        inputStream.read(filebytes);
        inputStream.close();

        //Save the buffer array in an object
        TransmittedFile file_to_send = new TransmittedFile();
        file_to_send.file = filebytes;
        file_to_send.filename = filename;

        //Send the file to client
        out_to_Client.writeObject(file_to_send);
    }

}
