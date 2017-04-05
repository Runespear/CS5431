package org.cs5431;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class SSLSocketThread implements Runnable {
    SQL_Connection sqlConnection;
    String serverName;
    Integer sslPort;

    public SSLSocketThread(SQL_Connection sqlConnection, String
            serverName, Integer sslPort) {
        this.sqlConnection = sqlConnection;
        this.serverName = serverName;
        this.sslPort = sslPort;
    }

    public void run() {
        try {
            ServerSocket ss = setup_SSLServerSocket
                    (serverName, sslPort);
            while (true) {
                Socket s = ss.accept();
                new SSLServer(s, sqlConnection).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ServerSocket setup_SSLServerSocket(String serverName, int
            Port_Number) throws Exception{

        System.setProperty("javax.net.ssl.keyStore", "./server-config/" + serverName + ".jks");

        Scanner scanner = new Scanner (System.in);
        System.out.println("Type in your password to access the keystore: ");
        String pass = scanner.nextLine(); //Obtain user's command

        System.setProperty("javax.net.ssl.keyStorePassword", pass);

        //ServerSocketFactory f = SSLServerSocketFactory.getDefault();
        //ServerSocket ss = f.createServerSocket(Port_Number);

        final SSLServerSocketFactory sslSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        final SSLServerSocket sslServerSocket = (SSLServerSocket) sslSocketFactory.createServerSocket(Port_Number);

        String[] ciphersuite = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};
        sslServerSocket.setEnabledCipherSuites(ciphersuite);
        return sslServerSocket;
        //return ss;

    }
}
