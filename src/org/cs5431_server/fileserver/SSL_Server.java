package org.cs5431_server.fileserver;

import com.sun.corba.se.spi.activation.Server;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Brandon on 24/3/2017.
 */
public class SSL_Server {

    public static final int PORT = 55555;


    public static void main(String[] args){
        try {
            //generate keystore
            SSL_Server_Methods.generateKeyStore();

            //export certificate and public key
            SSL_Server_Methods.exportCert();


            //Setup SSL server socket
            ServerSocket ss = SSL_Server_Methods.setup_SSLServerSocket(PORT);

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
