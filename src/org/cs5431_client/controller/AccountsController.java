package org.cs5431_client.controller;

import org.cs5431_client.model.*;
import org.cs5431_client.util.SQL_Connection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;

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

        boolean isUniqueUsername = sql_connection.isUniqueUsername(username);

        if (isUniqueUsername) {
            try {
                JSONObject user = new JSONObject();
                user.put("username", username);
                user.put("pwd", password);
                user.put("email", email);

                //TODO: priv key must be generated on client side and
                // encrypted using pwd-based encryption

                JSONObject newUser = sendUser(user);
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
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private JSONObject sendUser(JSONObject user) {
        //TODO: send to server

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
