package org.cs5431;

import java.net.ServerSocket;
import java.net.Socket;

public class SSL_Server {

    public static final int PORT = 55555;


    public static void main(String[] args){
        try {
            //generate keystore
            String filename = "";
            SSL_Server_Methods.generateKeyStore(filename);

            //export certificate and public key
            SSL_Server_Methods.exportCert(filename);


            //Setup SSL server socket
            ServerSocket ss = SSL_Server_Methods.setup_SSLServerSocket
                    (filename, PORT);

            while (true){
                System.out.println("Waiting for client to connect.");
                Socket s = ss.accept();
                new SSL_Server_Handler(s).start();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
