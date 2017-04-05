package org.cs5431;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.sql.SQLException;
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
        boolean acceptName = false;

        while (!acceptName) {
            for (File f : configFiles) {
                String fileName = f.getName();
                int pos = fileName.lastIndexOf(".");
                fileName = fileName.substring(0, pos);
                if (serverName.equals(fileName)) {
                    acceptName = true;
                    break;
                } else {
                    System.out.println("Please enter a valid server name:");
                    serverName = scanner.nextLine();
                }
            }
        }
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
            CertSocketThread cst = new CertSocketThread(serverName,
                    outPort, serverPrivKey);
            new Thread(cst).start();

            SSLSocketThread sst = new SSLSocketThread(sqlConnection,
                    serverName, sslPort);
            new Thread(sst).start();

            PromptAdminThread pat = new PromptAdminThread(sqlConnection);
            new Thread(pat).start();

        } catch(Exception e) {
            System.err.println("We should change setup_SSLServerSocket to not" +
                    " throw exception");
            e.printStackTrace();
        }
    }
}