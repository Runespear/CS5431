package org.cs5431_server.fileserver;

import org.cs5431_server.fileserver.Unsecured_Server_Methods;

import javax.crypto.SecretKey;
import java.net.Socket;
import java.security.PrivateKey;

/**
 * Created by Brandon on 28/3/2017.
 */
public class Unsecured_Server_Handler extends Thread{

    protected Socket s;
    protected PrivateKey key;


    public Unsecured_Server_Handler(Socket socket){
        this.s = socket;
        //TODO: INITIALIZE KEY

    }

    public void run(){
        try {

            //SEND CERT HERE
            String filename = "certfilename.cer";
            String filepath = System.getProperty("user.dir") + "/Testing SSL Stuff/Server/certfilename.cer";
            Unsecured_Server_Methods.sendCert(s, filepath, filename, key);



        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
