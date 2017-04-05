package org.cs5431.controller;

import javafx.concurrent.Task;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.User;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.SHA256;
import static org.cs5431.Encryption.pwdBasedKey;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.sendJson;

/**
 * A controller for an individual account, including both admins and users.
 * The controller for all accounts is called AccountController.
 */
public class UserController {
    private FileController fileController;
    private User user;
    private Socket sslSocket;

    /**
     * Creates a new UserController
     * @param user The user to control
     */
    public UserController(User user, Socket sslSocket) {
        this.user = user;
        this.sslSocket = sslSocket;
        this.fileController = new FileController(user, sslSocket);
    }

    /**
     * Change the password of the user associated with this controller.
     * @param oldPassword Old password to be changed from
     * @param newPassword Password to be changed to.
     */
    public int changePassword(String oldPassword, String newPassword) throws ChangePwdFailException {
        try {
            JSONObject getSalt = new JSONObject();
            getSalt.put("msgType", "getPrivKeySalt");
            getSalt.put("username", user.getUsername());
            sendJson(getSalt, sslSocket);
            JSONObject saltResponse = receiveJson(sslSocket);

            if (saltResponse.getString("msgType").equals("getPrivKeySaltAck")) {
                String strSalt = saltResponse.getString("privKeySalt");
                byte[] privKeySalt = Base64.getDecoder().decode(strSalt);
                JSONObject allegedUser = new JSONObject();

                byte keyAndSalt[][] = pwdBasedKey(newPassword, privKeySalt);
                byte key[] = keyAndSalt[0];
                SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
                IvParameterSpec iv = new IvParameterSpec(new byte[16]);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
                byte encryptedKey[] = cipher.doFinal(user.getPrivKey().getEncoded());
                allegedUser.put("newPrivKey", Base64.getEncoder().encodeToString(encryptedKey));

                String username = user.getUsername();
                allegedUser.put("msgType", "changePwd");
                allegedUser.put("uid", user.getId());
                allegedUser.put("username", username);
                allegedUser.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(oldPassword)));
                allegedUser.put("newHashedPwd", Base64.getEncoder().encodeToString(SHA256(newPassword)));

                sendJson(allegedUser, sslSocket);
                if (DEBUG_MODE) {
                    System.out.println("waiting to receive json...");
                }
                JSONObject user = receiveJson(sslSocket);
                if (DEBUG_MODE) {
                    System.out.println("change pwd json recived: " + user);
                }

                if (user.getString("msgType").equals("changePwdAck")) {
                    int uid = user.getInt("uid");
                    return uid;
                } else if (user.getString("msgType").equals("error")) {
                    throw new ChangePwdFailException(user.getString
                            ("message"));
                } else {
                    throw new ChangePwdFailException("Received bad response " +
                            "from server");
                }
            } else if (saltResponse.getString("msgType").equals("error")) {
                throw new ChangePwdFailException(saltResponse.getString
                        ("message"));
            } else {
                throw new ChangePwdFailException("Received bad response " +
                        "from server");
            }
        } catch (JSONException | IOException | ClassNotFoundException | NoSuchAlgorithmException
                | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException
                | BadPaddingException | NoSuchProviderException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Change the password of the user associated with this controller.
     * @param oldEmail Old password to be changed from
     * @param newEmail Password to be changed to.
     */
    public void changeEmail(String oldEmail, String newEmail) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            //TODO: connect to server
            //TODO: decide what it should return
            //TODO: server side old email check
                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public List<FileSystemObject> getFileSystemObjects() {
        Task<List<FileSystemObject>> task = new Task<List<FileSystemObject>>() {
            @Override
            protected List<FileSystemObject> call() throws Exception {
                int parentFolderId = user.getUserParentFolder().getId();

                //TODO: query server to get all children associated with this folderId
                //TODO: decrypt file names
                return null;
            }
        };
        final Object[] ret = {null};
        task.setOnSucceeded(t -> ret[0] = task.getValue());
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        if (ret[0] instanceof List<?>) {
            return (List<FileSystemObject>) ret[0];
        }
        return null;
    }

    public class ChangePwdFailException extends Exception {
        public ChangePwdFailException (String message) {
            super(message);
        }
    }
 }
