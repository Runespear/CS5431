package org.cs5431.controller;

import org.cs5431.model.User;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.SHA256;
import static org.cs5431.Encryption.encryptPrivateKey;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.sendJson;

/**
 * A controller for an individual account, including both admins and users.
 * The controller for all accounts is called AccountController.
 */
public class UserController {
    private User user;
    private Socket sslSocket;

    /**
     * Creates a new UserController
     * @param user The user to control
     */
    public UserController(User user, Socket sslSocket) {
        this.user = user;
        this.sslSocket = sslSocket;
    }

    /**
     * Change the password of the user associated with this controller.
     * @param oldPassword Old password to be changed from
     * @param newPassword Password to be changed to.
     * @throws ChangePwdFailException If password could not be changed, with
     * a message explaining why
     */
    public void changePassword(String oldPassword, String newPassword) throws
            ChangePwdFailException {
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
                allegedUser.put("newPrivKey", encryptPrivateKey(newPassword,
                        user.getPrivKey().getEncoded(), privKeySalt));

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
                JSONObject jsonAck = receiveJson(sslSocket);
                if (DEBUG_MODE) {
                    System.out.println("change pwd json recived: " + jsonAck);
                }

                if (jsonAck.getString("msgType").equals("changePwdAck")) {
                    if (jsonAck.getInt("uid") != user.getId())
                        throw new ChangePwdFailException("Received bad response " +
                                "from server - user id does not match up");
                } else if (jsonAck.getString("msgType").equals("error")) {
                    throw new ChangePwdFailException(jsonAck.getString
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
    }

    /**
     * Change the password of the user associated with this controller.
     * @param oldEmail Old password to be changed from
     * @param newEmail Password to be changed to.
     * @throws ChangeEmailFailException if failed, with message explaining why
     */
    public void changeEmail(String oldEmail, String newEmail) throws
            IOException, ClassNotFoundException, ChangeEmailFailException {
        JSONObject json = new JSONObject();
        json.put("msgType","changeEmail");
        json.put("oldEmail", oldEmail);
        json.put("newEmail", newEmail);
        sendJson(json, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("changeEmailAck")) {
            if (response.getInt("uid") != user.getId())
                throw new ChangeEmailFailException("Bad response from server " +
                        "- user id does not match up");
        } else if (response.getString("msgType").equals("error")) {
            throw new ChangeEmailFailException(response.getString
                    ("message"));
        } else {
            throw new ChangeEmailFailException("Received bad response " +
                    "from server");
        }
    }

    public class ChangePwdFailException extends Exception {
        ChangePwdFailException(String message) {
            super(message);
        }
    }

    public class ChangeEmailFailException extends Exception {
        ChangeEmailFailException(String message) {
            super(message);
        }
    }
 }
