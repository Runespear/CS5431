package org.cs5431.controller;

import org.cs5431.Validator;
import org.cs5431.model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.*;
import static org.cs5431.JSON.*;
import static org.cs5431.model.FileActionType.DOWNLOAD;

public class FileController {
    private org.cs5431.model.User user;
    private Socket sslSocket;

    public FileController(User user, Socket sslSocket) {
        this.user = user;
        this.sslSocket = sslSocket;
    }

    /**
     * Returns true if the user has the permission to perform the action.
     * @param action FileActionType that the user intends to perform
     * @return true if the user has the permission; false otherwise
     * TO BE DEPRECATED
     */
    public boolean isAllowed(FileActionType action, FileSystemObject fso) {
        //TODO: to be done on server side
        List<Integer> usersWithPermission = fso.getEditors();
        if (action == DOWNLOAD) {
            usersWithPermission.addAll(fso.getViewers());
        }
        return (usersWithPermission.contains(user.getId()));
    }

    /**
     * Sends java.io.file object to the server. Adds the file as a child
     * of the parent folder.
     * @param file File that is was returned from the javaFX dialogue box
     * @param parentFolder Folder where the file is to be uploaded
     * @throws FileControllerException If the upload fails because of some
     * communication issue with the server or server-side issue
     * @throws IOException If the file to upload cannot be read
     */
    public void uploadFile(java.io.File file, Folder parentFolder) throws
            IOException, JSONException, NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, ClassNotFoundException, FileControllerException {
        String name = file.getName();
        long size = file.length();
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        JSONObject fso = new JSONObject();
        fso.put("msgType","upload");
        fso.put("uid", user.getId());
        fso.put("parentFolderid", parentFolder.getId());

        fso.put("size", String.valueOf(size));
        fso.put("lastModified", lastModified);
        fso.put("isFile", true);

        String fileDetails[] = generateAndEncFile(file, name, user.getPubKey());
        fso.put("file", fileDetails[0]);
        fso.put("fsoName", fileDetails[1]);
        fso.put("encSK", fileDetails[2]);
        fso.put("fileIV", fileDetails[3]);
        fso.put("fsoNameIV", fileDetails[4]);
        sendJson(fso, sslSocket);
        JSONObject fsoAck = receiveJson(sslSocket);
        if (fsoAck.get("msgType").equals("uploadAck")) {
            int fileSentid = fsoAck.getInt("fsoid");
            if (DEBUG_MODE) {
                System.out.println(fileSentid);
            }
            if (fileSentid != -1) {
                File fileSent = new File(fileSentid, name, parentFolder, size, lastModified);
                fileSent.addPriv(PrivType.EDIT, user.getId());
            } else {
                throw new FileControllerException("Failed to add file");
            }
        } else if (fsoAck.getString("msgType").equals("error")) {
            throw new FileControllerException(fsoAck.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    /**
     * Creates a new folder and uploads it to the server along with its log entry. Adds the new folder as a
     * child of the parent folder.
     * @param folderName is the name of the folder that is to be created
     * @param parentFolder Folder where the file is to be uploaded
     * @throws FileControllerException If the upload fails because of some
     * communication issue with the server or server-side issue
     */
    public void createFolder(String folderName, Folder parentFolder) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, IOException, ClassNotFoundException,
            FileControllerException {
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());
        if (Validator.validFileName(folderName)) {
            JSONObject fso = new JSONObject();
            fso.put("msgType","upload");
            fso.put("uid", user.getId());
            fso.put("parentFolderid", parentFolder.getId());

            fso.put("size", "0");
            fso.put("lastModified", lastModified);
            fso.put("isFile", false);

            String fsoNameDetails[] = generateAndEncFileName(folderName, user
                    .getPubKey());
            fso.put("fsoName", fsoNameDetails[0]);
            fso.put("encSK", fsoNameDetails[1]);
            fso.put("fsoNameIV", fsoNameDetails[2]);

            sendJson(fso, sslSocket);
            JSONObject fsoAck = receiveJson(sslSocket);

            if (fsoAck.get("msgType").equals("uploadAck")) {
                int folderSentId = fsoAck.getInt("fsoid");

                if (folderSentId != -1) {
                    Folder folderSent = new Folder(folderSentId, folderName, parentFolder, lastModified);
                    folderSent.addPriv(PrivType.EDIT, user.getId()); //TODO: do we even need this
                    parentFolder.addChild(folderSent);
                } else {
                    throw new FileControllerException("Failed to create folder");
                }
            } else if (fsoAck.getString("msgType").equals("error")) {
                throw new FileControllerException(fsoAck.getString("message"));
            } else {
                throw new FileControllerException("Received bad response " +
                        "from server");
            }
        }
    }

    /**
     * Attempts to change the file contents to the originalFile contents of file. If the user has the permission,
     * the changes are sent to the server along with its log entry.
     * @param originalFile is the file to be overwritten
     * @param file object returned by the javaFX file picker dialogue box
     * @throws FileControllerException If the upload fails because of some
     * communication issue with the server or server-side issue
     * @throws IOException If the file to upload cannot be read
     */
    public void overwrite(File originalFile, java.io.File file) throws
            IOException, NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, ClassNotFoundException {
        JSONObject jsonKeys = new JSONObject();
        jsonKeys.put("msgType","overwriteKeys");
        jsonKeys.put("fsoid", originalFile.getId());
        jsonKeys.put("uid", user.getId());
        sendJson(jsonKeys, sslSocket);
        JSONObject keyResponse = receiveJson(sslSocket);
        if (keyResponse.getString("msgType").equals("overwriteKeysAck")) {
            byte[] encFileSK = Base64.getDecoder().decode(keyResponse.getString
                    ("fileSK"));
            SecretKey fileSK = decFileSecretKey(encFileSK, user.getPrivKey());
            //TODO more stuff
        } //TODO throw exception
    }

    /**
     * Attempts to rename the file. If the user has the permission, the changes are sent to
     * the server along with its log entry. If rename is successful on the server,
     * the system object on client side is renamed as well.
     * @param systemObject is the file/folder to be renamed
     * @param newName New name of the file/folder
     * @throws FileControllerException If the file is not successfully
     * modified due to communication failure with server or bad server response
     */
    public void rename(FileSystemObject systemObject, String newName)
            throws IOException, ClassNotFoundException, FileControllerException{
        JSONObject json = new JSONObject();
        json.put("msgType", "rename");
        json.put("fsoid", systemObject.getId());
        json.put("uid", user.getId());
        json.put("newName", newName);   //TODO encryption
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("renameAck") && response
                .getInt("fsoid") == systemObject.getId() && response.getInt
                ("uid") == user.getId()) {
            systemObject.rename(newName);
        }
        else if (response.getString("msgType").equals("error")) {
            throw new FileControllerException(response.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    /**
     * Gets the file/folder contents from server and decrpyts it.
     * @param fsoId is ID of the file to be downloaded
     * @throws FileControllerException If download fails
     */
    public void download(int fsoId, String fsoName, java.io.File dir) throws
        IOException, JSONException, NoSuchAlgorithmException,
        NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException, FileControllerException, ClassNotFoundException {

        JSONObject fileReq = new JSONObject();
        fileReq.put("msgType", "download");
        fileReq.put("fsoid", fsoId);
        fileReq.put("uid", user.getId());

        sendJson(fileReq, sslSocket);
        JSONObject fileAck = receiveJson(sslSocket);

        if (fileAck.getString("msgType").equals("downloadAck")) {
            int fsoID = fileAck.getInt("fsoid");
            byte encFileSKbytes[] = Base64.getDecoder()
                    .decode(fileAck.getString("encFileSK"));
            SecretKey fileSK = decFileSecretKey(encFileSKbytes, user.getPrivKey());
            byte fsoBytes[] = Base64.getDecoder().decode(fileAck
                    .getString("encFile"));
            //TODO: what should we do with dateModified and size?
            String ivString = fileAck.getString("fileIV");
            IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder()
                    .decode(ivString));
            if (!decryptFile(fsoBytes, fsoName, fileSK, iv, dir))
                throw new FileControllerException("Failed to decrypt file " +
                        "that was downloaded");
        } else if (fileAck.getString("msgType").equals("error")) {
            throw new FileControllerException(fileAck.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    /**
     * Gets all children of the folderId
     * @param parentFolder gets the children of parentFolder
     * @return List of fso if download is successful; false null
     */
    public List<FileSystemObject> getChildren(Folder parentFolder) throws
            Exception {
        int parentFolderid = parentFolder.getId();
        ArrayList<FileSystemObject> children = new ArrayList<>();
        JSONObject json = new JSONObject();
        json.put("msgType", "getChildren");
        json.put("fsoid", parentFolderid);
        json.put("uid", user.getId());
        sendJson(json, sslSocket);
        JSONArray jsonChildren = receiveJsonArray(sslSocket);
        for (int i = 0; i < jsonChildren.length(); i++) {
            JSONObject c = jsonChildren.getJSONObject(i);
            try {
                int id = c.getInt("id");
                String encName = c.getString("name");
                String size = c.getString("size");
                Long longSize = Long.valueOf(size);
                Timestamp lastModified = Timestamp.valueOf(c.getString("lastModified"));
                String ivNameString = c.getString("fsoNameIV");
                String encKeyString = c.getString("encKey");
                SecretKey fileSK = decFileSecretKey(Base64.getDecoder().decode
                                (encKeyString), user.getPrivKey());
                IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder()
                        .decode(ivNameString));
                String name = decryptFileName(Base64.getDecoder().decode
                                (encName), fileSK, ivSpec);

                String type = c.getString("FSOType");
                FileSystemObject child;
                if (type.equals("FOLDER")) {
                    child = new Folder(id, name, parentFolder,
                            lastModified);
                } else {
                    child = new File(id, name, parentFolder,
                            longSize, lastModified);
                }
                children.add(child);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return children;
    }

    /**
     * Deletes the file from server entirely and from parentFolder No one is able to access it anymore.
     * @param fso is the file to be deleted
     * @param parentFolder is parentFolder of the fso associated with the id
     * @throws FileControllerException If file cannot be deleted
     */
    public void delete(FileSystemObject fso, Folder parentFolder) throws
            FileControllerException {
        //TODO: remove from db
        parentFolder.removeChild(fso);
        //throw new FileControllerException("Failed to delete file");
    }

    /**
     * Adds privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public boolean addPriv(FileSystemObject systemObject, int userId,
                           PrivType priv) throws IOException, ClassNotFoundException {
        int newUser = -1;
        JSONObject json = new JSONObject();
        json.put("msgType", "addPriv");
        json.put("addPrivType", priv);
        json.put("fsoid", systemObject.getId());
        json.put("uid", user.getId());
        json.put("newUid", userId);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("addPrivKeys")) {
            //TODO
            return true;
        } else {
            //throw error?
            return false;
        }

        //TODO add this eventually?
        //systemObject.addPriv(priv, userId);
    }

    /**
     * Removes privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public boolean removePriv(FileSystemObject systemObject, int userId, PrivType priv) {
        return false;

        //todo add this enventually?
        //systemObject.removePriv(priv, userId);
    }

    public void rollback(int rollbackToThisfileId) {
        //TODO: how???
    }

    /**
     * Sends the update of privileges to the server with the serverIP attribute of the fileController.
     * @param fsoId ID of fso modified
     * @param userId Refers to the user that is given more privileges
     * @param priv Type of priv to be given
     * @param logEntry to be sent
     * * @return the file/folder that is uploaded to server if successful; null otherwise
     */
    private FileSystemObject addFSOPriv(int fsoId, int userId, PrivType priv, FileLogEntry logEntry) {
        //TODO: remember to add new file to parent folder of userId
        //TODO: remember to change priv to child too
        return null;
    }

    /**
     * Sends the update of privileges to the server with the serverIP attribute of the fileController.
     * @param fsoId ID of fso modified
     * @param userId Refers to the user that is to have his/her privileges removed
     * @param priv Type of priv to be given
     * @param logEntry to be sent
     * * @return the file/folder that is uploaded to server if successful; null otherwise
     */
    private FileSystemObject removeFSOPriv(int fsoId, int userId, PrivType priv, FileLogEntry logEntry) {
        //TODO: remember to remove the file from parent folder of userId if view privileges are removed
        //TODO: remember to change priv to child too
        return null;
    }

    private FileSystemObject modifyFSOName(int fsoId, String newName, FileLogEntry logEntry) {
        return null;
    }

    private FileSystemObject modifyFSOContents(int fsoId, java.io.File file, FileLogEntry logEntry) {
        return null;
    }

    public List<String> getFileLogs(int fsoid) throws IOException,
            ClassNotFoundException {
        JSONObject request = new JSONObject();
        request.put("msgType", "getFileLogs");
        request.put("fsoid", fsoid);
        request.put("uid", user.getId());
        sendJson(request, sslSocket);

        JSONObject response = receiveJson(sslSocket);
        //TODO parse into list of strings
        return null;
    }

    public class FileControllerException extends Exception {
        FileControllerException(String message) {
            super(message);
        }
    }
}
