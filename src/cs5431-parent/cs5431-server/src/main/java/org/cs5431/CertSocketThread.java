package org.cs5431;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.SecureRandom;

import static org.cs5431.Encryption.signCert;
import static org.cs5431.Encryption.signJKS;

public class CertSocketThread implements Runnable {
    private String serverName;
    private Integer outPort;
    private PrivateKey serverPrivKey;

    CertSocketThread(String serverName, Integer outPort,
                     PrivateKey serverPrivKey) {
        this.serverName = serverName;
        this.outPort = outPort;
        this.serverPrivKey = serverPrivKey;
    }

    public void run() {
        try {
            ServerSocket ss = new ServerSocket(outPort);
            while (true) {
                Socket s = ss.accept();
                new CertTransferThread(s, serverPrivKey, serverName).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CertTransferThread extends Thread{

    protected Socket s;
    private PrivateKey key;
    private String serverName;

    CertTransferThread(Socket socket, PrivateKey key,
                             String serverName){
        this.s = socket;
        this.key = key;
        this.serverName = serverName;
    }

    public void run(){
        try {
            String certfilename = serverName + ".cer";
            String certfilepath = "./server-config/" + serverName + ".cer";
            String jksfilename = serverName + ".jks";
            String jksfilepath = "./server-config/" + serverName + ".jks";
            sendCert(s, certfilepath, certfilename, key);
            sendJKS(s,jksfilepath, jksfilename,key);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendCert(Socket s, String filepath, String filename, PrivateKey key) throws Exception{
        ObjectOutputStream out_to_Client = new ObjectOutputStream(s.getOutputStream());

        //Get the cert, mac and store in object
        FileInputStream inputStream = new FileInputStream(filepath);
        byte[] filebytes = new byte[inputStream.available()];
        if (inputStream.read(filebytes) == 0)
            throw new Exception("Cert transmitted is empty!");
        inputStream.close();
        byte[] signedCert = signCert(filebytes, key);
        TransmittedFile file_to_send = new TransmittedFile();
        file_to_send.file = filebytes;
        file_to_send.signature = signedCert;
        file_to_send.filename = filename;
        //Send the file to client

        out_to_Client.writeObject(file_to_send);
    }

    public static void sendJKS(Socket s, String filepath, String filename, PrivateKey key) throws Exception{
        ObjectOutputStream out_to_Client = new ObjectOutputStream(s.getOutputStream());

        //Get the cert, mac and store in object
        FileInputStream inputStream = new FileInputStream(filepath);
        byte[] filebytes = new byte[inputStream.available()];
        if (inputStream.read(filebytes) == 0)
            throw new Exception("JKS transmitted is empty!");
        inputStream.close();
        byte[] signedJKS = signJKS(filebytes, key);
        TransmittedFile file_to_send = new TransmittedFile();
        file_to_send.file = filebytes;
        file_to_send.signature = signedJKS;
        file_to_send.filename = filename;
        //Send the file to client

        out_to_Client.writeObject(file_to_send);
    }


}
