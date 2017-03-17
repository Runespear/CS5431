package org.cs5431_server.setup;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;

/**
 * Sets up the server. The admin of a server should run this first and only
 * once.
 */
public class ServerSetup {

    public static void main(String[] args) {
        //TODO validation for IP address and port
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the name of the server:");
        String name = scanner.nextLine();
        System.out.println("Enter the IP address of the server:");
        String ip = scanner.nextLine();
        System.out.println("Enter the port address of the server:");
        String port = scanner.nextLine();

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new
                    BouncyCastleProvider());
            kpg.initialize(2048, new SecureRandom());
            KeyPair keyPair = kpg.generateKeyPair();

            File userDir = new File("./user-config/");
            if (!userDir.exists()){
                if (!userDir.mkdir())
                    System.err.println("Could not make user config folder");
            }

            //writes the server details into an easily distributable config file
            File configFile = new File("./user-config/"+name+".config");
            Writer writer = new BufferedWriter(new OutputStreamWriter(new
                    FileOutputStream(configFile)));
            writer.write(name+"\n"+ip+"\n"+port+"\n");
            writer.close();
            //writes the server public key into an easily distributable file
            File pubKeyFile = new File("./user-config/"+name+".pub");
            ObjectOutputStream publicKeyOS= new ObjectOutputStream(
                    new FileOutputStream(pubKeyFile));
            publicKeyOS.writeObject(keyPair.getPublic());
            publicKeyOS.close();

            //writes the server private key into a file
            File serverDir = new File("./server-config/");
            if (!serverDir.exists()){
                if (!serverDir.mkdir())
                    System.err.println("Could not make server config folder");
            }
            File privKeyFile = new File("./server-config/"+name+".priv");
            ObjectOutputStream privateKeyOS = new ObjectOutputStream(
                    new FileOutputStream(privKeyFile));
            privateKeyOS.writeObject(keyPair.getPrivate());
            privateKeyOS.close();

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }


        //TODO create databases

        System.out.println("Distribute the "+name+".config and the "+name+
                        ".pub file found in the /user-config folder to your users.");
    }
}
