package org.cs5431.controller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cs5431.SSS;
import org.cs5431.UserKeyPacket;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.Folder;
import org.cs5431.model.User;
import org.cs5431.view.PwdRecoveryBundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.*;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.sendJson;

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

    public Socket getSSLSocket() { return this.sslSocket;}
    /**
    * Creates user with the username, password, and email provided.
    * @return user if successful
    */
    public User createUser(String username, String password, String email, String phoneNumber,
                           int twoFa, boolean hasRecovery, List<Integer> nominatedUids, int neededUsers,
                           List<PublicKey> publicKeys)
        throws Exception {

        JSONObject user = new JSONObject();
        user.put("msgType", "registration");
        user.put("username", username);
        user.put("email", email);
        user.put("phoneNo", phoneNumber);
        user.put("has2fa", twoFa);
        user.put("hasPwdRec", hasRecovery);
        if (hasRecovery) {
            user.put("nominatedUids", nominatedUids);
            user.put("neededUsers", neededUsers);
            SSS secretGen = new SSS(nominatedUids.size(), neededUsers,
                    new BigInteger(password.getBytes(StandardCharsets.UTF_8)));
            List<String> encSecrets = encryptSecrets(publicKeys, secretGen.generateSecrets());
            user.put("secrets", encSecrets);
        }

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
                    lastModified, true, new Timestamp(System.currentTimeMillis()));
            return new User(uid, username, email, parentFolder,
                    privKey, pubKey, twoFa, phoneNumber, new Timestamp(System.currentTimeMillis()));
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
     * @return The received JSONObject if successful
     */
    public JSONObject login(String username, String password) throws Exception {
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
                return user;
            } else if (user.getString("msgType").equals("error")) {
                throw new LoginFailException(user.getString
                        ("message"));
            } else {
                throw new LoginFailException("Received bad response " +
                        "from server");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject do2fa(String otp, int uid)
            throws IOException, ClassNotFoundException, LoginFailException {
        JSONObject json = new JSONObject();
        json.put("msgType", "login2fa");
        json.put("uid", uid);
        json.put("otp", otp);

        sendJson(json, sslSocket);

        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("login2faAck")) {
            return response;
        } else if (response.getString("msgType").equals("error")) {
            throw new LoginFailException(response.getString
                    ("message"));
        } else {
            throw new LoginFailException("Received bad response " +
                    "from server");
        }
    }

    public User parseLogin(String username, String password, JSONObject user, int has2fa) {
        try {
            int uid = user.getInt("uid");
            int parentFolderid = user.getInt("parentFolderid");
            String email = user.getString("email");
            String phoneNo = user.getString("phoneNo");
            String encodedPrivKey = user.getString("privKey");
            String privKeySalt = user.getString("privKeySalt");
            PrivateKey privKey = getPrivKeyFromJSON(encodedPrivKey, privKeySalt,
                    password);
            PublicKey pubKey = getPubKeyFromJSON(user.getString("pubKey"));

            Folder parentFolder = getFolderFromId(parentFolderid, uid,
                    privKey);
            Timestamp keyLastUpdated = new Timestamp(user.getLong("timestamp"));
            return new User(uid, username, email, parentFolder,
                    privKey, pubKey, has2fa, phoneNo, keyLastUpdated);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException |
                NoSuchProviderException | InvalidKeySpecException |
                InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Folder getFolderFromId(int folderId, int uid, PrivateKey
            userPrivKey) {
        try {
            Folder parentFolder = new Folder(folderId, "", null, true, null);
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

    public PwdRecoveryBundle getUserForPwdRecovery(String username)  throws IOException,
            ClassNotFoundException, UserRetrieveException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        JSONObject json = new JSONObject();
        json.put("msgType","pwdNominate");
        json.put("username", username);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("pwdNominateAck"))
            return new PwdRecoveryBundle(response.getInt("uid"), username,
                    getPubKeyFromJSON(response.getString("pubKey")));
        else if (response.getString("msgType").equals("error"))
            throw new UserRetrieveException(response.getString("message"));
        else
            throw new UserRetrieveException("Received bad response from " +
                    "server");
    }

    public JSONObject recoverPassword(String username) throws IOException,
            ClassNotFoundException, UserRetrieveException {
        JSONObject json = new JSONObject();
        json.put("msgType","recoverPwd");
        json.put("username", username);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("recoverPwdAck"))
            return response;
        else if (response.getString("msgType").equals("error"))
            throw new UserRetrieveException(response.getString("message"));
        else
            throw new UserRetrieveException("Received bad response from " +
                    "server");

    }

    public void sendRecoveryEmail(int uid, String username) throws IOException, ClassNotFoundException,
            UserRetrieveException {
        JSONObject json = new JSONObject();
        json.put("msgType","recoverPwdEmail");
        json.put("uid", uid);
        json.put("username", username);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("recoverPwdEmailAck")) {
            if (response.getInt("uid") != uid) {
                throw new UserRetrieveException("Could not send out email!");
            }
        } else if (response.getString("msgType").equals("error"))
            throw new UserRetrieveException(response.getString("message"));
        else
            throw new UserRetrieveException("Received bad response from " +
                    "server");
    }

    public User updateUserKeys(User user, String password) throws IOException, ClassNotFoundException,
            LoginFailException, NoSuchAlgorithmException, NoSuchProviderException,
    NoSuchPaddingException, InvalidKeyException,
    InvalidAlgorithmParameterException, IllegalBlockSizeException,
    BadPaddingException {
        UserKeyPacket packet = generateKeyPacket(password);
        JSONObject getKeys = new JSONObject();
        getKeys.put("msgType", "updateUserKey");
        getKeys.put("uid", user.getId());
        sendJson(getKeys, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("updateUserKeyAck")) {
            if (response.getInt("uid") == user.getId()) {
                List<Integer> fsoid = new ArrayList<>();
                JSONArray fsoArray = response.getJSONArray("fsoids");
                List<String> newFileKeys = new ArrayList<>();
                JSONArray encArray = response.getJSONArray("encFileKeys");
                for (int i = 0; i < fsoArray.length(); i++) {
                    fsoid.add(fsoArray.getInt(i));
                    String oldFileKeyStr = encArray.getString(i);
                    SecretKey fileSK = decFileSecretKey(
                            Base64.getDecoder().decode(oldFileKeyStr), user.getPrivKey());
                    String newFileSK = Base64.getEncoder().encodeToString(
                            encFileSecretKey(fileSK, packet.keyPair.getPublic())
                    );
                    newFileKeys.add(newFileSK);
                }
                List<Integer> groupUid = new ArrayList<>();
                JSONArray groupArray = response.getJSONArray("groupUid");
                List<PublicKey> publicKeys = new ArrayList<>();
                List<String> plainSecrets = new ArrayList<>();
                JSONArray secretArray = response.getJSONArray("secrets");
                for (int i = 0; i < groupArray.length(); i++) {
                    groupUid.add(groupArray.getInt(i));
                    String currSecret = secretArray.getString(i);
                    plainSecrets.add(decryptSecret(user.getPrivKey(), currSecret));
                    publicKeys.add(packet.keyPair.getPublic());
                }
                List<String> newSecrets = encryptSecrets(publicKeys, plainSecrets);

                JSONObject update = new JSONObject();
                update.put("msgType", "updateUserKeyFile");
                update.put("uid", user.getId());
                update.put("privKey", packet.encPrivKey);
                update.put("privKeySalt", Base64.getEncoder().encodeToString(packet.salt));
                update.put("pubKey", Base64.getEncoder().encodeToString(packet.keyPair.getPublic().getEncoded()));
                update.put("fsoid", fsoid);
                update.put("encFileKeys", newFileKeys);
                update.put("groupUid", groupUid);
                update.put("secrets", newSecrets);

                sendJson(update, sslSocket);

                JSONObject upRes = receiveJson(sslSocket);
                if (response.getString("msgType").equals("updateUserKeyAck")) {
                    if (response.getInt("uid") == user.getId()) {
                        return new User(user.getId(), user.getUsername(), user.getEmail(),
                                user.getUserParentFolder(), packet.keyPair.getPrivate(),
                                packet.keyPair.getPublic(), user.getHas2fa(),
                                user.getPhoneNo(), new Timestamp(System.currentTimeMillis()));
                    } else {
                        throw new LoginFailException("Could not update user keys");
                    }
                } else if (upRes.getString("msgType").equals("error"))
                    throw new LoginFailException(response.getString("message"));
                else
                    throw new LoginFailException("Received bad response from " +
                            "server");
            } else {
                throw new LoginFailException("Could not update user keys");
            }
        } else if (response.getString("msgType").equals("error"))
            throw new LoginFailException(response.getString("message"));
        else
            throw new LoginFailException("Received bad response from " +
                    "server");
    }

    public static class RegistrationFailException extends Exception {
        RegistrationFailException(String message) {
            super(message);
        }
    }

    public static class LoginFailException extends Exception {
        LoginFailException(String message) {
            super(message);
        }
    }

    public static class UserRetrieveException extends Exception {
        UserRetrieveException(String message) {
            super(message);
        }
    }
}
