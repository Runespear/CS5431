package SSL_CmdLine_Test;

import org.cs5431.TransmittedFile;

import java.io.FileInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * Created by Brandon on 28/3/2017.
 */
public class Unsecured_Server_Methods {

    //Computing the MAC of a message
    public static byte[] sign(byte[] message, PrivateKey key) throws Exception{
        //SHA-256 hashing algorithm with RSA, getting the signature object
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign(key); //initializing the private key
        dsa.update(message);
        byte[] signature  = dsa.sign();
        return signature;
    }

    public static void sendCert(Socket s, String filepath, String filename, PrivateKey key) throws Exception{
        ObjectOutputStream out_to_Client = new ObjectOutputStream(s.getOutputStream());

        //Get the cert, mac and store in object
        FileInputStream inputStream = new FileInputStream(filepath);
        byte[] filebytes = new byte[inputStream.available()];
        inputStream.read(filebytes);
        inputStream.close();
        byte[] signedCert = sign(filebytes, key);
        TransmittedFile file_to_send = new TransmittedFile();
        file_to_send.file = filebytes;
        file_to_send.signature = signedCert;
        file_to_send.filename = filename;
        //Send the file to client

        out_to_Client.writeObject(file_to_send);
    }
}
