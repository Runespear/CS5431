package org.cs5431_client.controller;

import javafx.concurrent.Task;
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
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                //TODO: connect to server
                //TODO: decide what it should return
                //TODO: server side old password check
                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
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
 }
