package org.cs5431_server.fileserver;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.symmetric.ARC4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class ServerView {
    public static void main(String[] args) {
        System.out.println("Password hash of qwerty:");
        String values[] = generatePasswordHash("qwerty");
        System.out.println("Hash: " + values[0]);
        System.out.println("Salt: " + values[1]);
        System.out.println("Verify hash of qwerty: " + verifyPassword
                ("qwerty", values[0], values[1]));
        System.out.println("Password hash of password:");
        values = generatePasswordHash("password");
        System.out.println("Hash: " + values[0]);
        System.out.println("Salt: " + values[1]);
        System.out.println("Verify hash of password: " + verifyPassword
                ("password", values[0], values[1]));
        System.out.println("Fail to verify against wrong pwd: " +
                !verifyPassword("qwerty", values[0], values[1]));

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

    //TODO: move to appropriate place
    private static String[] generatePasswordHash(String pwd) {
        Random random = new SecureRandom();
        //TODO: 32 is currently the salt length. Is this correct?
        byte salt[] = new byte[32];
        random.nextBytes(salt);
        String hashedPW = hash(pwd, salt);
        String returnedValues[] = new String[2];
        returnedValues[0] = hashedPW;
        returnedValues[1] = Base64.getEncoder().encodeToString(salt);
        return returnedValues;
    }

    private static String hash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 1);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters(256);
        return Base64.getEncoder().encodeToString(kp.getKey());
    }

    private static boolean verifyPassword(String pwd, String actualHash, String
            salt) {
        String cmpHash = hash(pwd, Base64.getDecoder().decode(salt));
        return (cmpHash.equals(actualHash));
    }
}
