package org.cs5431;


import java.net.Socket;

public class SSL_Server_Handler extends Thread{
    protected Socket s;


    public SSL_Server_Handler(Socket socket){
        this.s = socket;

    }

    public void run(){
        try {

            //TESTING METHODS FROM HERE ON

            //SSL_Server_Methods.receive_Request(s); WORKS

            String filepath = System.getProperty("user.dir")+"/" ;//+ "/Testing SSL Stuff/Server/";
            System.out.println(filepath);
            SSL_Server_Methods.receiveFile(s, filepath);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
