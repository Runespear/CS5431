package org.cs5431_server.fileserver;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class SSL_Server_Methods {

    // Command for keystore generation
    public static void generateKeyStore(String filename) throws Exception{
        //If keystore has already been created, just skip the step
        File f = new File(System.getProperty("user.dir") + "/keystorefilename.jks");
        if(!f.exists()) {
            String command = " -genkeypair " +
                                " -alias mykey " +
                                " -keyalg RSA " +
                                //" -sigalg SHA256withRSA "+
                                //" -dname CN=Java "+
                                //" -storetype JKS "+
                                //" -keypass password "+
                                " -keystore ./server-config/" + filename + ".jks";
                                //" -storepass password";
            String[] options = command.trim().split("\\s+");
            System.out.println(command);
            sun.security.tools.keytool.Main.main(options);
        }
    }

    // Command for exporting server's certificate and public key
    public static void exportCert(String filename) throws Exception{
        //If cert has already been created, just skip the step
        File f = new File("./server-config/"+ filename + ".cer");
        if(!f.exists()) {
            String command = " -export " +
                    " -alias mykey " +
                    //" -keyalg RSA " +
                    //" -sigalg SHA256withRSA "+
                    //" -dname CN=Java "+
                    //" -storetype JKS "+
                    //" -keypass password "+
                    " -keystore ./server-config/" + filename + ".jks" +
                    " -rfc -file ./server-config/" + filename+ ".cer";
            //" -storepass password";
            String[] options = command.trim().split("\\s+");
            System.out.println(command);
            sun.security.tools.keytool.Main.main(options);
        }
    }


    public static ServerSocket setup_SSLServerSocket(String serverName, int
                                                     Port_Number) throws Exception{

        System.setProperty("javax.net.ssl.keyStore", "" +
                "./server-config/" + serverName + ".jks");

        Scanner scanner = new Scanner (System.in);
        System.out.println("Type in your password to access the keystore: ");
        String pass = scanner.nextLine(); //Obtain user's command

        System.setProperty("javax.net.ssl.keyStorePassword", pass);

        ServerSocketFactory f = SSLServerSocketFactory.getDefault();
        ServerSocket ss = f.createServerSocket(Port_Number);

        return ss;

    }

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
