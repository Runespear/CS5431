package org.cs5431;

import java.net.ServerSocket;
import java.net.Socket;

public class SSLSocketThread implements Runnable {
    private SQL_Connection sqlConnection;
    private ServerSocket ss;

    SSLSocketThread(SQL_Connection sqlConnection, ServerSocket serverSocket) {
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
