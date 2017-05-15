package org.cs5431;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.Socket;
import java.net.SocketException;
import java.util.*;

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
    private Email email;
    private long otpGenTime;
    private String otp;
    private TwoFactorAuth twoFactorAuth;

    SSLServer(Socket socket, SQL_Accounts sql_accounts, SQL_Files sql_files, Email email){
        this.s = socket;
        this.sql_accounts = sql_accounts;
        this.sql_files = sql_files;
        this.sourceIp = s.getRemoteSocketAddress().toString();
        this.email = email;
        this.twoFactorAuth = new TwoFactorAuth(email);
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
                        type.equals("renameKeys") || type.equals("addEditor")
                        || type.equals("addViewer") || type.equals("addViewerKeys") ||
                        type.equals("removePriv") || type.equals
                        ("deleteForAll") || type.equals("deleteForUser") ||
                        type.equals("overwrite") || type.equals
                        ("overwriteKeys") || type.equals("editEmail") || type
                        .equals("getFileLogs") || type.equals("getChildren")
                        || type.equals("logout") || type.equals
                        ("getEditorViewerList") || type.equals("deleteUser")
                        || type.equals("uploadKeys") || type.equals("2faToggle") ||
                        type.equals("setPwdGroup") || type.equals("pwdRecoveryInfo") ||
                        type.equals("changePhoneNo")) {
                    if (!isLoggedInUser(jsonObject)) {
                        check = false;
                        sql_accounts.attemptedUidFailLog(jsonObject.getInt("uid"), loggedInUid, sourceIp);
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
                    case "login2fa":
                        response = login2fa(jsonObject, sql_accounts);
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
                    case "uploadKeys":
                        response = uploadKeys(jsonObject, sql_files);
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
                    case "addEditor":
                        response = addEditor(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "addViewer":
                        response = addViewer(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "addViewerKeys":
                        response = addViewerKeys(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "demoteEditor":
                        response = demoteEditor(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "removePriv":
                        response = removePriv(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "deleteForAll":
                        response = deleteForAll(jsonObject, sql_files);
                        sendJson(response, s);
                        break;
                    case "deleteForUser":
                        response = deleteForUser(jsonObject, sql_files);
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
                    case "2faToggle":
                        response = twoFactorToggle(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "pwdNominate":
                        response = findUserPwdRec(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "setPwdGroup":
                        response = setPwdRecovery(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "pwdRecoveryInfo":
                        response = getPwdRecovery(jsonObject, sql_accounts);
                        sendJson(response, s);
                        break;
                    case "recoverPwd":
                        response = recoverPwd(jsonObject, sql_accounts, email);
                        sendJson(response, s);
                        break;
                    case "changePhoneNo":
                        response = changePhoneNo(jsonObject, sql_accounts);
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
        if (loggedInUid != -1) {
            return makeErrJson("There is another logged in user");
        }
        //rate limiting: check if too many failed logins within this one minute
        Date now = new Date();
        if (failedLogins >= MAX_LOGINS_PER_MINUTE && withinOneMinute(now,
                failedTime)) {
            return (sql_accounts.logSessionLimit(sourceIp)) ?
                    makeErrJson("Too many failed logins recently. Please try again in 5 minutes."): null;
        }

        String pwdSalt = sql_accounts.getSalt(jsonObject.getString
                ("username"), sourceIp, "LOGIN");
        String hashedPwd = jsonObject.getString("hashedPwd");
        if (pwdSalt != null) {
            String encPwd = secondPwdHash(hashedPwd, Base64.getDecoder().decode(pwdSalt));
            JSONObject auth = sql_accounts.authenticate(jsonObject, encPwd, sourceIp, "LOGIN", email);
            if (auth != null) {
                int has2fa = auth.getInt("has2fa");
                JSONObject jsonFor2fa = new JSONObject();
                jsonFor2fa.put("msgType", "loginAck");
                jsonFor2fa.put("uid", auth.getInt("uid"));
                jsonFor2fa.put("has2fa", has2fa);
                switch (has2fa) {
                    case 0: loggedInUid = auth.getInt("uid");
                        return auth;
                    case 1: otp = twoFactorAuth.generateAndSend2fa(auth.getString("email"));
                        otpGenTime = System.nanoTime();
                        return jsonFor2fa;
                    case 2: otp = twoFactorAuth.generateAndSend3fa(auth.getString("phoneNo"));
                        otpGenTime = System.nanoTime();
                        return jsonFor2fa;
                }
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

    private JSONObject login2fa(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        int uid = jsonObject.getInt("uid");
        if (twoFactorAuth.checkOtpValid(otp, jsonObject.getString("otp"), otpGenTime)) {
            JSONObject user = sql_accounts.twoFactorLogin(uid, sourceIp);
            if (user != null) {
                loggedInUid = uid;
                return user;
            }
            return makeErrJson("An error occurred. Please try logging in again.");
        }
        if (sql_accounts.create2faFailureLog(uid, sourceIp)) {
            return makeErrJson("Invalid OTP.");
        }
        return makeErrJson("An error occurred. Please try logging in again.");    }

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
        return makeErrJson("Change password failed");
    }

    private JSONObject uploadKeys(JSONObject jsonObject, SQL_Files sql_files) throws Exception {
        JSONObject ack = sql_files.uploadKeys(jsonObject, sourceIp);
        if (ack != null)
            return ack;
        return makeErrJson("Unable to retrieve keys to create this object with all privileges");
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
        return sql_files.getFile(jsonObject, sourceIp);
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

    private JSONObject demoteEditor(JSONObject jsonObject, SQL_Files
            sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        int demoteUid = jsonObject.getInt("demoteUid");
        HashMap<Integer, Integer> allChildren = getAllChildren(fsoid, -1, uid, sql_files);
        for (Integer childid : allChildren.keySet()) {
            int newUidRes = sql_files.addViewPriv(uid, childid, fsoid, demoteUid,
                    "", sourceIp);
            if (newUidRes == -1)
                return makeErrJson("Failed to demote this user - double check" +
                        " user id");
            else if (newUidRes != demoteUid)
                return makeErrJson("Could not demote user - demoted the wrong" +
                        " user with id " + newUidRes);
        }
        JSONObject response = new JSONObject();
        response.put("msgType", "demoteEditorAck");
        response.put("newUid", demoteUid);
        return response;
    }

    private JSONObject removePriv(JSONObject jsonObject, SQL_Files
            sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        int removeUid = jsonObject.getInt("removeUid");
        HashMap<Integer, Integer> allChildren = getAllChildren(fsoid, -1, uid, sql_files);
        for (Integer childid : allChildren.keySet()) {
            int removed;
            if (jsonObject.getString("userType").equals("editor")) {
                removed = sql_files.removeEditPriv(childid, uid, removeUid, sourceIp);
            }
            else {
                removed = sql_files.removeViewPriv(childid, uid, removeUid, sourceIp);
            }
            if (removed == -1)
                return makeErrJson("Failed to remove this user - double check" +
                        " user id");
            else if (removed != removeUid)
                return makeErrJson("Could not add editor - remove the wrong " +
                        "user with id " + removed);
        }

        JSONObject response = new JSONObject();
        response.put("msgType","removePrivAck");
        response.put("removeUid", removeUid);
        return response;
    }

    private JSONObject addEditor(JSONObject jsonObject, SQL_Files sql_files) {
        jsonObject.put("encSecretKey","");
        int uid = jsonObject.getInt("uid");
        int fsoid = jsonObject.getInt("fsoid");
        int newUid = jsonObject.getInt("newUid");

        HashMap<Integer, Integer> allChildren = getAllChildren(fsoid, -1, uid, sql_files);
        for (Integer childid : allChildren.keySet()) {
            int newUidRes = sql_files.addEditPriv(uid, childid, newUid, sourceIp);
            sql_files.removeDuplicates(newUid);
            if (newUidRes == -1)
                return makeErrJson("Failed to add this new editor - double check " +
                        "user id");
            else if (newUidRes != newUid)
                return makeErrJson("Could not add editor - added the wrong user with " +
                    "id " + newUidRes);
        }
        JSONObject response = new JSONObject();
        response.put("msgType", "addEditorAck");
        response.put("newUid", newUid);
        return response;
    }

    private JSONObject addViewerKeys(JSONObject jsonObject, SQL_Files
            sql_files){
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        int newUid = jsonObject.getInt("newUid");

        HashMap<Integer, Integer> allChildren = getAllChildren(fsoid, -1, uid, sql_files);
        List<String> keys = new ArrayList<>();
        for (Integer childid : allChildren.keySet()) {
            String encFileSK = sql_files.getEncFileSK(childid, uid, sourceIp);
            if (encFileSK == null)
                return makeErrJson("Could not retrieve keys - check if you have " +
                        "the permission to add privileges to this file");
            keys.add(encFileSK);
        }

        String pubKey = sql_files.getPubKey(newUid);

        JSONObject response = new JSONObject();
        response.put("msgType", "addViewerKeysAck");
        response.put("secretKey", keys);
        response.put("fsoid", new JSONObject(allChildren));
        response.put("pubKey", pubKey);
        return response;
    }

    /**
     * Finds all children recursively, child:parent
     */
    private HashMap<Integer,Integer> getAllChildren(int fsoid, int parentid, int uid, SQL_Files
                                         sql_files) {
        HashMap<Integer,Integer> list = new HashMap<>();
        list.put(fsoid, parentid);
        if (sql_files.isFolder(fsoid, uid, sourceIp)) {
            List<Integer> children = sql_files.getChildrenId(fsoid, uid,
                    sourceIp);
            for (Integer child : children) {
                list.putAll(getAllChildren(child, fsoid, uid, sql_files));
            }
        }
        return list;
    }

    private JSONObject addViewer(JSONObject jsonObject, SQL_Files sql_files) {
        int uid = jsonObject.getInt("uid");
        int newUid = jsonObject.getInt("newUid");
        JSONArray encKeyArr = jsonObject.getJSONArray("encSecretKey");
        JSONArray fsoIdArr = jsonObject.getJSONArray("fsoid");
        JSONArray parentIdArr = jsonObject.getJSONArray("parentid");

        int sumUid = 0;
        for (int i = 0; i < encKeyArr.length(); i++) {
            int fsoid = fsoIdArr.getInt(i);
            int parentid = parentIdArr.getInt(i);
            String encKey = encKeyArr.getString(i);
            int newUidRes = sql_files.addViewPriv(uid, fsoid, parentid, newUid,
                    encKey, sourceIp);
            if (newUidRes != newUid && newUidRes != -1)
                return makeErrJson("Could not add viewer - added the wrong user with " +
                    "id " + newUidRes);
        }
        if (sumUid == -1* encKeyArr.length())
            return makeErrJson("Failed to add this new viewer - double check " +
                    "user id");
        JSONObject response = new JSONObject();
        response.put("msgType", "addViewerAck");
        response.put("newUid", newUid);
        return response;
    }

    private JSONObject deleteForAll(JSONObject jsonObject, SQL_Files
            sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");

        HashMap<Integer, Integer> allChildren = getAllChildren(fsoid, -1, uid, sql_files);
        for (Integer childid : allChildren.keySet()) {
            int deletedId = sql_files.deleteForAll(childid, uid, sourceIp);
            if (deletedId == -1)
                return makeErrJson("Could not delete file with id " + childid);
            else if (deletedId != childid)
                return makeErrJson("Deleted wrong file with id " + deletedId);
            sql_files.deleteIfOrphanFile(childid, uid, sourceIp);
        }
        JSONObject response = new JSONObject();
        response.put("msgType","deleteForAllAck");
        response.put("fsoid", fsoid);
        response.put("uid", uid);
        return response;
    }

    private JSONObject deleteForUser(JSONObject jsonObject, SQL_Files sql_files) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");

        if (sql_files.deleteForUser(fsoid, uid, sourceIp) == fsoid) {
            sql_files.deleteIfOrphanFile(fsoid, uid, sourceIp);
            JSONObject response = new JSONObject();
            response.put("msgType","deleteForUserAck");
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
        return sql_files.getFileLog(jsonObject, sourceIp);
    }

    private JSONArray getChildren(JSONObject jsonObject, SQL_Files sql_files) {
        JSONArray arr = sql_files.getChildren(jsonObject, sourceIp);
        if (arr == null) {
            return new JSONArray(); //TODO handle this better: currently just
            // preventing server stalling
        }
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
            response.put("uid", userId);
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
                sourceIp);
        if (deletedUid == uid) {
            JSONObject response = new JSONObject();
            response.put("msgType", "deleteUserAck");
            response.put("uid", uid);
            loggedInUid = -1;
            return response;
        }
        return makeErrJson("The user could not be deleted.");
    }

    private JSONObject twoFactorToggle(JSONObject json, SQL_Accounts sql_accounts) {
        int uid = json.getInt("uid");
        int newToggle = json.getInt("newToggle");
        boolean isToggled = sql_accounts.toggle2fa(uid, newToggle, sourceIp);
        if (isToggled) {
            JSONObject response = new JSONObject();
            response.put("msgType", "2faToggleAck");
            response.put("uid", uid);
            return response;
        }
        return makeErrJson("The user's 2FA status could not be changed.");
    }

    private JSONObject findUserPwdRec(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        String username = jsonObject.getString("username");
        JSONObject response = sql_accounts.userEmailExists(username);
        if (response != null) {
            return response;
        }
        return makeErrJson("The user does not support 2FA.");
    }

    private JSONObject setPwdRecovery(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        boolean hasRec = jsonObject.getBoolean("hasPwdRec");
        int uid = jsonObject.getInt("uid");
        boolean removedOldSecrets = sql_accounts.removeSecrets(uid, sourceIp);
        if (removedOldSecrets) {
            if (hasRec) {
                //TODO: generate secrets and put into json
                boolean setGroup = sql_accounts.createRecoveryGroup(jsonObject, sourceIp);
                if (!setGroup) {
                    return makeErrJson("Failed to create recovery group.");
                }
            }
            JSONObject response = new JSONObject();
            response.put("msgType", "setPwdGroupAck");
            response.put("uid", jsonObject.getInt("uid"));
            response.put("hasPwdRec", hasRec);
            return response;
        }
        return makeErrJson("Failed to create recovery group.");
    }

    private JSONObject getPwdRecovery(JSONObject jsonObject, SQL_Accounts sql_accounts) {
        int uid = jsonObject.getInt("uid");
        JSONObject response = sql_accounts.getPasRecInfo(uid);
        if (response != null) {
            return response;
        }
        return makeErrJson("Error occurred while fetching the information. Please try again.");
    }

    private JSONObject recoverPwd(JSONObject jsonObject, SQL_Accounts sql_accounts, Email adminEmail) {
        String username = jsonObject.getString("username");
        int uid = sql_accounts.getUserId(username);
        if (uid == -1) {
            return makeErrJson("The username does not exist.");
        }
        JSONObject json = sql_accounts.getSecrets(uid);
        if (json != null) {
            JSONArray groupUid = json.getJSONArray("groupUid");
            JSONArray secrets = json.getJSONArray("secrets");
            JSONArray emails = json.getJSONArray("emails");

            for (int i=0; i<groupUid.length(); i++) {
                adminEmail.send(emails.getString(i),"Password Recovery", "The user " + username + " has " +
                        "requested to recover the password to his/her Pretty Secure File Sharing account. Please inform " +
                        username + " of the following secret: " + secrets.getString(i));
            }
            JSONObject response = sql_accounts.recoverPwd(uid, sourceIp);
            if (response != null) {
                return response;
            }
        }
        return makeErrJson("Unable to recover password. Please try again.");
    }

    private JSONObject changePhoneNo(JSONObject json, SQL_Accounts sql_accounts) {
        if (sql_accounts.changePhoneNo(json, sourceIp) != -1) {
            JSONObject response = new JSONObject();
            response.put("msgType","changePhoneNoAck");
            response.put("uid", json.getInt("uid"));
            return response;
        }
        return makeErrJson("Unable to change phone number.");
    }

    private JSONObject makeErrJson(String message) {
        JSONObject response = new JSONObject();
        response.put("msgType","error");
        response.put("message", message);
        return response;
    }
}
