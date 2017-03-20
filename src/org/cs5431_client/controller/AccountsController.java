package org.cs5431_client.controller;

import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * A controller for all accounts.
 * The controller for an individual account is called UserController.
 */
public class AccountsController {

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
            user.put("pwd", password);
            user.put("email", email);

            JSONObject newUser = sendUser(user);
            int uid = newUser.getInt("uid");
            int parentFolderid = newUser.getInt("parentFolderid");
            SecretKey privKey = (SecretKey) user.get("privKey"); //TODO: how to convert to secret key
            SecretKey pubKey = (SecretKey) user.get("pubKey"); //TODO: how to convert to secret key
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            Folder parentFolder = new Folder(parentFolderid, username, null, uid, lastModified);
            return new User(uid, username, email, parentFolder, privKey, pubKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject sendUser(JSONObject user) {
        //TODO: send to server
        return null;
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
    public int login(String username, String password, String serverIP,
                         String serverPort) {
        //TODO: establish connection

        //TODO: attempt to connect with given credentials
        JSONObject allegedUser = new JSONObject();
        try {
            allegedUser.put("username", username);
            allegedUser.put("pwd", password);
            //TODO: send allegeduser to server and check

            JSONObject user = new JSONObject();
            int uid = user.getInt("uid");
            int parentFolderid = user.getInt("parentFolderid");
            String email = user.getString("email");
            SecretKey privKey = (SecretKey) user.get("privKey"); //TODO: how to convert to secret key
            SecretKey pubKey = (SecretKey) user.get("pubKey"); //TODO: how to convert to secret key
            Folder parentFolder = getFolderFromId(parentFolderid);
            User currUser = new User(uid, username, email, parentFolder, privKey,pubKey);

            //TODO: create relevant controllers? and pass them? ???
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public Folder getFolderFromId(int folderId) {
        //TODO: send to server and get the corresponding folder
        return null;
    }

    public class RegistrationFailException extends Exception {
        public RegistrationFailException (String message) {
            super(message);
        }
    }

}
