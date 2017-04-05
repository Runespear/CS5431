package org.cs5431.controller;

import org.cs5431.TransmittedFile;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Scanner;

public class SSLController {
    public static void importCert(String name) throws Exception{
        System.out.println("importing cert");
        String command = " -import " +
                " -alias mykey " +
                //" -keyalg RSA " +
                //" -sigalg SHA256withRSA "+
                //" -dname CN=Java "+
                //" -storetype JKS "+
                " -file ./user-config/" + name + ".cer "+
                " -keystore ./user-config/" + name + ".jks";


        String[] options = command.trim().split("\\s+");
        System.out.println(command);
        sun.security.tools.keytool.Main.main(options);

        Scanner scanner = new Scanner (System.in);
        System.out.println("Type in your password to access the truststore:");
        String pass = scanner.nextLine(); //Obtain user's command
        System.setProperty("javax.net.ssl.trustStorePassword", pass);
    }

    public static Socket connect_SSLServerSocket(String host, int
            Port_Number, String storeName) throws Exception{

        System.setProperty("javax.net.ssl.trustStore", storeName);

        //SocketFactory f = SSLSocketFactory.getDefault();
        //Socket s = f.createSocket(host, Port_Number);
        //return s;

        final SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        final SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, Port_Number);

        String[] ciphersuite = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};
        sslSocket.setEnabledCipherSuites(ciphersuite);
        return sslSocket;
    }

    public static void verify_and_receive_Cert(PublicKey ver_key, Socket s,
                                               String filepath) throws Exception{
        //Receiving object
        ObjectInputStream object_in = new ObjectInputStream(s.getInputStream());
        TransmittedFile received = (TransmittedFile) object_in.readObject();

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(ver_key);//initializing verification with public verification key of server
        sig.update(received.file);//supply signature object with cert to be verified
        boolean verifies = sig.verify(received.signature); //verifying with received signature
        System.out.println("Is Signature Verified: "+ verifies);

        if (verifies) {
            FileOutputStream fos = new FileOutputStream(filepath + received.filename);
            fos.write(received.file);
            fos.close();
        }
    }
}
