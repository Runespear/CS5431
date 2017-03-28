package org.cs5431_server.fileserver;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.cs5431_client.util.SQL_Connection;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class ServerView {
    public static void main(String[] args) {

        File serverConfigDir = new File("./server-config");
        if (!serverConfigDir.exists()){
            System.err.println("Config files not found. Please run " +
                    "ServerSetup first.");
            return;
        }
        File[] configFiles = serverConfigDir.listFiles((dir, filename) ->
                filename.endsWith(".config"));

        if (configFiles == null) {
            System.err.println("Config files not found. Please run " +
                    "ServerSetup first.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the name of the server you wish to run:");
        System.out.print("Current servers: ");
        for (File f : configFiles) {
            String fileName = f.getName();
            int pos = fileName.lastIndexOf(".");
            fileName = fileName.substring(0, pos);
            System.out.println(fileName);
        }
        String serverName = scanner.nextLine();
        System.out.println("Enter the username you use to login to the server:");
        String username = scanner.nextLine();
        System.out.println("Enter the password you use to login to the server:");
        String password = scanner.nextLine();

        String server;
        Integer dbPort;
        Integer outPort;
        Integer sslPort;
        PrivateKey serverPrivKey;
        try {
            File configFile = new File("./server-config/" + serverName +
                    ".config");
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            server = br.readLine();
            dbPort = Integer.parseInt(br.readLine());
            outPort = Integer.parseInt(br.readLine());
            sslPort = Integer.parseInt(br.readLine());
            File privKeyFile = new File("./server-config/" + serverName +
                    ".priv");
            ObjectInputStream privateKeyIS = new ObjectInputStream(
                    new FileInputStream(privKeyFile));
            serverPrivKey = (PrivateKey) privateKeyIS.readObject();
        } catch(IOException | NumberFormatException e) {
            System.err.println("Could not read config file");
            return;
        } catch(ClassNotFoundException e) {
            System.err.println("Could not read private key file");
            return;
        }

        SQL_Connection sqlConnection = new SQL_Connection(server, dbPort,
                username, password);

        try {
            waitForIncomingCert(serverName, outPort, serverPrivKey);
            System.out.println("have cert");
            ServerSocket ss = SSL_Server_Methods.setup_SSLServerSocket(serverName, sslPort);
            waitForIncomingSSL(ss, sqlConnection);
            while (true) {
                promptAdmin(sqlConnection);
            }
        } catch (SQLException e) {
            System.out.println("Wrong password, please try again.");
            e.printStackTrace();
        } catch(Exception e) {
            System.err.println("We should change setup_SSLServerSocket to not" +
                    " throw exception");
            e.printStackTrace();
        }
    }

    private static void waitForIncomingCert(String serverName, Integer outPort,
                                            PrivateKey serverPrivKey) {
        try {
            ServerSocket ss = new ServerSocket(outPort);
            while (true){
                Socket s = ss.accept();
                new Unsecured_Server_Handler(s, serverPrivKey, serverName).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("got icoming cert?");
    }

    private static void waitForIncomingSSL(ServerSocket ss,
                                        SQL_Connection sqlConnection) {
        try {
            Socket s = ss.accept();
            new SSL_Server_Actual(s, sqlConnection).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void promptAdmin(SQL_Connection sql_connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter 'u' to download user logs");
            System.out.println("Enter 'd' to delete a user");
            String command = scanner.nextLine();
            String[] elements = command.trim().split("\\s+");

            switch (elements[0]) {
                case "u":
                    downloadUserLogs(sql_connection);
                    return;
                case "d":
                    deleteUser(sql_connection);
                    return;
                default:
                    System.out.println("Sorry, your command was not " +
                            "understood.");
            }
        }
    }

    private static void downloadUserLogs(SQL_Connection sql_connection) {
        //TODO
    }

    private static void deleteUser(SQL_Connection sql_connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter the username of the user to delete:");
            String userToDelete = scanner.nextLine();

            System.out.println("Please confirm the username of the user to " +
                    "delete by entering it again:");
            String confirm = scanner.nextLine();
            if (userToDelete.equals(confirm)) {
                //TODO
            }
        }
    }

}
