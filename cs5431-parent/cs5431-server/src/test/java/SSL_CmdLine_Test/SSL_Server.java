package SSL_CmdLine_Test;

import java.net.ServerSocket;
import java.net.Socket;

import static org.cs5431.ServerView.setup_SSLServerSocket;

public class SSL_Server {

    public static final int PORT = 55555;


    public static void main(String[] args){
        try {
            //generate keystore
            //TODO: test generation of keystore and exporting in server-setup
            /*
            String filename = "";
            generateKeyStore(filename);

            //export certificate and public key
            exportCert(filename);
            */

            String filename = "";
            //Setup SSL server socket
            ServerSocket ss = setup_SSLServerSocket(filename, PORT);

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
