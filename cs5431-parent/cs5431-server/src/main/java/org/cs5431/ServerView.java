package org.cs5431;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
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

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
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
        System.out.println("Enter the threshold for maximum number of failures per user per day: (leave blank for default = 20)");
        String threshold = scanner.nextLine();
        Integer failures_per_uid = -1;
        if (threshold.isEmpty())
            failures_per_uid = 20;
        else {
            try {
                failures_per_uid = Integer.parseInt(threshold);
            } catch (NumberFormatException e) {}
        }
        while (failures_per_uid == -1) {
            System.out.println("Please enter a valid number or leave blank for default:");
            threshold = scanner.nextLine();
            if (threshold.isEmpty())
                failures_per_uid = 20;
            else {
                try {
                    failures_per_uid = Integer.parseInt(threshold);
                } catch (NumberFormatException e) {}
            }
        }

        System.out.println("Enter the threshold for maximum number of failures per IP per day: (leave blank for default = 20)");
        threshold = scanner.nextLine();
        Integer failures_per_ip = -1;
        if (threshold.isEmpty())
            failures_per_ip = 20;
        else {
            try {
                failures_per_ip = Integer.parseInt(threshold);
            } catch (NumberFormatException e) {}
        }
        while (failures_per_ip == -1) {
            System.out.println("Please enter a valid number or leave blank for default:");
            threshold = scanner.nextLine();
            if (threshold.isEmpty())
                failures_per_ip = 20;
            else {
                try {
                    failures_per_ip = Integer.parseInt(threshold);
                } catch (NumberFormatException e) {}
            }
        }

        System.out.println("Enter the threshold for maximum total number of failures per day: (leave blank for default = 200)");
        threshold = scanner.nextLine();
        Integer total_failures = -1;
        if (threshold.isEmpty())
            total_failures = 200;
        else {
            try {
                total_failures = Integer.parseInt(threshold);
            } catch (NumberFormatException e) {}
        }
        while (total_failures == -1) {
            System.out.println("Please enter a valid number or leave blank for default:");
            threshold = scanner.nextLine();
            if (threshold.isEmpty())
                total_failures = 200;
            else {
                try {
                    total_failures = Integer.parseInt(threshold);
                } catch (NumberFormatException e) {}
            }
        }





        String server;
        Integer dbPort;
        Integer outPort;
        Integer sslPort;
        PrivateKey serverPrivKey;
        try {
            File configFile = new File("./server-config/" + serverName +
                    ".config");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(configFile), StandardCharsets.UTF_8.name()));
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

        SQL_Accounts sql_accounts = new SQL_Accounts(server, dbPort,
                username, password);
        if (!sql_accounts.checkCredentials()) {
            System.err.println("Could not connect to SQL server with given " +
                    "credentials");
            return;
        }
        SQL_Files sql_files = new SQL_Files(server, dbPort, username, password);

        String finalUsername = username;
        String finalPassword = password;
        Thread t = new Thread(() -> {
            while(true) try {
                new IntrusionDetection(server, dbPort, finalUsername, finalPassword,
                        failures_per_uid, failures_per_ip, total_failures);
                Thread.sleep(1000 * 60 * 60 * 24);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        });
        t.start();

        System.out.println("Enter the username you use to login to the admin email:");
        username = scanner.nextLine();
        username = "psfs5431@gmail.com"; //TODO: to be removed
        System.out.println("Enter the password you use to login to the admin email:");
        password = scanner.nextLine();
        password = "theroadtoA+";//TODO: to be removed

        Email email = new Email(username, password);

        try {
            CertSocketThread cst = new CertSocketThread(serverName,
                    outPort, serverPrivKey);
            new Thread(cst).start();

            ServerSocket ss = setup_SSLServerSocket(serverName, sslPort);
            SSLSocketThread sst = new SSLSocketThread(sql_accounts, sql_files, ss, email);
            new Thread(sst).start();

            PromptAdminThread pat = new PromptAdminThread(sql_accounts, sql_files);
            new Thread(pat).start();

        } catch(Exception e) {
            System.err.println("We should change setup_SSLServerSocket to not" +
                    " throw exception");
            e.printStackTrace();
        }
    }

    public static ServerSocket setup_SSLServerSocket(String serverName, int
            Port_Number) throws Exception{

        System.setProperty("javax.net.ssl.keyStore", "./server-config/" + serverName + ".jks");

        Scanner scanner = new Scanner (System.in, StandardCharsets.UTF_8.name());
        System.out.println("Type in your password to access the keystore: ");
        String pass = scanner.nextLine(); //Obtain user's command

        System.setProperty("javax.net.ssl.keyStorePassword", pass);

        final SSLServerSocketFactory sslSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        final SSLServerSocket sslServerSocket = (SSLServerSocket) sslSocketFactory.createServerSocket(Port_Number);

        String[] ciphersuite = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};
        sslServerSocket.setEnabledCipherSuites(ciphersuite);
        return sslServerSocket;
    }
}