package org.cs5431.controller;

import org.cs5431.TransmittedFile;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;

import static org.cs5431.Constants.DEBUG_MODE;

public class SSLController {
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

    public static boolean verify_Cert(PublicKey ver_key,TransmittedFile received) throws Exception{
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(ver_key);//initializing verification with public verification key of server
        sig.update(received.file);//supply signature object with cert to be verified
        boolean verifies = sig.verify(received.signature); //verifying with received signature
        if (DEBUG_MODE)
            System.out.println("Is Cert Signature Verified: "+ verifies);
        return verifies;
    }

    public static TransmittedFile receive_Cert(Socket s, String filepath) throws Exception{
        //Receiving object
        ObjectInputStream object_in = new ObjectInputStream(s.getInputStream());
        TransmittedFile received = (TransmittedFile) object_in.readObject();

        FileOutputStream fos = new FileOutputStream(filepath + received.filename);
        fos.write(received.file);
        fos.close();
        return received;
    }

    public static boolean verify_JKS(PublicKey ver_key,TransmittedFile received) throws Exception{
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(ver_key);//initializing verification with public verification key of server
        sig.update(received.file);//supply signature object with cert to be verified
        boolean verifies = sig.verify(received.signature); //verifying with received signature
        if (DEBUG_MODE)
            System.out.println("Is Truststore Signature Verified: "+ verifies);
        return verifies;
    }

    public static TransmittedFile receive_JKS(Socket s, String filepath) throws Exception{
        //Receiving object
        ObjectInputStream object_in = new ObjectInputStream(s.getInputStream());
        TransmittedFile received = (TransmittedFile) object_in.readObject();

        FileOutputStream fos = new FileOutputStream(filepath + received.filename);
        fos.write(received.file);
        fos.close();
        return received;
    }
}
