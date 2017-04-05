package SSL_CmdLine_Test;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;

import static org.cs5431.controller.SSLController.verify_and_receive_Cert;

/**
 * Created by Brandon on 28/3/2017.
 */
public class Unsecured_Client {

    public static final int unsecured_PORT = 44444;
    public static final String HOST = "127.0.0.1";

    public static void main(String[] args){
        try {
            Socket s = new Socket(HOST,unsecured_PORT);

            //TESTING METHODS FROM HERE ON
            String filepath = System.getProperty("user.dir") + "/Testing SSL Stuff/Client/";
            verify_and_receive_Cert(null,s,filepath);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
