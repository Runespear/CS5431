package org.cs5431_server.setup;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Scanner;

/**
 * Sets up the server. The admin of a server should run this first and only
 * once.
 */
public class ServerSetup {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        //TODO validation for server name
        System.out.println("Enter the name of the server (that your users " +
                "will see):");
        String name = scanner.nextLine();
        System.out.println("Enter the IP address of the server:");
        InetAddressValidator IPValidator = new InetAddressValidator();
        String ip;
        while (true) {
            ip = scanner.nextLine();
            if (IPValidator.isValid(ip))
                break;
            System.out.println("Please enter a valid IP address:");
        }
        System.out.println("Enter the port address of the server:");
        Integer port;
        while (true) {
            try {
                port = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid port:");
            }
        }
        System.out.println("Enter the username you use to login to your MySQL" +
                " server");
        String username = scanner.nextLine();
        System.out.println("Enter the password you use to login to your MySQL" +
                " server");
        String password = scanner.nextLine();

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

        String url = "jdbc:mysql://" + ip + ":" + port;
        String createDB = "CREATE DATABASE IF NOT EXISTS cs5431";
        String createFSO = "CREATE TABLE FileSystemObjects (fsoid INT " +
                "UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                "parentFolderid INT UNSIGNED NOT NULL, fsoName VARCHAR(100) " +
                "NOT NULL, size VARCHAR(20) NOT NULL, " +
                "lastModified TIMESTAMP, isFile boolean NOT NULL, " +
                "FOREIGN KEY (parentFolderid) REFERENCES FileSystemObjects" +
                "(fsoid))";
        String createUsers = "CREATE TABLE Users (uid INT UNSIGNED " +
                "AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL, " +
                "pwd VARCHAR(50) NOT NULL, parentFolderid INT UNSIGNED NOT " +
                "NULL, email VARCHAR(50), " +
                "privKey CHAR(100) NOT NULL, pubKey CHAR(100) NOT NULL," +
                "FOREIGN KEY (parentFolderid) REFERENCES FileSystemObjects" +
                "(fsoid) ON DELETE CASCADE)";

        //TODO make tables as well?

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(createDB);
            statement.execute();
            connection.close();
            connection = DriverManager.getConnection(url+"/cs5431",
                    username, password);
            statement = connection.prepareStatement(createFSO);
            statement.execute();
            statement = connection.prepareStatement(createUsers);
            statement.execute();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("Distribute the "+name+".config and the "+name+
                        ".pub file found in the /user-config folder to your users.");
    }
}
