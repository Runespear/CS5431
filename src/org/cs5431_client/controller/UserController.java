package org.cs5431_client.controller;

import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.User;

import java.net.Socket;
import java.util.List;

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
    public void changePassword(String oldPassword, String newPassword) {
        //TODO: connect to server
        //TODO: decide what it should return
        //TODO: server side old password check
    }

    /**
     * Change the password of the user associated with this controller.
     * @param oldEmail Old password to be changed from
     * @param newEmail Password to be changed to.
     */
    public void changeEmail(String oldEmail, String newEmail) {
        //TODO: connect to server
        //TODO: decide what it should return
        //TODO: server side old email check
    }

    public List<FileSystemObject> getFileSystemObjects() {
        int parentFolderId = user.getUserParentFolder().getId();

        //TODO: query server to get all children associated with this folderId
        //TODO: decrpyt file names
        return null;
    }
 }
