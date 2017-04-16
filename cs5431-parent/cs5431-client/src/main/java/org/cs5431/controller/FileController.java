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
import java.security.*;
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

    public boolean isEditor(FileSystemObject fso) {
        return fso.isEditor();
    }

    public boolean isViewer(FileSystemObject fso) {
        return fso.isViewer();
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
                File fileSent = new File(fileSentid, name, size,
                        lastModified, true, true);
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
                    Folder folderSent = new Folder(folderSentId, folderName,
                            lastModified, true, true);
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
            BadPaddingException, ClassNotFoundException, FileControllerException {
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
            JSONObject jsonFile = new JSONObject();
            jsonFile.put("msgType", "overwrite");
            jsonFile.put("fsoid", originalFile.getId());
            jsonFile.put("uid", user.getId());
            String reencryptedFile[] = reEncryptFile(file, fileSK);
            jsonFile.put("newFileIV", reencryptedFile[0]);
            jsonFile.put("encFile", reencryptedFile[1]);

            sendJson(jsonFile, sslSocket);
            JSONObject fileResponse = receiveJson(sslSocket);
            if (fileResponse.getString("msgType").equals("overwriteKeysAck")) {
                if (fileResponse.getInt("fsoid") != originalFile.getId() ||
                        fileResponse.getInt("uid") != user.getId())
                    throw new FileControllerException("Received bad response " +
                            "from server - details don't match the file " +
                            "changed");
            } else if (fileResponse.getString("msgType").equals("error")) {
                throw new FileControllerException(fileResponse.getString("message"));
            } else {
                throw new FileControllerException("Received bad response " +
                        "from server");
            }
        } else if (keyResponse.getString("msgType").equals("error")) {
            throw new FileControllerException(keyResponse.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
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
            throws IOException, ClassNotFoundException,
            FileControllerException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException,
            IOException, IllegalBlockSizeException, BadPaddingException{
        JSONObject jsonKeys = new JSONObject();
        jsonKeys.put("msgType","renameKeys");
        jsonKeys.put("fsoid", systemObject.getId());
        jsonKeys.put("uid", user.getId());
        sendJson(jsonKeys, sslSocket);
        JSONObject keyResponse = receiveJson(sslSocket);
        if (keyResponse.getString("msgType").equals("renameKeysAck")) {
            byte[] encFileSK = Base64.getDecoder().decode(keyResponse.getString
                    ("fileSK"));
            SecretKey fileSK = decFileSecretKey(encFileSK, user.getPrivKey());
            JSONObject jsonFile = new JSONObject();
            jsonFile.put("msgType", "rename");
            jsonFile.put("fsoid", systemObject.getId());
            jsonFile.put("uid", user.getId());
            String reencryptedFile[] = reEncryptFileName(newName, fileSK);
            jsonFile.put("newFsoNameIV", reencryptedFile[0]);
            jsonFile.put("newName", reencryptedFile[1]);

            sendJson(jsonFile, sslSocket);
            JSONObject fileResponse = receiveJson(sslSocket);
            if (fileResponse.getString("msgType").equals("renameKeysAck")) {
                if (fileResponse.getInt("fsoid") != systemObject.getId() ||
                        fileResponse.getInt("uid") != user.getId())
                    throw new FileControllerException("Received bad response " +
                            "from server - details don't match the file " +
                            "changed");
                else
                    systemObject.rename(newName);
            } else if (fileResponse.getString("msgType").equals("error")) {
                throw new FileControllerException(fileResponse.getString("message"));
            } else {
                throw new FileControllerException("Received bad response " +
                        "from server");
            }
        } else if (keyResponse.getString("msgType").equals("error")) {
            throw new FileControllerException(keyResponse.getString("message"));
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
            if (fileAck.getInt("fsoid") != fsoId)
                throw new FileControllerException("Received bad response " +
                        "from server");
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
        int uid = user.getId();
        return getChildrenWithId(parentFolderid, uid, sslSocket, user.getPrivKey());
    }

    public static List<FileSystemObject> getChildrenWithId(int folderid, int
            uid, Socket s, PrivateKey userPrivKey) throws Exception {
        ArrayList<FileSystemObject> children = new ArrayList<>();
        JSONObject json = new JSONObject();
        json.put("msgType", "getChildren");
        json.put("fsoid", folderid);
        json.put("uid", uid);
        sendJson(json, s);
        JSONArray jsonChildren = receiveJsonArray(s);
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
                boolean isEditor = c.getBoolean("isEditor");
                boolean isViewer = c.getBoolean("isViewer");
                SecretKey fileSK = decFileSecretKey(Base64.getDecoder().decode
                        (encKeyString), userPrivKey);
                IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder()
                        .decode(ivNameString));
                String name = decryptFileName(Base64.getDecoder().decode
                        (encName), fileSK, ivSpec);

                String type = c.getString("FSOType");
                FileSystemObject child;
                if (type.equals("FOLDER")) {
                    child = new Folder(id, name, lastModified, isEditor, isViewer);
                } else {
                    child = new File(id, name, longSize, lastModified, isEditor, isViewer);
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
            FileControllerException, IOException, ClassNotFoundException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", "delete");
        jsonObject.put("fsoid", fso.getId());
        jsonObject.put("uid", user.getId());
        sendJson(jsonObject, sslSocket);

        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("deleteAck")) {
            if (response.getInt("fsoid") != fso.getId())
                throw new FileControllerException("Received bad response " +
                        "from server");
            else
                parentFolder.removeChild(fso);
        } else if (response.getString("msgType").equals("error")) {
            throw new FileControllerException(response.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
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
     * * @return the file/folder that is uploaded to server if successful; null otherwise
     */
    private FileSystemObject addFSOPriv(int fsoId, int userId, PrivType priv) {
        //TODO: remember to add new file to parent folder of userId
        //TODO: remember to change priv to child too
        return null;
    }

    /**
     * Sends the update of privileges to the server with the serverIP attribute of the fileController.
     * @param fsoId ID of fso modified
     * @param userId Refers to the user that is to have his/her privileges removed
     * @param priv Type of priv to be given
     * * @return the file/folder that is uploaded to server if successful; null otherwise
     */
    private FileSystemObject removeFSOPriv(int fsoId, int userId, PrivType priv) {
        //TODO: remember to remove the file from parent folder of userId if view privileges are removed
        //TODO: remember to change priv to child too
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


    public JSONObject getEditorsViewers(FileSystemObject fso)
            throws IOException, ClassNotFoundException, FileControllerException{
        JSONObject request = new JSONObject();
        request.put("msgType", "getEditorViewerList");
        request.put("fsoid", fso.getId());
        request.put("uid", user.getId());
        sendJson(request, sslSocket);

        JSONObject reqAck = receiveJson(sslSocket);
        if (reqAck.getString("msgType").equals("getEditorViewerListAck")) {
            return reqAck;
        } else if (reqAck.getString("msgType").equals("error")) {
            throw new FileControllerException(reqAck.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    public class FileControllerException extends Exception {
        FileControllerException(String message) {
            super(message);
        }
    }
}
