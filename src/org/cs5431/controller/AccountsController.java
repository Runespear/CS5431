package org.cs5431.controller;

public class AccountsController {

    /*
    * Creates user with the username, password, and email provided.
    * Return: userId if successful, throws RegistrationFailException if unsuccessful.
    */
    public int createUser(String username, String password, String email) throws RegistrationFailException {
        //TODO
        return -1;
    }

    /*
    * Deletes user with the userId.
    * Return: true if successful, returns false otherwise.
    */
    public boolean deleteUser(int userId) {
        //TODO
        return false;
    }

    /*
    * Creates a connection with serverIP and logs in with the username and password.
    * Return: true if successful, returns false otherwise.
    */
    public void login(String username, String password, String serverIP) {
        //TODO
    }

    public class RegistrationFailException extends Exception {
        public RegistrationFailException (String message) {
            super(message);
        }
    }

}
