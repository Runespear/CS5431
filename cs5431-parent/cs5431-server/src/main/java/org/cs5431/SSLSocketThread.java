package org.cs5431;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class SSLSocketThread implements Runnable {
    SQL_Connection sqlConnection;
    ServerSocket ss;

    public SSLSocketThread(SQL_Connection sqlConnection, ServerSocket serverSocket) {
        this.sqlConnection = sqlConnection;
        this.ss = serverSocket;
    }

    public void run() {
        try {
            while (true) {
                Socket s = ss.accept();
                new SSLServer(s, sqlConnection).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
