package org.cs5431;

import java.net.Socket;
import java.security.PrivateKey;

public class Unsecured_Server_Handler extends Thread{

    protected Socket s;
    protected PrivateKey key;
    private String serverName;

    Unsecured_Server_Handler(Socket socket, PrivateKey key,
                             String serverName){
        this.s = socket;
        this.key = key;
        this.serverName = serverName;
    }

    public void run(){
        try {
            String filename = serverName + ".cer";
            String filepath = "./server-config/" + serverName + ".cer";
            Unsecured_Server_Methods.sendCert(s, filepath, filename, key);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
