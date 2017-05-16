package org.cs5431.controller;

import org.cs5431.Encryption;
import org.cs5431.SSS;
import org.cs5431.model.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.*;
import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.sendJson;

/**
 * A controller for an individual account, including both admins and users.
 * The controller for all accounts is called AccountController.
 */
public class UserController {
    private static User user;
    private static Socket sslSocket;

    /**
     * Creates a new UserController
     * @param user The user to control
     */
    public UserController(User user, Socket sslSocket) {
        UserController.user = user;
        UserController.sslSocket = sslSocket;
    }

    /**
     * Change the password of the user associated with this controller.
     * @param oldPassword Old password to be changed from
     * @param newPassword Password to be changed to.
     * @throws ChangePwdFailException If password could not be changed, with
     * a message explaining why
     */
    public void changePassword(String oldPassword, String newPassword) throws
            ChangePwdFailException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, IOException, ClassNotFoundException,
            PwdRecoveryException, InvalidKeySpecException {
        try {
            JSONObject getSalt = new JSONObject();
            getSalt.put("msgType", "getPrivKeySalt");
            getSalt.put("username", user.getUsername());
            sendJson(getSalt, sslSocket);
            JSONObject saltResponse = receiveJson(sslSocket);

            if (saltResponse.getString("msgType").equals("getPrivKeySaltAck")) {
                String strSalt = saltResponse.getString("privKeySalt");
                completeChangePwd(newPassword, user.getPrivKey(), strSalt,
                        user.getUsername(), oldPassword, user.getId(), sslSocket);
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

    public static void completeChangePwd(String newPassword, PrivateKey privateKey, String strSalt,
                                         String username, String oldPassword, int uid, Socket sslSocket)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, IOException, ClassNotFoundException,
            UserController.ChangePwdFailException, PwdRecoveryException, InvalidKeySpecException
            {
        JSONObject allegedUser = new JSONObject();
        byte[] privKeySalt = Base64.getDecoder().decode(strSalt);
        allegedUser.put("newPrivKey", encryptPrivateKey(newPassword, privateKey.getEncoded()
                , privKeySalt));

        allegedUser.put("msgType", "changePwd");
        allegedUser.put("uid", uid);
        allegedUser.put("username", username);
        allegedUser.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(oldPassword)));
        allegedUser.put("newHashedPwd", Base64.getEncoder().encodeToString(SHA256(newPassword)));

        sendJson(allegedUser, sslSocket);
        if (DEBUG_MODE) {
            System.out.println("waiting to receive json...");
        }
        JSONObject jsonAck = receiveJson(sslSocket);
        if (DEBUG_MODE) {
            System.out.println("change pwd json received: " + jsonAck);
        }

        if (jsonAck.getString("msgType").equals("changePwdAck")) {
            if (jsonAck.getInt("uid") == uid) {
                JSONObject recoveryInfo = getRecoveryInfo();
                boolean hasPwdRec = recoveryInfo.getBoolean("hasPwdRec");
                if (hasPwdRec) {
                    int neededUsers = recoveryInfo.getInt("neededUsers");
                    JSONArray groupArr = recoveryInfo.getJSONArray("groupUid");
                    List<Integer> nominatedUids = new ArrayList<>();
                    for (int i = 0; i < groupArr.length(); i++) {
                        nominatedUids.add(groupArr.getInt(i));
                    }
                    JSONArray pubKeyArr = recoveryInfo.getJSONArray("pubKeys");
                    List<PublicKey> publicKeys = new ArrayList<>();
                    for (int i = 0; i < pubKeyArr.length(); i++) {
                        publicKeys.add(getPubKeyFromJSON(pubKeyArr.getString(i)));
                    }

                    SSS secretGen = new SSS(nominatedUids.size(), neededUsers,
                            new BigInteger(newPassword.getBytes(StandardCharsets.UTF_8)));
                    List<String> encSecrets = encryptSecrets(publicKeys, secretGen.generateSecrets());
                    saveRecoveryInfo(true, neededUsers, nominatedUids, encSecrets);
                }
            } else {
                throw new ChangePwdFailException("Received bad response " +
                        "from server - user id does not match up");
            }
        } else if (jsonAck.getString("msgType").equals("error")) {
            throw new ChangePwdFailException(jsonAck.getString
                    ("message"));
        } else {
            throw new ChangePwdFailException("Received bad response " +
                    "from server");
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
        json.put("uid", user.getId());
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
        }
    }

    public void changePhoneNumber(String oldPhone, String newPhone) throws
            IOException, ClassNotFoundException, ChangeEmailFailException {
        JSONObject json = new JSONObject();
        json.put("msgType","changePhoneNo");
        json.put("uid", user.getId());
        json.put("oldPhone", oldPhone);
        json.put("newPhone", newPhone);
        sendJson(json, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("changePhoneAck")) {
            if (response.getInt("uid") != user.getId())
                throw new ChangeEmailFailException("Bad response from server " +
                        "- user id does not match up");
        } else if (response.getString("msgType").equals("error")) {
            throw new ChangeEmailFailException(response.getString
                    ("message"));
        }
    }

    /**
     * Deletes user with the userId and logs it.
     * @param password Password of the logged in user
     * @throws DeleteUserException If the user could not be deleted
     */
    public void deleteUser(String username, String password) throws IOException,
            ClassNotFoundException, DeleteUserException {
        JSONObject request = new JSONObject();
        request.put("msgType", "deleteUser");
        request.put("uid", user.getId());
        request.put("username", username);
        request.put("password", Base64.getEncoder().encodeToString(SHA256
                (password)));
        sendJson(request, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("deleteUserAck")) {
            if (response.getInt("uid") != user.getId())
                throw new DeleteUserException("Failed to delete user - check " +
                        "your password");
        } else if (response.getString("msgType").equals("error")) {
            throw new DeleteUserException(response.getString
                    ("message"));
        } else {
            throw new DeleteUserException("Received bad response " +
                    "from server");
        }
    }

    /**
     * Checks whether the username is the username of the logged in user
     * @param username The supposed username
     * @return true if that is indeed the username of the logged in user,
     * false otherwise
     */
    public boolean isLoggedInUser(String username) {
        return user.getUsername().equals(username);
    }

    /**
     * Logs out the current user
     */
    public void logout() throws IOException, ClassNotFoundException,
            LogoutException {
        JSONObject request = new JSONObject();
        request.put("msgType", "logout");
        request.put("uid", user.getId());
        sendJson(request, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("logoutAck")) {
            if (response.getInt("uid") != user.getId())
                throw new LogoutException("Failed to logout - wrong user " +
                        "logged out");
        } else if (response.getString("msgType").equals("error")) {
            throw new LogoutException(response.getString
                    ("message"));
        } else {
            throw new LogoutException("Received bad response " +
                    "from server");
        }
    }

    public static void saveRecoveryInfo(boolean hasRecovery,
                                 int neededUsers, List<Integer> nominatedUids,
                                 List<String> encSecrets)
            throws IOException, ClassNotFoundException, PwdRecoveryException {
        JSONObject recover = new JSONObject();
        recover.put("msgType", "setPwdGroup");
        recover.put("uid", user.getId());
        recover.put("hasPwdRec", hasRecovery);
        if (hasRecovery) {
            recover.put("neededUsers", neededUsers);
            recover.put("groupUid", nominatedUids);
            recover.put("secrets", encSecrets);
        }

        sendJson(recover, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("setPwdGroupAck")) {
            if (response.getInt("uid") != user.getId())
                throw new PwdRecoveryException("Password recovery info was set for wrong user!");
        } else if (response.getString("msgType").equals("error")) {
            throw new PwdRecoveryException(response.getString
                    ("message"));
        } else {
            throw new PwdRecoveryException("Received bad response " +
                    "from server");
        }
    }

    public void save2fa(int has2fa) throws IOException,
            ClassNotFoundException, TwoFactorException {
        JSONObject json = new JSONObject();
        json.put("msgType","2faToggle");
        json.put("uid", user.getId());
        json.put("newToggle", has2fa);

        sendJson(json, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("2faToggleAck")) {
            if (response.getInt("uid") != user.getId())
                throw new TwoFactorException("Two factor authentication info" +
                        " was set for wrong user!");
        } else if (response.getString("msgType").equals("error")) {
            throw new TwoFactorException(response.getString
                    ("message"));
        } else {
            throw new TwoFactorException("Received bad response " +
                    "from server");
        }
    }

    public static JSONObject getRecoveryInfo() throws IOException, ClassNotFoundException,
            PwdRecoveryException {
        JSONObject recover = new JSONObject();
        recover.put("msgType", "pwdRecoveryInfo");
        recover.put("uid", user.getId());

        sendJson(recover, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("pwdRecoveryInfoAck")) {
            if (response.getInt("uid") == user.getId()) {
                return response;
            } else {
                throw new PwdRecoveryException("Got password recovery information" +
                        "for wrong user!");
            }
        } else if (response.getString("msgType").equals("error")) {
            throw new PwdRecoveryException(response.getString
                    ("message"));
        } else {
            throw new PwdRecoveryException("Received bad response " +
                    "from server");
        }
    }

    public void checkLoggedInPwd(String password) throws IOException,
            ClassNotFoundException, PwdRecoveryException{
        JSONObject json = new JSONObject();
        json.put("msgType", "checkPwd");
        json.put("uid", user.getId());
        json.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(password)));

        sendJson(json, sslSocket);

        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("checkPwdAck")) {
            if (response.getInt("uid") != user.getId())
                throw new PwdRecoveryException("Wrong user id received");
        } else if (response.getString("msgType").equals("error")) {
            throw new PwdRecoveryException(response.getString
                    ("message"));
        } else {
            throw new PwdRecoveryException("Received bad response " +
                    "from server");
        }
    }

    public String getEmail() {
        return user.getEmail();
    }

    public void setEmail(String email) {
        user.setEmail(email);
    }

    public void setPhoneNo(String phoneNo) {
        user.setPhoneNo(phoneNo);
    }

    public void setHas2fa(int has2fa) {
        user.setHas2fa(has2fa);
    }

    public String getPhoneNo() {
        return user.getPhoneNo();
    }

    public int getHas2fa() {
        return user.getHas2fa();
    }

    public String decryptSecret(String code) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return Encryption.decryptSecret(user.getPrivKey(), code);
    }

    public static class ChangePwdFailException extends Exception {
        ChangePwdFailException(String message) {
            super(message);
        }
    }

    public static class ChangeEmailFailException extends Exception {
        ChangeEmailFailException(String message) {
            super(message);
        }
    }

    public static class DeleteUserException extends Exception {
        DeleteUserException(String message) {
            super(message);
        }
    }

    public static class LogoutException extends Exception {
        LogoutException(String message) {
            super(message);
        }
    }

    public static class PwdRecoveryException extends Exception {
        PwdRecoveryException(String message) {
            super(message);
        }
    }

    public static class TwoFactorException extends Exception {
        TwoFactorException(String message) {
            super(message);
        }
    }
 }
