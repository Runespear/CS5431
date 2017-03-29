package org.cs5431_client.controller;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import jdk.nashorn.internal.parser.JSONParser;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cs5431_client.model.*;
import org.cs5431_client.model.File;
import org.cs5431_client.util.SQL_Connection;
import org.cs5431_server.fileserver.TransmittedFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
    private Socket sslSocket;

    public AccountsController(){
        Security.addProvider(new BouncyCastleProvider());
        this.sql_connection = new SQL_Connection("localhost", 3306);
    }

    public void setSocket(Socket s) {
        this.sslSocket = s;
    }
    /**
    * Creates user with the username, password, and email provided.
    * @return user if successful
    * @throws RegistrationFailException if unsuccessful
    */
    public User createUser(String username, String password, String email)
            throws RegistrationFailException {
        Task<User> task = new Task<User>() {
            @Override
            protected User call() throws Exception {
                try {
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

                    sendJson(user);
                    JSONObject newUser = receiveJson();
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        final User[] ret = new User[1];
        task.setOnSucceeded(t -> ret[0] = task.getValue());
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        try {
            th.join();
            return ret[0];
        } catch (InterruptedException e) {
            System.err.println("interrupted accounts thread");
            e.printStackTrace();
        }
        return null;
    }

    private byte[] SHA256(String msg) {
        SHA256Digest sha256 = new SHA256Digest();
        byte msgByte[] = msg.getBytes();
        sha256.update(msgByte, 0, msgByte.length);
        Arrays.fill(msgByte, (byte)0 );    //an attempt to zero out pwd
        byte[] hashedPwd = new byte[sha256.getDigestSize()];
        sha256.doFinal(hashedPwd, 0);
        return hashedPwd;
    }

    private byte[][] pwdBasedKey(String pwd) {
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
     * @throws LoginFailException if unsuccessful
     */
    public User login(String username, String password) throws LoginFailException {
        Task<User> task = new Task<User>() {
            @Override
            protected User call() throws Exception {
                JSONObject allegedUser = new JSONObject();
                try {
                    allegedUser.put("msgType", "login");
                    allegedUser.put("username", username);
                    allegedUser.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(password)));

                    sendJson(allegedUser);
                    JSONObject user = receiveJson();

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
        };
        final User[] ret = new User[1];
        task.setOnSucceeded(t -> ret[0] = task.getValue());
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        return ret[0];
    }

    private void sendJson(JSONObject json) throws IOException {
        System.out.println("sending json");

        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(sslSocket.getOutputStream()));
        String str = json.toString();
        w.write(str + '\n');
        w.flush();

        /*ObjectOutputStream out_to_Client = new ObjectOutputStream(sslSocket.getOutputStream());
        TransmittedFile file_to_send = new TransmittedFile();
        file_to_send.jsonString = json.toString();
        out_to_Client.writeObject(file_to_send);


        ObjectOutputStream oos = new ObjectOutputStream(sslSocket.getOutputStream());
        oos.writeObject(json.toString());
        System.out.println("sent json");*/
    }

    private JSONObject receiveJson() throws IOException, ClassNotFoundException {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(sslSocket.getInputStream()));
        String str;
        while ((str = r.readLine()) != null) {
            System.out.println(str);
            System.out.flush();
        }

        return new JSONObject(str);
        /*
        ObjectInputStream ois = new ObjectInputStream(sslSocket.getInputStream());
        String strJson = (String) ois.readObject();
        JSONObject json = new JSONObject(strJson);*/
    }

    public Folder getFolderFromId(int folderId, int uid) {
        //TODO: send to server and get the corresponding folder
        Folder parentFolder = new Folder(folderId, "", null,null);
        JSONObject json = new JSONObject();
        json.put("fsoid", folderId);
        json.put("uid", uid);
        json.put("msgType", "getChildren");
        JSONArray children = sql_connection.getChildren(json);
        for (int i=0; i<children.length(); i++) {
            JSONObject c = children.getJSONObject(i);
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

    public class LoginFailException extends Exception {
        public LoginFailException (String message) {
            super(message);
        }
    }

}
