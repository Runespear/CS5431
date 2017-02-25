package org.cs5431.controller;

import org.cs5431.model.User;

/**
 * A controller for an individual account, including both admins and users.
 * The controller for all accounts is called AccountController.
 */
public class UserController {
    private AccountsLogController logController;
    private User user;

    /**
     * Creates a new UserController with its associated AccountsLogController
     * @param user The user to control
     */
    public UserController(User user) {
        this.user = user;
        this.logController = new AccountsLogController(user);
    }

    /**
     * Change the password of the user associated with this controller.
     * @param newPassword Password to be changed to.
     */
    public void changePassword(String newPassword) {
        //TODO
    }
}
