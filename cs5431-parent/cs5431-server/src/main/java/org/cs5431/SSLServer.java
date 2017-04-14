package org.cs5431;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Constants.MAX_LOGINS_PER_MINUTE;
import static org.cs5431.Encryption.secondPwdHash;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.sendJson;
import static org.cs5431.JSON.sendJsonArray;

public class SSLServer extends Thread {
    protected Socket s;
    private SQL_Connection sqlConnection;
    private int failedLogins = 0;
    private Date failedTime;
    private String sourceIp;

    public SSLServer(Socket socket, SQL_Connection sqlConnection){
        this.s = socket;
        this.sqlConnection = sqlConnection;
        this.sourceIp = s.getRemoteSocketAddress().toString();
    }

    public void run(){
        failedTime = new Date();
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
                        response = login(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "getPrivKeySalt":
                         response = getPrivKeySalt(jsonObject, sqlConnection);
                         sendJson(response, s);
                         break;
                    case "editPassword":
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
                    case "renameKeys":
                        response = renameKeys(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "addPriv":
                        response = addPriv(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "removePriv":
                        response = removePriv(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "delete":
                        response = delete(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "overwrite":
                        response = overwrite(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "overwriteKeys":
                        response = overwriteKeys(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "editEmail":
                        response = changeEmail(jsonObject, sqlConnection);
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
                    case "username":
                        response = getUsername(jsonObject, sqlConnection);
                        sendJson(response, s);
                        break;
                    case "userid":
                        response = getUserId(jsonObject, sqlConnection);
                        sendJson(response, s);
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
                pwdSalt, sourceIp);
        if (response == null)
            return makeErrJson("Failed to register user");
        return response;
    }

    private JSONObject getPrivKeySalt(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String privSalt = sqlConnection.getPrivKeySalt(jsonObject.getString
            ("username"));
        if (privSalt != null) {
            JSONObject salt = new JSONObject();
            salt.put("msgType", "getPrivKeySaltAck");
            salt.put("privKeySalt", privSalt);
            return salt;
        }

        if (DEBUG_MODE) {
            System.out.println("Sending error -- unable to get salt");
        }
        JSONObject jsonErr = new JSONObject();
        jsonErr.put("msgType", "error");
        jsonErr.put("message", "Unable to get privKeySalt");
        return jsonErr;
    }

    private JSONObject login(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        //rate limiting: check if too many failed logins within this one minute
        Date now = new Date();
        if (failedLogins >= MAX_LOGINS_PER_MINUTE && withinOneMinute(now,
                failedTime)) {
            JSONObject jsonErr = new JSONObject();
            jsonErr.put("msgType", "error");
            jsonErr.put("message", "Too many failed logins recently");
            return jsonErr;
        }

        String pwdSalt = sqlConnection.getSalt(jsonObject.getString
                ("username"), sourceIp);
        String hashedPwd = jsonObject.getString("hashedPwd");
        if (pwdSalt != null) {
            String encPwd = secondPwdHash(hashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject auth = sqlConnection.authenticate(jsonObject, encPwd, sourceIp);
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
        //Rate limiting: keep track of failed logins
        if (withinOneMinute(now,failedTime))
            failedLogins++;
        else {
            failedTime = new Date();
            failedLogins = 1;
        }
        return jsonErr;
    }

    private boolean withinOneMinute(Date now, Date then) {
        return (now.getTime() - then.getTime() <= 60*1000);
    }

    private JSONObject changePwd(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String newHashedPwd = jsonObject.getString("newHashedPwd");
        String pwdSalt = sqlConnection.getSalt(jsonObject.getString("username"), sourceIp);
        if (pwdSalt != null) {
            String newEncPwd = secondPwdHash(newHashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject verification = sqlConnection.changePassword(jsonObject, newEncPwd, sourceIp);
            if (verification != null) {
                if (DEBUG_MODE) {
                    System.out.println("changing pwd: " + verification);
                }
                return verification;
            }
        }
        //TODO: return error that says unable to authenticate IF AND ONLY IF
        //the authentication is the part that failed!
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
        int fsoid = sqlConnection.createFso(jsonObject, s.getRemoteSocketAddress().toString());
        if (fsoid != -1) {
            JSONObject response = new JSONObject();
            response.put("msgType","uploadAck");
            response.put("fsoid", fsoid);
            return response;
        }
        System.out.println("Sending error -- unable to create new folder");
        return makeErrJson("Unable to upload");
    }

    private JSONObject download(JSONObject jsonObject, SQL_Connection
            sqlConnection) throws Exception {
        return sqlConnection.getFile(jsonObject);
    }

    private JSONObject rename(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        String newName = jsonObject.getString("newName");
        String newFsoNameIV = jsonObject.getString("newFsoNameIV");

        if (sqlConnection.renameFso(fsoid, uid, newName, newFsoNameIV) ==
                fsoid) {
            JSONObject response = new JSONObject();
            response.put("msgType","renameAck");
            response.put("fsoid", fsoid);
            response.put("uid", uid);
            return response;
        }

        return makeErrJson("Unable to rename");
    }

    private JSONObject overwrite(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        String newFileIV = jsonObject.getString("newFileIV");
        String encFile = jsonObject.getString("encFile");

        if (sqlConnection.overwrite(fsoid, uid, newFileIV, encFile) ==
                fsoid) {
            JSONObject response = new JSONObject();
            response.put("msgType","overwriteAck");
            response.put("fsoid", fsoid);
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("Unable to overwrite file");
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
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");

        if (sqlConnection.deleteFile(fsoid, uid) == fsoid) {
            JSONObject response = new JSONObject();
            response.put("msgType","deleteAck");
            response.put("fsoid", fsoid);
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("Unable to delete file");
    }

    private JSONObject changeEmail(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        int uid = jsonObject.getInt("uid");
        String oldEmail = jsonObject.getString("oldEmail");
        String newEmail = jsonObject.getString("newEmail");

        if (sqlConnection.changeEmail(uid, oldEmail, newEmail)) {
            JSONObject response = new JSONObject();
            response.put("msgType","editEmailAck");
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("Unable to change email");
    }

    private JSONArray getFileLog(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        return sqlConnection.getFileLog(jsonObject);
    }

    private JSONArray getChildren(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        return sqlConnection.getChildren(jsonObject);
    }

    private JSONObject getUsername(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        int uid = jsonObject.getInt("uid");
        String username = sqlConnection.getUsername(uid);
        if (username != null) {
            JSONObject response = new JSONObject();
            response.put("msgType", "usernameAck");
            response.put("username", username);
            return response;
        }
        return makeErrJson("Could not find user with that username");
    }

    private JSONObject getUserId(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        String username = jsonObject.getString("username");
        int userId = sqlConnection.getUserId(username);
        if (userId != -1) {
            JSONObject response = new JSONObject();
            response.put("msgType", "useridAck");
            response.put("userid", userId);
            return response;
        }
        return makeErrJson("Could not find user with that userid");
    }

    private JSONObject renameKeys(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        JSONObject response = getFileSK(jsonObject, sqlConnection);
        if (response != null)
            response.put("msgType", "renameKeysAck");
        return response;
    }

    private JSONObject overwriteKeys(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        JSONObject response = getFileSK(jsonObject, sqlConnection);
        if (response != null)
            response.put("msgType", "overwriteKeysAck");
        return response;
    }

    private JSONObject getFileSK(JSONObject jsonObject, SQL_Connection
            sqlConnection) {
        JSONObject response = new JSONObject();
        String fileSK = sqlConnection.getFileSK(jsonObject.getInt("fsoid"),
                jsonObject.getInt("uid"));
        if (fileSK != null) {
            response.put("fileSK", fileSK);
            response.put("fsoid", jsonObject.getInt("fsoid"));
            response.put("uid", jsonObject.getInt("uid"));
            return response;
        }
        return null;
    }

    private JSONObject makeErrJson(String message) {
        //TODO
        JSONObject response = new JSONObject();
        response.put("msgType","error");
        response.put("message", message);
        return response;
    }

    public static String[] generatePasswordHash(String pwd) {
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
