package org.cs5431;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.secondPwdHash;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.sendJson;
import static org.cs5431.JSON.sendJsonArray;

public class SSLServer extends Thread {
    protected Socket s;
    private SQL_Connection sqlConnection;

    public SSLServer(Socket socket, SQL_Connection sqlConnection){
        this.s = socket;
        this.sqlConnection = sqlConnection;
    }

    public void run(){
        try {
        while(true) {
                JSONObject jsonObject = receiveJson(s);
                if (DEBUG_MODE) {
                    System.out.println("received json: " + jsonObject.toString());
                }
                String type = jsonObject.getString("msgType");
                JSONObject response;
                switch (type) {
                    case "registration":
                        response = register(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "login":
                        if (DEBUG_MODE) {
                            System.out.println("trying to login");
                        }
                        response = login(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "getPrivKeySalt":
                        response = getPrivKeySalt(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "changePwd":
                        response = changePwd(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "upload":
                        response = upload(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "download":
                        response = download(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "rename":
                        response = rename(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "add privilege":
                        response = addPriv(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "remove privilege":
                        response = removePriv(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "delete":
                        response = delete(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "edit user details":
                        response = editDetails(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "getFileLogs":
                        JSONArray arr = getFileLog(jsonObject, sqlConnection);
                        sendJsonArray(arr, s);
                        break;
                    case "getChildren":
                        JSONArray arr2 = getChildren(jsonObject, sqlConnection);
                        sendJsonArray(arr2, s);
                        break;
                    default:
                        response = makeErrJson("Did not understand " +
                                "incoming request");
                        sendJson(response, s);
                        break;
                }
            }
        } catch (NullPointerException | SocketException e) {
            if (DEBUG_MODE) {
                System.err.println("Probably the client disconnecting, if so this" +
                        " can be safely ignored:");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            String encPwd = secondPwdHash(hashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject auth = sqlConnection.authenticate(jsonObject, encPwd);
            if (auth != null) {
                return auth;
            }
        }
        if (DEBUG_MODE) {
            System.out.println("Sending json error - username does not exist");
        }
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
            String newEncPwd = secondPwdHash(newHashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject verification = sqlConnection.changePassword(jsonObject, newEncPwd);
            if (verification != null) {
                if (DEBUG_MODE) {
                    System.out.println("chaning pwd: " + verification);
                }
                return verification;
            }
        }
        if (DEBUG_MODE) {
            System.out.println("Sending error -- unable to authenticate");
        }
        JSONObject jsonErr = new JSONObject();
        jsonErr.put("msgType", "error");
        jsonErr.put("message", "Change password failed");
        return jsonErr;
    }

    private JSONObject upload(JSONObject jsonObject, SQL_Connection
            sqlConnection) throws Exception {
        int fsoid = sqlConnection.createFso(jsonObject);
        if (fsoid != -1) {
            JSONObject response = new JSONObject();
            response.put("msgType","uploadAck");
            response.put("fsoid", fsoid);
            return response;
        }
        System.out.println("Sending error -- unable to create new folder");
        JSONObject jsonErr = new JSONObject();
        jsonErr.put("msgType", "error");
        jsonErr.put("message", "Unable to upload");
        return jsonErr;
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

        //copied from FileController
        /*if (priv == PrivType.EDIT) {
            rmUser = sql_connection.removeEditPriv(systemObject.getId(), user.getId(), userId);
        } else {
            rmUser = sql_connection.removeViewPriv(systemObject.getId(), user.getId(), userId);
        }*/
        return null;
    }

    private JSONObject addPriv(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //TODO
        //copied from FileController:
        /*if (priv == PrivType.EDIT) {
            newUser = sql_connection.addEditPriv(json);
        } else {
            newUser = sql_connection.addViewPriv(json);
        }*/
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
        String hashedPW = secondPwdHash(pwd, salt);
        String returnedValues[] = new String[2];
        returnedValues[0] = hashedPW;
        returnedValues[1] = Base64.getEncoder().encodeToString(salt);
        return returnedValues;
    }
}
