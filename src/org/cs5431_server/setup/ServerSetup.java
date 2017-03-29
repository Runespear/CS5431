package org.cs5431_server.setup;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cs5431_client.util.Validator;
import org.cs5431_server.fileserver.SSL_Server_Methods;

import java.io.*;
import java.net.ServerSocket;
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

        System.out.println("Enter the name of the server (that your users " +
                "will see):");
        String name;
        while (!Validator.validFileName(name = scanner.nextLine())) {
            System.out.println("Please enter a server name that can be used " +
                    "as a file name:");
        }

        System.out.println("Enter the IP address of the server/database:");
        String ip;
        while (!Validator.validIP(ip = scanner.nextLine())) {
            System.out.println("Please enter a valid IP address:");
        }

        System.out.println("Enter the port of the MYSQL database:");
        String dbPort;
        while (!Validator.validPort(dbPort = scanner.nextLine())) {
            System.out.println("Please enter a valid IP address:");
        }

        System.out.println("Enter the outward-facing non-SSL port of the " +
                "server:");
        String outPort;
        while (!Validator.validPort(outPort = scanner.nextLine())) {
            System.out.println("Please enter a valid port:");
        }

        System.out.println("Enter the outward-facing SSL port of the server:");
        String sslPort;
        while (!Validator.validPort(sslPort = scanner.nextLine())) {
            System.out.println("Please enter a valid port:");
        }

        System.out.println("Enter the username you use to login to your MySQL" +
                " server:");
        String username = scanner.nextLine();
        System.out.println("Enter the password you use to login to your MySQL" +
                " server:");
        String password = scanner.nextLine();

        System.out.println("Enter the username you want to use to login to " +
                "your server");
        String serverUser;
        while (!Validator.validUsername(serverUser = scanner.nextLine())) {
            System.out.println("Please enter a valid username");
        }
        System.out.println("Enter the password you want to use to login to " +
                "your server");
        String serverPwd;
        while (!Validator.validPassword(serverPwd = scanner.nextLine())) {
            System.out.println("Please enter a password that is at least 16 " +
                    "characters long");
        }


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

            //writes the server details that the user needs to know into an
            // easily distributable config file
            File configFile = new File("./user-config/"+name+".config");
            Writer writer = new BufferedWriter(new OutputStreamWriter(new
                    FileOutputStream(configFile)));
            writer.write(ip+"\n"+outPort+"\n"+sslPort+"\n");
            writer.close();
            //writes the server public key into an easily distributable file
            File pubKeyFile = new File("./user-config/"+name+".pub");
            ObjectOutputStream publicKeyOS= new ObjectOutputStream(
                    new FileOutputStream(pubKeyFile));
            publicKeyOS.writeObject(keyPair.getPublic());
            publicKeyOS.close();

            File serverDir = new File("./server-config/");
            if (!serverDir.exists()){
                if (!serverDir.mkdir())
                    System.err.println("Could not make server config folder");
            }
            //writes the server details that the server needs to know into a
            // config file
            File serverConfigFile = new File("./server-config/"+name+"" +
                    ".config");
            writer = new BufferedWriter(new OutputStreamWriter(new
                    FileOutputStream(serverConfigFile)));
            writer.write(ip+"\n"+dbPort+"\n"+outPort+"\n"+sslPort+"\n");
            writer.close();
            //writes the server private key into a file
            File privKeyFile = new File("./server-config/"+name+".priv");
            ObjectOutputStream privateKeyOS = new ObjectOutputStream(
                    new FileOutputStream(privKeyFile));
            privateKeyOS.writeObject(keyPair.getPrivate());
            privateKeyOS.close();

            File fileDir = new File("./files/");
            if (!fileDir.exists()) {
                if (!fileDir.mkdir())
                    throw new IOException("Could not create ./files/ folder");
            }

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        String url = "jdbc:mysql://" + ip + ":" + dbPort;
        String createDB = "CREATE DATABASE IF NOT EXISTS cs5431";
        String createUser = "CREATE USER ?@? IDENTIFIED BY ?;";
        String grantPermissions = "GRANT ALL ON cs5431.* TO ?@? IDENTIFIED BY ?;";
        String createFSO = "CREATE TABLE FileSystemObjects (fsoid INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, \n" +
                "parentFolderid INT UNSIGNED NOT NULL, fsoName VARCHAR(100) NOT NULL, size VARCHAR(20) NOT NULL, \n" +
                "lastModified TIMESTAMP, isFile boolean NOT NULL, fsoNameIV CHAR(255));";
        String createUsers = "CREATE TABLE Users (uid INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL, \n" +
                "pwd VARCHAR(50) NOT NULL, parentFolderid INT UNSIGNED NOT NULL, email VARCHAR(50), \n" +
                "privKey BLOB NOT NULL, pubKey BLOB NOT NULL, pwdSalt CHAR(255) NOT NULL, privKeySalt CHAR(255) NOT NULL, \n" +
                "FOREIGN KEY (parentFolderid) REFERENCES FileSystemObjects(fsoid) ON DELETE CASCADE);";
        String createEditors = "CREATE TABLE Editors (fsoid INT UNSIGNED NOT NULL,\n" +
                "uid INT UNSIGNED NOT NULL,\n" +
                "FOREIGN KEY (uid) REFERENCES Users(uid) ON DELETE CASCADE,\n" +
                "FOREIGN KEY (fsoid) REFERENCES FileSystemObjects(fsoid) ON DELETE CASCADE);";
        String createViewers = "CREATE TABLE Viewers (fsoid INT UNSIGNED NOT NULL,\n" +
                "uid INT UNSIGNED NOT NULL,\n" +
                "FOREIGN KEY (uid) REFERENCES Users(uid) ON DELETE CASCADE,\n" +
                "FOREIGN KEY (fsoid) REFERENCES FileSystemObjects(fsoid) ON DELETE CASCADE);";
        String createFileLog = "CREATE TABLE FileLog (fileLogid INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,\n" +
                "fsoid INT UNSIGNED NOT NULL,\n" +
                "uid INT UNSIGNED NOT NULL, \n" +
                "lastModified TIMESTAMP, actionType CHAR(20),\n" +
                "FOREIGN KEY (fsoid) REFERENCES FileSystemObjects(fsoid) ON DELETE CASCADE,\n" +
                "FOREIGN KEY (uid) REFERENCES Users(uid) ON DELETE CASCADE);";
        String createUserLog = "CREATE TABLE UserLog (userLogid INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,\n" +
                "uid INT UNSIGNED NOT NULL, \n" +
                "lastModified TIMESTAMP, actionType CHAR(20),\n" +
                "FOREIGN KEY (uid) REFERENCES Users(uid) ON DELETE CASCADE);";
        String createFSOEnc = "CREATE TABLE FsoEncryption (fsoid INT UNSIGNED NOT NULL, uid INT UNSIGNED NOT NULL,\n" +
                "encKey BLOB NOT NULL, fileIV CHAR(255), \n" +
                "FOREIGN KEY (uid) REFERENCES Users(uid) ON DELETE CASCADE,\n" +
                "FOREIGN KEY (fsoid) REFERENCES FileSystemObjects(fsoid) ON DELETE CASCADE);";
        String createFileContents = "CREATE TABLE FileContents (fsoid INT UNSIGNED NOT NULL, path VARCHAR(100),\n" +
                "fileIV VARCHAR(32), FOREIGN KEY (fsoid) REFERENCES FileSystemObjects(fsoid) ON DELETE CASCADE);";

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(createDB);
            statement.execute();
            statement = connection.prepareStatement(createUser);
            statement.setString(1, serverUser);
            statement.setString(2, ip);
            statement.setString(3, serverPwd);
            statement.execute();

            statement = connection.prepareStatement(grantPermissions);
            statement.setString(1, serverUser);
            statement.setString(2, ip);
            statement.setString(3, serverPwd);
            statement.execute();
            connection.close();
            connection = DriverManager.getConnection(url+"/cs5431?autoReconnect=true&useSSL=false",
                    serverUser, serverPwd);

            statement = connection.prepareStatement(createFSO);
            statement.execute();
            statement = connection.prepareStatement(createUsers);
            statement.execute();
            statement = connection.prepareStatement(createEditors);
            statement.execute();
            statement = connection.prepareStatement(createViewers);
            statement.execute();
            statement = connection.prepareStatement(createFileLog);
            statement.execute();
            statement = connection.prepareStatement(createUserLog);
            statement.execute();
            statement = connection.prepareStatement(createFSOEnc);
            statement.execute();
            statement = connection.prepareStatement(createFileContents);
            statement.execute();

            connection.close();

            //SSL handling
            //generate keystore
            SSL_Server_Methods.generateKeyStore(name);
            //export certificate and public key
            SSL_Server_Methods.exportCert(name);

            System.out.println("Distribute the "+name+".config and the "+name+
                    ".pub file found in the /user-config folder to your users.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Problem when trying to initialise server. " +
                    "Please delete the "+name+".config and the "+name+
                    ".pub file found in the /user-config and /server-config " +
                    "folders and try again.");
        }
    }
}
