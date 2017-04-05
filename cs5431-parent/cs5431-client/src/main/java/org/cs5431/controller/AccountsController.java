package org.cs5431.controller;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cs5431.model.*;
import org.cs5431.model.File;
import org.cs5431.SQL_Connection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.SHA256;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.receiveJsonArray;
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
        user.put("hashedPwd", Base64.getEncoder().encodeToString(
                SHA256(password)));
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new
                BouncyCastleProvider());
        kpg.initialize(4096, new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        user.put("pubKey", Base64.getEncoder().encodeToString
                (keyPair.getPublic().getEncoded()));
        //encrypt secret key using password based key
        //symmetric, uses AES
        byte keyAndSalt[][] = pwdBasedKey(password);
        byte key[] = keyAndSalt[0]; //TODO check if this key is the right length
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte encryptedKey[] = cipher.doFinal(keyPair.getPrivate().getEncoded());
        user.put("privKey", Base64.getEncoder().encodeToString(encryptedKey));
        user.put("privKeySalt", Base64.getEncoder().encodeToString
                (keyAndSalt[1]));

        sendJson(user,sslSocket);
        JSONObject newUser = receiveJson(sslSocket);
        if (newUser.getString("msgType").equals("registrationAck")) {
            int uid = newUser.getInt("uid");
            int parentFolderid = newUser.getInt("parentFolderid");

            PrivateKey privKey = getPrivKeyFromJSON(user, password);
            PublicKey pubKey = getPubKeyFromJSON(user);

            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            Folder parentFolder = new Folder(parentFolderid, username, null, lastModified);
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

    private byte[][] pwdBasedKey(String pwd) {
        Random random = new SecureRandom();
        //TODO: 32 is currently the salt length. Is this correct?
        byte salt[] = new byte[32];
        random.nextBytes(salt);
        byte[] hashedPW = hash(pwd, salt);
        if (DEBUG_MODE) {
            System.out.println("On Client side: key generated from pwd and salt:");
            System.out.println(Base64.getEncoder().encodeToString(hashedPW));
        }
        byte returnedValues[][] = new byte[2][128];
        returnedValues[0] = hashedPW;
        returnedValues[1] = salt;
        return returnedValues;
    }

    private byte[] hash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 3000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters
                (128);
        return kp.getKey();
    }

    private PublicKey getPubKeyFromJSON(JSONObject json) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        String encodedPubKey = json.getString("pubKey");
        byte[] decodedPub = Base64.getDecoder().decode(encodedPubKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new X509EncodedKeySpec(decodedPub));
    }

    private PrivateKey getPrivKeyFromJSON(JSONObject json, String password)
    throws NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeySpecException, InvalidKeyException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        String encodedPrivKey = json.getString("privKey");
        String privKeySalt = json.getString("privKeySalt");
        byte[] decodedPriv = decryptPwdBasedKey(encodedPrivKey,
                password, privKeySalt);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(decodedPriv));
    }

    private byte[] decryptPwdBasedKey(String enc, String pwd, String salt)
    throws NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        byte key[] = hash(pwd, Base64.getDecoder().decode(salt));
        if (DEBUG_MODE) {
            System.out.println("From server: key generated from pwd and salt:");
            System.out.println(Base64.getEncoder().encodeToString(key));
        }
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return cipher.doFinal(Base64.getDecoder().decode(enc));
    }

    /**
     * Deletes user with the userId and logs it.
     * @param userId User to be deleted
     * @return true if successful, returns false otherwise.
     */
    public boolean deleteUser(int userId) {
        //TODO
        return false;
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
                //TODO get username?
                int parentFolderid = user.getInt("parentFolderid");
                String email = user.getString("email");

                PrivateKey privKey = getPrivKeyFromJSON(user, password);
                PublicKey pubKey = getPubKeyFromJSON(user);

                Folder parentFolder = getFolderFromId(parentFolderid, uid);
                User currUser = new User(uid, username, email, parentFolder,
                        privKey, pubKey);
                return currUser;
            } else if (user.getString("msgType").equals("error")) {
                throw new LoginFailException(user.getString
                        ("message"));
            } else {
                throw new LoginFailException("Received bad response " +
                        "from server");
            }
            //TODO: create relevant controllers? and pass them? ???
        } catch (JSONException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException |
                NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Folder getFolderFromId(int folderId, int uid) {
        try {
        //TODO: send to server and get the corresponding folder
        Folder parentFolder = new Folder(folderId, "", null,null);
        JSONObject json = new JSONObject();
        json.put("fsoid", folderId);
        json.put("uid", uid);
        json.put("msgType", "getChildren");
        sendJson(json, sslSocket);
        JSONArray children = receiveJsonArray(sslSocket);
        for (int i=0; i<children.length(); i++) {
            JSONObject c = children.getJSONObject(i);
            int id = c.getInt("id");
            String name = c.getString("name");
            String size = c.getString("size");
            long longSize = Long.parseLong(size);
            Timestamp lastModified = Timestamp.valueOf(c.getString("lastModified"));
            String type = c.getString("FSOType");
            if (type.equals("FOLDER")) {
                Folder childFolder = new Folder(id, name, parentFolder, lastModified);

                parentFolder.addChild(childFolder);
            } else {
                File childFile = new File(id, name, parentFolder, longSize,
                        lastModified);
                parentFolder.addChild(childFile);
            }
        }
            return parentFolder;
        }    catch (Exception e) {
                e.printStackTrace();
        }
        return null;
    }

    public class RegistrationFailException extends Exception {
        public RegistrationFailException (String message) {
            super(message);
        }
    }

    public class LoginFailException extends Exception {
        public LoginFailException (String message) {
            super(message);
        }
    }

}
