package org.cs5431_client.controller;

import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;

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
        //TODO: create new user parent folder in database
        Folder parentFolder = new Folder(username, null, -1);
        return new User(-1, username, email, parentFolder);
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

        //TODO: create relevant controllers? and pass them? ???
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
