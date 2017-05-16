package org.cs5431;

import java.net.ServerSocket;
import java.net.Socket;

public class SSLSocketThread implements Runnable {
    private SQL_Files sql_files;
    private SQL_Accounts sql_accounts;
    private ServerSocket ss;
    private Email email;

    SSLSocketThread(SQL_Accounts sql_accounts, SQL_Files sql_files, ServerSocket serverSocket, Email email) {
        this.sql_accounts = sql_accounts;
        this.sql_files = sql_files;
        this.ss = serverSocket;
        this.email = email;
    }

    public void run() {
        try {
            while (true) {
                Socket s = ss.accept();
                new SSLServer(s, sql_accounts, sql_files, email).start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
