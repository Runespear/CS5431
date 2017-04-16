package org.cs5431;

import java.net.ServerSocket;
import java.net.Socket;

public class SSLSocketThread implements Runnable {
    private SQL_Files sql_files;
    private SQL_Accounts sql_accounts;
    private ServerSocket ss;

    SSLSocketThread(SQL_Accounts sql_accounts, SQL_Files sql_files, ServerSocket serverSocket) {
        this.sql_accounts = sql_accounts;
        this.sql_files = sql_files;
        this.ss = serverSocket;
    }

    public void run() {
        try {
            while (true) {
                Socket s = ss.accept();
                new SSLServer(s, sql_accounts, sql_files).start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
