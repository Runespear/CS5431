

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import sun.security.tools.*;
/**
 * Created by Brandon on 24/3/2017.
 */
public class SSL_Client {

    public static final int unsecured_PORT = 44444;
    public static final int PORT = 55555;
    public static final String HOST = "127.0.0.1";

    public static void main(String[] args){
        try {

            SSL_Client_Methods.importCert();
            Socket s = SSL_Client_Methods.connect_SSLServerSocket(HOST,PORT);



            //TESTING METHODS FROM HERE ON

            //SSL_Client_Methods.send_Request(s); WORKS
            String filename = "data_generation_Fall16.xls";
            String filepath = System.getProperty("user.dir") + "/Testing SSL Stuff/Client/" + "data_generation_Fall16.xls";
            SSL_Client_Methods.sendFile(s, filepath, filename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
