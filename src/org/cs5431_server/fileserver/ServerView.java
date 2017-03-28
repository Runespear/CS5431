package org.cs5431_server.fileserver;

import org.cs5431_client.util.SQL_Connection;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
        System.out.print("Options:");
        int i = 1;
        for (File f : configFiles) {
            String fileName = f.getName();
            int pos = fileName.lastIndexOf(".");
            fileName = fileName.substring(0, pos);
            System.out.println(" " + i+": " +fileName);
            i++;
        }
        String serverName = scanner.nextLine();
        System.out.println("Enter the username you use to login to your MySQL" +
                " server:");
        String username = scanner.nextLine();
        System.out.println("Enter the password you use to login to your MySQL" +
                " server:");
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
        //TODO listen to incoming packets on some other thread here
        //TODO threading???
        waitForIncomingCert(server, outPort, serverPrivKey);
        waitForIncomingSSL(server, sslPort, sqlConnection);
        try {
            while (true) {
                promptAdmin(sqlConnection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void waitForIncomingCert(String server, Integer sslPort,
                                            PrivateKey serverPrivKey) {
        try {
            ServerSocket serverSocket = new ServerSocket(sslPort);
            Socket client = serverSocket.accept();
            BufferedReader br = new BufferedReader(new InputStreamReader(client
                    .getInputStream()));
            JSONObject jsonObject = new JSONObject(br.readLine());
            String type = jsonObject.getString("messageType");
            switch (type) {
                case "request jkb":
                    requestJKB(serverPrivKey);   //TODO pass socket?
                    break;
                default:
                    sendErrJson();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void waitForIncomingSSL(String server, Integer sslPort,
                                        SQL_Connection sqlConnection) {
        try {
            //TODO change to ssl
            ServerSocket serverSocket = new ServerSocket(sslPort);
            Socket client = serverSocket.accept();
            BufferedReader br = new BufferedReader(new InputStreamReader(client
                    .getInputStream()));
            JSONObject jsonObject = new JSONObject(br.readLine());
            String type = jsonObject.getString("messageType");
            switch (type) {
                case "registration":
                    register(jsonObject, sqlConnection);
                    break;
                case "login":
                    login(jsonObject, sqlConnection);   //TODO pass socket?
                    break;
                case "upload":
                    upload(jsonObject, sqlConnection);
                    break;
                case "download":
                    download(jsonObject, sqlConnection);
                    break;
                case "rename":
                    rename(jsonObject, sqlConnection);
                    break;
                case "add privilege":
                    addPriv(jsonObject, sqlConnection);
                    break;
                case "remove privilege":
                    removePriv(jsonObject, sqlConnection);
                    break;
                case "delete":
                    delete(jsonObject, sqlConnection);
                    break;
                case "edit user details":
                    editDetails(jsonObject, sqlConnection);
                    break;
                case "file log":
                    getFileLog(jsonObject, sqlConnection);
                    break;
                case "get file list":
                    getFileList(jsonObject, sqlConnection);
                    break;
                default:
                    sendErrJson();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void requestJKB(PrivateKey serverPrivKey) {
        //TODO brandon write here
        //serverPrivKey is the signing key

    }

    private static void register(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO: make the privKey and pubKey in the jsonObject?
        //sqlConnection.createUser(jsonObject);
    }

    private static void login(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO: make sql_connection's authenticate not static?
        //sqlConnection.authenticate(jsonObject);
    }

    private static void upload(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO: make it read from User or jsonObject?
        //sqlConnection.createFso(jsonObject);
    }

    private static void download(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO figure out which method to call
        //sqlConnection.getFile()?
    }

    private static void rename(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void removePriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void addPriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void delete(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void editDetails(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void getFileLog(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void getFileList(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
    }

    private static void sendErrJson() {
        //TODO
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
