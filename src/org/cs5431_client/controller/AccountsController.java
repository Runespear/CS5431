package org.cs5431_client.controller;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cs5431_client.model.*;
import org.cs5431_client.util.SQL_Connection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

/**
 * A controller for all accounts.
 * The controller for an individual account is called UserController.
 */
public class AccountsController {

    private SQL_Connection sql_connection;

    public AccountsController(){
        this.sql_connection = new SQL_Connection("localhost", 3306);
    }
    /**
    * Creates user with the username, password, and email provided.
    * @return user if successful
    * @throws RegistrationFailException if unsuccessful
    */
    public User createUser(String username, String password, String email,
                           String ip, String port)
            throws RegistrationFailException {
        //TODO: send to server new account info and create user with the right info

        try {
            JSONObject user = new JSONObject();
            user.put("username", username);
            user.put("email", email);

            //hashing password
            SHA256Digest sha256 = new SHA256Digest();
            byte pwdByte[] = password.getBytes();
            sha256.update(pwdByte, 0, pwdByte.length);
            byte[] hashedPwd = new byte[sha256.getDigestSize()];
            sha256.doFinal(hashedPwd, 0);
            user.put("hashedPwd", Base64.getEncoder().encodeToString(hashedPwd));
            Arrays.fill( pwdByte, (byte)0 );    //an attempt to zero out pwd

            //TODO: priv key must be generated on client side and
            // encrypted using pwd-based encryption
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new
                    BouncyCastleProvider());
            kpg.initialize(4096, new SecureRandom());
            KeyPair keyPair = kpg.generateKeyPair();
            user.put("pubKey", Base64.getEncoder().encodeToString(keyPair
                    .getPublic().getEncoded()));

            byte keyAndSalt[][] = pwdBasedKey(password);
            byte key[] = keyAndSalt[0]; //TODO check if this key is the right length
            SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            //TODO should we create iv?
            byte encryptedKey[] = cipher.doFinal(key);

            user.put("privKey", Base64.getEncoder().encodeToString(encryptedKey));
            user.put("privKeySalt",Base64.getEncoder().encodeToString
                    (keyAndSalt[1]));

            JSONObject newUser = sendUser(user);    //TODO change to sendObject?
            int uid = newUser.getInt("uid");
            int parentFolderid = newUser.getInt("parentFolderid");

            //TODO: uncomment when privKey and pubKeys are implemented
        /*String encodedPrivKey = user.getString("privKey");
        byte[] decodedPriv = Base64.getDecoder().decode(encodedPrivKey);
        SecretKey privKey = new SecretKeySpec(decodedPriv, 0, decodedPriv.length, "RSA");

        String encodedPubKey = user.getString("privKey");
        byte[] decodedPub = Base64.getDecoder().decode(encodedPubKey);
        SecretKey pubKey = new SecretKeySpec(decodedPub, 0, decodedPub.length, "RSA");*/

            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            Folder parentFolder = new Folder(parentFolderid, username, null, lastModified);
            return new User(uid, username, email, parentFolder, null, null);
        } catch (JSONException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[][] pwdBasedKey(String pwd) {
        Random random = new SecureRandom();
        //TODO: 32 is currently the salt length. Is this correct?
        byte salt[] = new byte[32];
        random.nextBytes(salt);
        byte[] hashedPW = hash(pwd, salt);
        byte returnedValues[][] = new byte[2][128];
        returnedValues[0] = hashedPW;
        returnedValues[1] = salt;
        return returnedValues;
    }

    private static byte[] hash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 3000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters
                (128);
        return kp.getKey();
    }

    private JSONObject sendUser(JSONObject user) {
        //TODO: send to server
        //TODO change following line
        JSONObject newUser = sql_connection.createUser(user, "", "");
        return newUser;
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
     * @param serverIP IP of server to be connected to
     * @param serverPort Port of server to be connected to
     * @return userId if successful
     */
    public User login(String username, String password, String serverIP,
                         String serverPort) {
        //TODO: establish connection

        //TODO: attempt to connect with given credentials
        JSONObject allegedUser = new JSONObject();
        try {
            allegedUser.put("username", username);
            allegedUser.put("pwd", password);
            //TODO: send allegeduser to server and check
            JSONObject user = sql_connection.authenticate(allegedUser);

            if (user != null) {
                int uid = user.getInt("uid");
                int parentFolderid = user.getInt("parentFolderid");
                String email = user.getString("email");

                //TODO: uncomment when keys are up
                /*String encodedPrivKey = user.getString("privKey");
                byte[] decodedPriv = Base64.getDecoder().decode(encodedPrivKey);
                SecretKey privKey = new SecretKeySpec(decodedPriv, 0, decodedPriv.length, "RSA");

                String encodedPubKey = user.getString("privKey");
                byte[] decodedPub = Base64.getDecoder().decode(encodedPubKey);
                SecretKey pubKey = new SecretKeySpec(decodedPub, 0, decodedPub.length, "RSA");*/

                Folder parentFolder = getFolderFromId(parentFolderid, uid);
                User currUser = new User(uid, username, email, parentFolder, null,null);
                return currUser;
            }
            //TODO: create relevant controllers? and pass them? ???
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Folder getFolderFromId(int folderId, int uid) {
        //TODO: send to server and get the corresponding folder
        Folder parentFolder = new Folder(folderId, "", null,null);
        ArrayList<JSONObject> children = sql_connection.getChildren(folderId, uid);
        for (JSONObject c : children) {
            try {
                int id = c.getInt("id");
                String name = c.getString("name");
                String size = c.getString("size");
                long longSize = Long.valueOf(size);
                Timestamp lastModified = (Timestamp) c.get("lastModified");
                String type = c.getString("FSOType");
                if (type == "FOLDER") {
                    Folder childFolder = new Folder(id, name, parentFolder, lastModified);
                    parentFolder.addChild(childFolder);
                } else {
                    File childFile = new File(id, name, parentFolder, longSize, lastModified);
                    parentFolder.addChild(childFile);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return parentFolder;
    }

    public class RegistrationFailException extends Exception {
        public RegistrationFailException (String message) {
            super(message);
        }
    }

}
