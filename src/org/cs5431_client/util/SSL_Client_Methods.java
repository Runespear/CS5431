package org.cs5431_client.util;

import org.cs5431_server.fileserver.TransmittedFile;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by Brandon on 25/3/2017.
 */
public class SSL_Client_Methods {

    public static Socket connect_SSLServerSocket(String host, int Port_Number) throws Exception{

        System.setProperty("javax.net.ssl.trustStore", "truststorefilename.jks");

        Scanner scanner = new Scanner (System.in);
        System.out.println("Type in your password to access the truststore: \n");
        String pass = scanner.nextLine(); //Obtain user's command
        System.setProperty("javax.net.ssl.trustStorePassword", pass);

        SocketFactory f = SSLSocketFactory.getDefault();
        Socket s = f.createSocket(host, Port_Number);
        return s;
    }

    public static void send_Request(Socket s) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(s.getOutputStream()));

        System.out.println("Enter text below and press Enter. "
                + "The text will be echoed by the server.");
        String str = null;
        while ((str = r.readLine()) != null) {
            w.write(str + '\n');
            w.flush();
        }
    }

    //Method to import a cert into a truststore
    public static void importCert() throws Exception{

            String command = " -import " +
                    " -alias mykey " +
                    //" -keyalg RSA " +
                    //" -sigalg SHA256withRSA "+
                    //" -dname CN=Java "+
                    //" -storetype JKS "+
                    " -file certfilename.cer "+
                    " -keystore truststorefilename.jks";


            String[] options = command.trim().split("\\s+");
            System.out.println(command);
            sun.security.tools.keytool.Main.main(options);
    }

    public static void sendFile(Socket s, String filepath, String filename) throws Exception{

        ObjectOutputStream out_to_Server = new ObjectOutputStream(s.getOutputStream());

        //Get the file and store in filebytes array
        FileInputStream inputStream = new FileInputStream(filepath);
        byte[] filebytes = new byte[inputStream.available()];
        inputStream.read(filebytes);
        inputStream.close();

        //Save the buffer array in an object
        TransmittedFile file_to_send = new TransmittedFile();
        file_to_send.file = filebytes;
        file_to_send.filename = filename;

        //Send the file to server
        out_to_Server.writeObject(file_to_send);
    }

    public static void receiveFile(Socket s, String filepath) throws Exception{
        ObjectInputStream object_in = new ObjectInputStream(s.getInputStream());
        TransmittedFile received = (TransmittedFile) object_in.readObject();

        FileOutputStream fos = new FileOutputStream(filepath + received.filename);
        fos.write(received.file);
        fos.close();
    }
}
