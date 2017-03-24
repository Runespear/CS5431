package org.cs5431_server.fileserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        String dbPort;
        String outPort;
        try {
            File configFile = new File("./user-config/" + serverName +
                    ".config");
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            server = br.readLine();
            dbPort = br.readLine();
            outPort = br.readLine();
        } catch(IOException e) {
            System.err.println("Could not read config file");
            return;
        }

        //TODO listen to incoming packets on some other thread here
        waitForIncoming(server, dbPort, outPort, username, password);
        try {
            while (true) {
                promptAdmin(server, dbPort, username, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void waitForIncoming(String server, String dbPort, String
            outPort, String username, String password) {

    }

    private static void promptAdmin(String server, String dbPort, String
            username, String password) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter 'u' to download user logs");
            System.out.println("Enter 'd' to delete a user");
            String command = scanner.nextLine();
            String[] elements = command.trim().split("\\s+");

            switch (elements[0]) {
                case "u":
                    downloadUserLogs(server, dbPort, username, password);
                    return;
                case "d":
                    deleteUser(server, dbPort, username, password);
                    return;
                default:
                    System.out.println("Sorry, your command was not " +
                            "understood.");
            }
        }
    }

    private static void downloadUserLogs(String server, String dbPort, String
            username, String password) {

    }

    private static void deleteUser(String server, String dbPort, String
            username, String password) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter the username of the user to delete:");
            String userToDelete = scanner.nextLine();

            System.out.println("Please confirm the username of the user to " +
                    "delete by entering it again:");
            String confirm = scanner.nextLine();
            if (userToDelete.equals(confirm)) {
                String url = "jdbc:mysql://" + server + ":" + dbPort + "/cs5431";
                Connection connection = DriverManager.getConnection(url, username, password);
                PreparedStatement deleteUser = null; //TODO
                //TODO: maybe call SQL_Connection in util instead
            }
        }
    }

}
