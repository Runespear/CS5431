package org.cs5431;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.Socket;
import java.net.SocketException;
import java.util.Base64;
import java.util.Date;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Constants.MAX_LOGINS_PER_MINUTE;
import static org.cs5431.Encryption.generatePasswordHash;
import static org.cs5431.Encryption.secondPwdHash;
import static org.cs5431.JSON.*;

public class SSLServer extends Thread {
    protected Socket s;
    private int failedLogins = 0;
    private int loggedInUid = -1;
    private Date failedTime;
    private String sourceIp;
    private SQL_Accounts sql_accounts;
    private SQL_Files sql_files;

    SSLServer(Socket socket, SQL_Accounts sql_accounts, SQL_Files sql_files){
        this.s = socket;
        this.sql_accounts = sql_accounts;
        this.sql_files = sql_files;
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
                boolean check = true;
                if (type.equals("editPassword") || type.equals("upload") ||
                        type.equals("download") || type.equals("rename") ||
                        type.equals("renameKeys") || type.equals("addPriv") ||
                        type.equals("removePriv") || type.equals("delete") ||
                        type.equals("overwrite") || type.equals
                        ("overwriteKeys") || type.equals("editEmail") || type
                        .equals("getFileLogs") || type.equals("getChildren")
                        || type.equals("logout") || type.equals
                        ("getEditorViewerList") || type.equals("deleteUser")) {
                    if (!isLoggedInUser(jsonObject)) {
                        check = false;
                        response = makeErrJson("Requesting user does not match " +
                                "logged in user");
                        sendJson(response, s);
                    }
                }

                if (check) {
                switch (type) {
                    case "registration":
                        response = register(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "login":
                        response = login(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "getPrivKeySalt":
                        response = getPrivKeySalt(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "changePwd":
                        response = changePwd(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "upload":
                        response = upload(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "download":
                        response = download(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "rename":
                        response = rename(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "renameKeys":
                        response = renameKeys(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "addPriv":
                        response = addPriv(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "removePriv":
                        response = removePriv(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "delete":
                        response = delete(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "overwrite":
                        response = overwrite(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "overwriteKeys":
                        response = overwriteKeys(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "changeEmail":
                        response = changeEmail(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "getFileLogs":
                        JSONArray arr = getFileLog(jsonObject, sql_files);
                        sendJsonArray(arr, s);
                        break;
                    case "getChildren":
                        JSONArray arr2 = getChildren(jsonObject, sql_files);
                        sendJsonArray(arr2, s);
                        break;
                    case "username":
                        response = getUsername(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "userid":
                        response = getUserId(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "logout":
                        response = logout(jsonObject);
                        sendJson(response, s);
                        break;
                    case "getEditorViewerList":
                        response = getEditorViewerList(jsonObject,
                                sql_files);
                        sendJson(response, s);
                        break;
                    case "deleteUser":
                        response = deleteUser(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    default:
                        response = makeErrJson("Did not understand " +
                                "incoming request");
                        sendJson(response, s);
                        break;
                    }
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

    private JSONObject register(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        boolean isUniqueUsername = sql_accounts.isUniqueUsername(jsonObject
                .getString("username"));
        if (!isUniqueUsername)
            return makeErrJson("Username has already been chosen");

        String hashedPwd = jsonObject.getString("hashedPwd");
        String hashAndSalt[] = generatePasswordHash(hashedPwd);
        String hash = hashAndSalt[0];
        String pwdSalt = hashAndSalt[1];
        JSONObject response = sql_accounts.createUser(jsonObject, hash,
                pwdSalt, sourceIp);
        if (response == null)
            return makeErrJson("Failed to register user");
        return response;
    }

    private JSONObject getPrivKeySalt(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        String privSalt = sql_accounts.getPrivKeySalt(jsonObject.getString
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

    private JSONObject login(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        //rate limiting: check if too many failed logins within this one minute
        Date now = new Date();
        if (failedLogins >= MAX_LOGINS_PER_MINUTE && withinOneMinute(now,
                failedTime)) {
            JSONObject jsonErr = new JSONObject();
            jsonErr.put("msgType", "error");
            jsonErr.put("message", "Too many failed logins recently");
            return jsonErr;
        }

        String pwdSalt = sql_accounts.getSalt(jsonObject.getString
                ("username"), sourceIp, "LOGIN");
        String hashedPwd = jsonObject.getString("hashedPwd");
        if (pwdSalt != null) {
            String encPwd = secondPwdHash(hashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject auth = sql_accounts.authenticate(jsonObject, encPwd, sourceIp);
            if (auth != null) {
                loggedInUid = auth.getInt("uid");
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

    private JSONObject changePwd(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        String newHashedPwd = jsonObject.getString("newHashedPwd");
        String pwdSalt = sql_accounts.getSalt(jsonObject.getString("username"), sourceIp, "CHANGE_PWD");
        if (pwdSalt != null) {
            String newEncPwd = secondPwdHash(newHashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject verification = sql_accounts.changePassword(jsonObject, newEncPwd, sourceIp);
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

    private JSONObject upload(JSONObject jsonObject, SQL_Files sql_files) throws Exception {
        int fsoid = sql_files.createFso(jsonObject, s.getRemoteSocketAddress().toString());
        if (fsoid != -1) {
            JSONObject response = new JSONObject();
            response.put("msgType","uploadAck");
            response.put("fsoid", fsoid);
            return response;
        }
        System.out.println("Sending error -- unable to create new folder");
        return makeErrJson("Unable to upload");
    }

    private JSONObject download(JSONObject jsonObject, SQL_Files sql_files) throws Exception {
        return sql_files.getFile(jsonObject);
    }

    private JSONObject rename(JSONObject jsonObject, SQL_Files sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        String newName = jsonObject.getString("newName");
        String newFsoNameIV = jsonObject.getString("newFsoNameIV");

        if (sql_files.renameFso(fsoid, uid, newName, newFsoNameIV, sourceIp) ==
                fsoid) {
            JSONObject response = new JSONObject();
            response.put("msgType","renameAck");
            response.put("fsoid", fsoid);
            response.put("uid", uid);
            return response;
        }

        return makeErrJson("Unable to rename");
    }

    private JSONObject overwrite(JSONObject jsonObject, SQL_Files sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        String newFileIV = jsonObject.getString("newFileIV");
        String encFile = jsonObject.getString("encFile");

        if (sql_files.overwrite(fsoid, uid, newFileIV, encFile, sourceIp) ==
                fsoid) {
            JSONObject response = new JSONObject();
            response.put("msgType","overwriteAck");
            response.put("fsoid", fsoid);
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("Unable to overwrite file");
    }

    private JSONObject removePriv(JSONObject jsonObject, SQL_Files sql_files) {
        //TODO

        //copied from FileController
        /*if (priv == PrivType.EDIT) {
            rmUser = sql_connection.removeEditPriv(systemObject.getId(), user.getId(), userId);
        } else {
            rmUser = sql_connection.removeViewPriv(systemObject.getId(), user.getId(), userId);
        }*/
        return null;
    }

    private JSONObject addPriv(JSONObject jsonObject, SQL_Files sql_files) {
        //TODO
        //copied from FileController:
        /*if (priv == PrivType.EDIT) {
            newUser = sql_connection.addEditPriv(json);
        } else {
            newUser = sql_connection.addViewPriv(json);
        }*/
        return null;
    }

    private JSONObject delete(JSONObject jsonObject, SQL_Files sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");

        if (sql_files.deleteFile(fsoid, uid, sourceIp) == fsoid) {
            JSONObject response = new JSONObject();
            response.put("msgType","deleteAck");
            response.put("fsoid", fsoid);
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("Unable to delete file");
    }

    private JSONObject changeEmail(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        int uid = jsonObject.getInt("uid");
        String oldEmail = jsonObject.getString("oldEmail");
        String newEmail = jsonObject.getString("newEmail");

        if (sql_accounts.changeEmail(uid, oldEmail, newEmail, sourceIp)) {
            JSONObject response = new JSONObject();
            response.put("msgType","editEmailAck");
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("Unable to change email");
    }

    private JSONArray getFileLog(JSONObject jsonObject, SQL_Files sql_files) {
        return sql_files.getFileLog(jsonObject);
    }

    private JSONArray getChildren(JSONObject jsonObject, SQL_Files sql_files) {
        JSONArray arr = sql_files.getChildren(jsonObject);
        int uid = jsonObject.getInt("uid");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            int fsoid = obj.getInt("id");
            obj.put("isEditor", sql_files.verifyEditPermission(fsoid,
                    uid));
            obj.put("isViewer", sql_files.verifyBothPermission(fsoid,
                    uid));
        }
        return arr;
    }

    private JSONObject getUsername(JSONObject jsonObject, SQL_Accounts
            sql_accounts) {
        int uid = jsonObject.getInt("uid");
        String username = sql_accounts.getUsername(uid);
        if (username != null) {
            JSONObject response = new JSONObject();
            response.put("msgType", "usernameAck");
            response.put("username", username);
            return response;
        }
        return makeErrJson("Could not find user with that username");
    }

    private JSONObject getUserId(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        String username = jsonObject.getString("username");
        int userId = sql_accounts.getUserId(username);
        if (userId != -1) {
            JSONObject response = new JSONObject();
            response.put("msgType", "useridAck");
            response.put("userid", userId);
            return response;
        }
        return makeErrJson("Could not find user with that userid");
    }

    private JSONObject renameKeys(JSONObject jsonObject, SQL_Files sql_files) {
        JSONObject response = getFileSK(jsonObject, sql_files);
        if (response != null)
            response.put("msgType", "renameKeysAck");
        return response;
    }

    private JSONObject overwriteKeys(JSONObject jsonObject, SQL_Files sql_files) {
        JSONObject response = getFileSK(jsonObject, sql_files);
        if (response != null)
            response.put("msgType", "overwriteKeysAck");
        return response;
    }

    private JSONObject getFileSK(JSONObject jsonObject, SQL_Files sql_files) {
        JSONObject response = new JSONObject();
        String fileSK = sql_files.getFileSK(jsonObject.getInt("fsoid"),
                jsonObject.getInt("uid"), sourceIp);
        if (fileSK != null) {
            response.put("fileSK", fileSK);
            response.put("fsoid", jsonObject.getInt("fsoid"));
            response.put("uid", jsonObject.getInt("uid"));
            return response;
        }
        return makeErrJson("File secret key could not be retrieved");
    }

    private JSONObject logout(JSONObject jsonObject) {
        int uid = jsonObject.getInt("uid");
        if (uid == loggedInUid) {
            loggedInUid = -1;
            JSONObject response = new JSONObject();
            response.put("msgType", "logoutAck");
            response.put("uid", uid);
            return response;
        } else {
            return makeErrJson("User id does not match with logged in user");
        }
    }

    private boolean isLoggedInUser(JSONObject jsonObject) {
        return jsonObject.getInt("uid") == loggedInUid;
    }

    private JSONObject getEditorViewerList(JSONObject jsonObject, SQL_Files sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        if (sql_files.verifyEditPermission(fsoid, uid)) {
            JSONObject response = sql_files.getPermissions(fsoid);
            if (response != null) {
                response.put("msgType", "getEditorViewerListAck");
                return response;
            } else
                return makeErrJson("There is no file/folder corresponding to " +
                        "this file/folder id");
            }
        else
            return makeErrJson("This user does not have permission to see " +
                    "this list");
    }

    private JSONObject deleteUser(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        int uid = jsonObject.getInt("uid");
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        int deletedUid = sql_accounts.deleteUser(uid, username, password,
                s.getRemoteSocketAddress().toString());
        if (deletedUid == uid) {
            JSONObject response = new JSONObject();
            response.put("msgType", "deleteUserAck");
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("The user could not be deleted.");
    }

    private JSONObject makeErrJson(String message) {
        //TODO
        JSONObject response = new JSONObject();
        response.put("msgType","error");
        response.put("message", message);
        return response;
    }
}
