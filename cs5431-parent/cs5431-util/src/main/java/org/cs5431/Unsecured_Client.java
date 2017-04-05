package org.cs5431;
import org.cs5431.TransmittedFile;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

/**
 * Created by Brandon on 28/3/2017.
 */
public class Unsecured_Client {

    public static final int unsecured_PORT = 44444;
    public static final String HOST = "127.0.0.1";

    public static void verify_and_receive_Cert(PublicKey ver_key, Socket s,
                                               String filepath) throws Exception{
        //Receiving object
        ObjectInputStream object_in = new ObjectInputStream(s.getInputStream());
        TransmittedFile received = (TransmittedFile) object_in.readObject();

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(ver_key);//initializing verification with public verification key of server
        sig.update(received.file);//supply signature object with message to be verified
        boolean verifies = sig.verify(received.signature); //verifying with received signature
        System.out.println("Is Signature Verified: "+ verifies);

        if (verifies) {
            FileOutputStream fos = new FileOutputStream(filepath + received.filename);
            fos.write(received.file);
            fos.close();
        }
    }

    public static void main(String[] args){
        try {


            Socket s = new Socket(HOST,unsecured_PORT);

            //TESTING METHODS FROM HERE ON
            String filepath = System.getProperty("user.dir") + "/Testing SSL Stuff/Client/";
            verify_and_receive_Cert(null,s,filepath);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
