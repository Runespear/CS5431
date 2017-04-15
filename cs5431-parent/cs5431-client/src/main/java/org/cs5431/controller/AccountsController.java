package org.cs5431.controller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cs5431.model.File;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.Folder;
import org.cs5431.model.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.*;
import static org.cs5431.JSON.*;

/**
 * A controller for all accounts.
 * The controller for an individual account is called UserController.
 */
public class AccountsController {
    private Socket sslSocket;

    public AccountsController(){
        Security.addProvider(new BouncyCastleProvider());
    }

    public void setSocket(Socket s) {
        this.sslSocket = s;
    }
    /**
    * Creates user with the username, password, and email provided.
    * @return user if successful
    */
    public User createUser(String username, String password, String email)
        throws Exception {

        JSONObject user = new JSONObject();
        user.put("msgType", "registration");
        user.put("username", username);
        user.put("email", email);

        //hashing password
        user.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(password)));
        String keys[] = generateUserKeys(password);
        user.put("pubKey", keys[0]);
        user.put("privKey", keys[1]);
        user.put("privKeySalt", keys[2]);

        sendJson(user,sslSocket);
        JSONObject newUser = receiveJson(sslSocket);
        if (newUser.getString("msgType").equals("registrationAck")) {
            int uid = newUser.getInt("uid");
            int parentFolderid = newUser.getInt("parentFolderid");

            String encodedPrivKey = user.getString("privKey");
            String privKeySalt = user.getString("privKeySalt");
            PrivateKey privKey = getPrivKeyFromJSON(encodedPrivKey,
                    privKeySalt, password);
            PublicKey pubKey = getPubKeyFromJSON(user.getString("pubKey"));

            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            Folder parentFolder = new Folder(parentFolderid, username,
                    lastModified, true, true);
            return new User(uid, username, email, parentFolder,
                    privKey, pubKey);
        } else if (newUser.getString("msgType").equals("error")) {
            throw new RegistrationFailException(newUser.getString
                    ("message"));
        } else {
            throw new RegistrationFailException("Received bad response " +
                    "from server");
        }
    }

    /**
     * Creates a connection with serverIP and logs in with the username and password.
     * @param username Username to be used for this server
     * @param password Password associated with username
     * @return userId if successful
     */
    public User login(String username, String password) throws Exception {
        JSONObject allegedUser = new JSONObject();
        try {
            allegedUser.put("msgType", "login");
            allegedUser.put("username", username);
            allegedUser.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(password)));

            sendJson(allegedUser, sslSocket);
            if (DEBUG_MODE) {
                System.out.println("waiting to receive json...");
            }
            JSONObject user = receiveJson(sslSocket);

            if (user.getString("msgType").equals("loginAck")) {
                int uid = user.getInt("uid");
                int parentFolderid = user.getInt("parentFolderid");
                String email = user.getString("email");

                String encodedPrivKey = user.getString("privKey");
                String privKeySalt = user.getString("privKeySalt");
                PrivateKey privKey = getPrivKeyFromJSON(encodedPrivKey, privKeySalt,
                        password);
                PublicKey pubKey = getPubKeyFromJSON(user.getString("pubKey"));

                Folder parentFolder = getFolderFromId(parentFolderid, uid,
                        privKey);
                return new User(uid, username, email, parentFolder,
                        privKey, pubKey);
            } else if (user.getString("msgType").equals("error")) {
                throw new LoginFailException(user.getString
                        ("message"));
            } else {
                throw new LoginFailException("Received bad response " +
                        "from server");
            }
        } catch (JSONException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException |
                NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Folder getFolderFromId(int folderId, int uid, PrivateKey
            userPrivKey) {
        try {
            Folder parentFolder = new Folder(folderId, "", null, true, true);
            List<FileSystemObject> contents = FileController.getChildrenWithId
                    (folderId, uid, sslSocket, userPrivKey);
            for (FileSystemObject child : contents)
                parentFolder.addChild(child);
            return parentFolder;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUsername(int uid) throws IOException,
            ClassNotFoundException, UserRetrieveException {
        JSONObject json = new JSONObject();
        json.put("msgType","username");
        json.put("uid", uid);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("usernameAck"))
            return response.getString("username");
        else if (response.getString("msgType").equals("error"))
            throw new UserRetrieveException(response.getString("message"));
        else
            throw new UserRetrieveException("Received bad response from " +
                    "server");
    }

    public int getUserId(String username) throws IOException,
            ClassNotFoundException, UserRetrieveException {
        JSONObject json = new JSONObject();
        json.put("msgType","userid");
        json.put("username", username);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("useridAck"))
            return response.getInt("uid");
        else if (response.getString("msgType").equals("error"))
            throw new UserRetrieveException(response.getString("message"));
        else
            throw new UserRetrieveException("Received bad response from " +
                    "server");
    }

    public class RegistrationFailException extends Exception {
        RegistrationFailException(String message) {
            super(message);
        }
    }

    public class LoginFailException extends Exception {
        LoginFailException(String message) {
            super(message);
        }
    }

    public class UserRetrieveException extends Exception {
        UserRetrieveException(String message) {
            super(message);
        }
    }
}
