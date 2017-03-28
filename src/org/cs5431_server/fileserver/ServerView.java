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
            JSONObject response;
            switch (type) {
                case "request jkb":
                    response = requestJKB(serverPrivKey);   //TODO 
                    // pass socket?
                    break;
                default:
                    response = makeErrJson("Did not understand " +
                        "incoming request");
                    break;
            }
            //TODO send response
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
            JSONObject response;
            switch (type) {
                case "registration":
                    response = register(jsonObject, sqlConnection);
                    break;
                case "login":
                    response = login(jsonObject, sqlConnection);
                    break;
                case "upload":
                    response = upload(jsonObject, sqlConnection);
                    break;
                case "download":
                    response = download(jsonObject, sqlConnection);
                    break;
                case "rename":
                    response = rename(jsonObject, sqlConnection);
                    break;
                case "add privilege":
                    response = addPriv(jsonObject, sqlConnection);
                    break;
                case "remove privilege":
                    response = removePriv(jsonObject, sqlConnection);
                    break;
                case "delete":
                    response = delete(jsonObject, sqlConnection);
                    break;
                case "edit user details":
                    response = editDetails(jsonObject, sqlConnection);
                    break;
                case "getFileLogs":
                    JSONArray arr = getFileLog(jsonObject, sqlConnection);
                    //TODO send arr instead of response
                    break;
                case "getChildren":
                    response = getChildren(jsonObject, sqlConnection);
                    break;
                default:
                    response = makeErrJson("Did not understand " +
                            "incoming request");
                    break;
            }
            //TODO send response. BRANDON! SEND THE RESPONSE THROUGH SSL HERE
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject requestJKB(PrivateKey serverPrivKey) {
        //TODO brandon write here
        //serverPrivKey is the signing key
        return null;
    }

    private static JSONObject register(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        boolean isUniqueUsername = sqlConnection.isUniqueUsername(jsonObject
                .getString("username"));
        if (!isUniqueUsername)
            return makeErrJson("Username has already been chosen");

        String hashedPwd = jsonObject.getString("hashedPwd");
        String hashAndSalt[] = generatePasswordHash(hashedPwd);
        String hash = hashAndSalt[0];
        String pwdSalt = hashAndSalt[1];
        JSONObject response = sqlConnection.createUser(jsonObject, hash, 
                pwdSalt);
        if (response == null)
            return makeErrJson("Failed to register user");
        return response;
    }

    private static JSONObject login(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String pwdSalt = sqlConnection.getSalt(jsonObject.getString
                ("username"));
        String hashedPwd = jsonObject.getString("hashedPwd");
        String encPwd  = hash(hashedPwd, Base64.getDecoder().decode(pwdSalt));
        return sqlConnection.authenticate(jsonObject, encPwd);
    }

    private static JSONObject upload(JSONObject jsonObject, SQL_Connection
            sqlConnection) {

        //TODO: make it read from User or jsonObject?
        //sqlConnection.createFso(jsonObject);
        return null;
    }

    private static JSONObject download(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        //TODO CHECK FOR PERMISSIONS

        //TODO figure out which method to call
        //sqlConnection.getFile()?
        JSONObject downloadAck = new JSONObject();
        downloadAck.put("fsoid", fsoid);
        downloadAck.put("fileIV", "");
        downloadAck.put("fsoName", "");
        downloadAck.put("encFile","");
        downloadAck.put("encFileSK","");
        downloadAck.put("dateModified","");
        downloadAck.put("size","");
        return null;
    }

    private static JSONObject rename(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private static JSONObject removePriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private static JSONObject addPriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private static JSONObject delete(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private static JSONObject editDetails(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private static JSONArray getFileLog(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return sqlConnection.getFileLog(jsonObject);
    }

    private static JSONObject getChildren(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO HALP sqlConnection.getChildren();
        return null;
    }

    private static JSONObject makeErrJson(String message) {
        //TODO
        JSONObject response = new JSONObject();
        response.put("messageType","error");
        response.put("message", message);
        return response;
    }

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

    public static String hash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 10000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters(256);
        return Base64.getEncoder().encodeToString(kp.getKey());
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
