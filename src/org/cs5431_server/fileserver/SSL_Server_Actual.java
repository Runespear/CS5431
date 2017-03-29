package org.cs5431_server.fileserver;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.cs5431_client.util.SQL_Connection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
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
        while(true) {
                JSONObject jsonObject = receiveJson();
                System.out.println("received json: " + jsonObject.toString());
                String type = jsonObject.getString("msgType");
                JSONObject response;
                switch (type) {
                    case "registration":
                        response = register(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "login":
                        System.out.println("trying to login");
                        response = login(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "getPrivKeySalt":
                        response = getPrivKeySalt(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "changePwd":
                        response = changePwd(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "upload":
                        response = upload(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "download":
                        response = download(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "rename":
                        response = rename(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "add privilege":
                        response = addPriv(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "remove privilege":
                        response = removePriv(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "delete":
                        response = delete(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "edit user details":
                        response = editDetails(jsonObject, sqlConnection);
                        sendJson(response);
                        break;
                    case "getFileLogs":
                        JSONArray arr = getFileLog(jsonObject, sqlConnection);
                        sendJsonArray(arr);
                        break;
                    case "getChildren":
                        JSONArray arr2 = getChildren(jsonObject, sqlConnection);
                        sendJsonArray(arr2);
                        break;
                    default:
                        response = makeErrJson("Did not understand " +
                                "incoming request");
                        sendJson(response);
                        break;
                }
            }
        } catch (NullPointerException | SocketException e) {
            System.err.println("Probably the client disconnecting, if so this" +
                    " can be safely ignored:");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject receiveJson() throws IOException, ClassNotFoundException {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(s.getInputStream()));
        String str;
        str = r.readLine();
        System.out.println(str);
        System.out.flush();
        return new JSONObject(str);
    }

    private void sendJson (JSONObject json) throws IOException {
        System.out.println("sending json");
        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(s.getOutputStream()));
        String str = json.toString();
        System.out.println(str);
        w.write(str + '\n');
        w.flush();
    }

    private void sendJsonArray(JSONArray json) throws IOException {
        System.out.println("sending json");
        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(s.getOutputStream()));
        String str = json.toString();
        System.out.println(str);
        w.write(str + '\n');
        w.flush();
    }

    private JSONObject register(JSONObject jsonObject, SQL_Connection
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

    private JSONObject login(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String pwdSalt = sqlConnection.getSalt(jsonObject.getString
                ("username"));
        String hashedPwd = jsonObject.getString("hashedPwd");
        if (pwdSalt != null) {
            String encPwd = hash(hashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject auth = sqlConnection.authenticate(jsonObject, encPwd);
            if (auth != null) {
                return auth;
            }
        }
        System.out.println("Sending json error - username does not exist");
        JSONObject jsonErr = new JSONObject();
        jsonErr.put("msgType", "error");
        jsonErr.put("message", "Login failed");
        return jsonErr;
    }


    private JSONObject getPrivKeySalt(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String privSalt = sqlConnection.getPrivKeySalt(jsonObject.getString
                ("username"));
        if (privSalt != null) {
            JSONObject salt = new JSONObject();
            salt.put("msgType", "getPrivKeySaltAck");
            salt.put("privKeySalt", privSalt);
            if (salt != null) {
                return salt;
            }
        }

        System.out.println("Sending error -- unable to get salt");
        JSONObject jsonErr = new JSONObject();
        jsonErr.put("msgType", "error");
        jsonErr.put("message", "Unable to get privKeySalt");
        return jsonErr;
    }

    private JSONObject changePwd(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String newHashedPwd = jsonObject.getString("newHashedPwd");
        String pwdSalt = sqlConnection.getSalt(jsonObject.getString
                ("username"));
        if (pwdSalt != null) {
            String newEncPwd = hash(newHashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject verification = sqlConnection.changePassword(jsonObject, newEncPwd);
            if (verification != null) {
                System.out.println("chaning pwd: " + verification);
                return verification;
            }
        }

        System.out.println("Sending error -- unable to authenticate");
        JSONObject jsonErr = new JSONObject();
        jsonErr.put("msgType", "error");
        jsonErr.put("message", "Change password failed");
        return jsonErr;
    }

    private JSONObject upload(JSONObject jsonObject, SQL_Connection
            sqlConnection) throws Exception {
        int fsoid = sqlConnection.createFso(jsonObject);
        JSONObject response = new JSONObject();
        response.put("msgType","uploadAck");
        response.put("fsoid", fsoid);
        return response;
    }

    private JSONObject download(JSONObject jsonObject, SQL_Connection
            sqlConnection) throws Exception {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");

        JSONObject downloadAck = sqlConnection.getFile(jsonObject);
        return downloadAck;
    }

    private JSONObject rename(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private JSONObject removePriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private JSONObject addPriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private JSONObject delete(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private JSONObject editDetails(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return null;
    }

    private JSONArray getFileLog(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        return sqlConnection.getFileLog(jsonObject);
    }

    private JSONArray getChildren(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        return sqlConnection.getChildren(jsonObject);
    }

    private JSONObject makeErrJson(String message) {
        //TODO
        JSONObject response = new JSONObject();
        response.put("msgType","error");
        response.put("message", message);
        return response;
    }

    private String[] generatePasswordHash(String pwd) {
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
