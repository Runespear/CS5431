package SSL_CmdLine_Test;

import java.net.*;

import static org.cs5431.controller.SSLController.connect_SSLServerSocket;

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
            Socket s = connect_SSLServerSocket(HOST,PORT, "truststorefilename.jks");



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
