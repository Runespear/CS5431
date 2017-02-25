package org.cs5431.controller;

/**
 * A controller for all accounts.
 * The controller for an individual account is called UserController.
 */
public class AccountsController {

    /**
    * Creates user with the username, password, and email provided.
    * @return userId if successful
    * @throws RegistrationFailException if unsuccessful
    */
    public int createUser(String username, String password, String email) throws RegistrationFailException {
        //TODO
        return -1;
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
     * @return true if successful, false otherwise.
     */
    public boolean login(String username, String password, String serverIP) {
        //TODO: establish connection

        //TODO: attempt to connect with given credentials

        //TODO: create relevant controllers? and pass them? ???
        return false;
    }

    public class RegistrationFailException extends Exception {
        public RegistrationFailException (String message) {
            super(message);
        }
    }

}
