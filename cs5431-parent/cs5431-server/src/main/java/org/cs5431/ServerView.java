package org.cs5431;

import org.cs5431.SQL_Connection;

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
            waitForIncomingCert wfic = new waitForIncomingCert(serverName,
                    outPort, serverPrivKey);
            new Thread(wfic).start();

            waitForIncomingSSL wfis = new waitForIncomingSSL(sqlConnection,
                    serverName, sslPort);
            new Thread(wfis).start();

            //promptAdmin pa = new promptAdmin(sqlConnection);
            //new Thread(pa).start();

        } catch(Exception e) {
            System.err.println("We should change setup_SSLServerSocket to not" +
                    " throw exception");
            e.printStackTrace();
        }
    }
}

class waitForIncomingCert implements Runnable {
    String serverName;
    Integer outPort;
    PrivateKey serverPrivKey;

    public waitForIncomingCert(String serverName, Integer outPort,
                               PrivateKey serverPrivKey) {
        this.serverName = serverName;
        this.outPort = outPort;
        this.serverPrivKey = serverPrivKey;
    }

    public void run() {
        try {
            ServerSocket ss = new ServerSocket(outPort);
            while (true) {
                Socket s = ss.accept();
                new Unsecured_Server_Handler(s, serverPrivKey, serverName).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class waitForIncomingSSL implements Runnable {
    SQL_Connection sqlConnection;
    String serverName;
    Integer sslPort;

    public waitForIncomingSSL(SQL_Connection sqlConnection, String
            serverName, Integer sslPort) {
        this.sqlConnection = sqlConnection;
        this.serverName = serverName;
        this.sslPort = sslPort;
    }

    public void run() {
        try {
            ServerSocket ss = SSL_Server_Methods.setup_SSLServerSocket
                    (serverName, sslPort);
            while (true) {
                Socket s = ss.accept();
                new SSL_Server_Actual(s, sqlConnection).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class promptAdmin implements Runnable {
    SQL_Connection sqlConnection;

    public promptAdmin(SQL_Connection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public void run() {
        try {
            while (true) {
                prompt(sqlConnection);
            }
        } catch (SQLException e) {
            System.out.println("Wrong password, please try again.");
            e.printStackTrace();
        }
    }

    private void prompt(SQL_Connection sql_connection) throws SQLException {
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