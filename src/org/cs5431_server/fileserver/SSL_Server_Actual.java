package org.cs5431_server.fileserver;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.cs5431_client.util.SQL_Connection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class SSL_Server_Actual extends Thread {
    protected Socket s;
    private SQL_Connection sqlConnection;

    public SSL_Server_Actual(Socket socket, SQL_Connection sqlConnection){
        this.s = socket;
        this.sqlConnection = sqlConnection;
    }

    public void run(){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
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
                    sendJson(response);
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void sendJson(JSONObject json) {
        //TODO: hi ruixin help me do this
    }

    private static void sendJsonArray(JSONArray json) {
        //TODO
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
        int fsoid = sqlConnection.createFso(jsonObject);
        JSONObject response = new JSONObject();
        response.put("messageType","uploadAck");
        response.put("fsoid", fsoid);
        return response;
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
        return downloadAck;
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

}
