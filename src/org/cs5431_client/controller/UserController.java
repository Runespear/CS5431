package org.cs5431_client.controller;

import javafx.concurrent.Task;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
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
    public int changePassword(String oldPassword, String newPassword) throws ChangePwdFailException {
        try {
            JSONObject allegedUser = new JSONObject();
            String username = user.getUsername();
            allegedUser.put("msgType", "changePwd");
            allegedUser.put("uid", user.getId());
            allegedUser.put("username", username);
            allegedUser.put("hashedPwd", Base64.getEncoder().encodeToString(SHA256(oldPassword)));
            allegedUser.put("newHashedPwd", Base64.getEncoder().encodeToString(SHA256(newPassword)));

            sendJson(allegedUser);
            System.out.println("waiting to receive json...");
            JSONObject user = receiveJson();
            System.out.println("change pwd json recived: " + user);

            if (user.getString("msgType").equals("changePwdAck")) {
                int uid = user.getInt("uid");
                return uid;
            } else if (user.getString("msgType").equals("error")) {
                throw new ChangePwdFailException(user.getString
                        ("message"));
            } else {
                throw new ChangePwdFailException("Received bad response " +
                        "from server");
            }
            //TODO: create relevant controllers? and pass them? ???
        } catch (JSONException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
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

    private byte[] SHA256(String msg) {
        SHA256Digest sha256 = new SHA256Digest();
        byte msgByte[] = msg.getBytes();
        sha256.update(msgByte, 0, msgByte.length);
        Arrays.fill(msgByte, (byte)0 );    //an attempt to zero out pwd
        byte[] hashedPwd = new byte[sha256.getDigestSize()];
        sha256.doFinal(hashedPwd, 0);
        return hashedPwd;
    }

    private void sendJson(JSONObject json) throws IOException {
        System.out.println("sending json");

        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(sslSocket.getOutputStream()));
        String str = json.toString();
        System.out.println(str);
        w.write(str + '\n');
        w.flush();
    }

    private JSONObject receiveJson() throws IOException, ClassNotFoundException {
        System.out.println("received json to change password");
        BufferedReader r = new BufferedReader(
                new InputStreamReader(sslSocket.getInputStream()));
        String str;
        str = r.readLine();
        System.out.println(str);
        System.out.flush();

        return new JSONObject(str);
    }

    public class ChangePwdFailException extends Exception {
        public ChangePwdFailException (String message) {
            super(message);
        }
    }
 }
