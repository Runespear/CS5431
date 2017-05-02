package org.cs5431.controller;

import org.cs5431.EncFilePacket;
import org.cs5431.Validator;
import org.cs5431.model.File;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.Folder;
import org.cs5431.model.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.DestroyFailedException;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.*;

import static org.cs5431.Constants.CAN_KEYS_BE_DESTROYED;
import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.*;
import static org.cs5431.JSON.*;

public class FileController {
    private org.cs5431.model.User user;
    private Socket sslSocket;
    private Map<Integer, String> uidToUsername = new HashMap<>();

    public FileController(User user, Socket sslSocket) {
        this.user = user;
        this.sslSocket = sslSocket;
    }

    public boolean isEditor(FileSystemObject fso) {
        return fso.isEditor();
    }

    /*unused
    public boolean isViewer(FileSystemObject fso) {
        return fso.isViewer();
    }*/

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
            BadPaddingException, ClassNotFoundException, FileControllerException,
            InvalidKeySpecException {
        JSONObject keys = new JSONObject();
        keys.put("msgType","uploadKeys");
        keys.put("fsoid", parentFolder.getId());
        keys.put("uid", user.getId());

        sendJson(keys, sslSocket);
        JSONObject keysAck = receiveJson(sslSocket);
        if (keysAck.get("msgType").equals("uploadKeysAck")) {
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

            JSONArray editors = keysAck.getJSONArray("editors");
            JSONArray viewers = keysAck.getJSONArray("viewers");
            JSONArray editorsKeyStrings = keysAck.getJSONArray("editorsKeys");
            JSONArray viewersKeyStrings = keysAck.getJSONArray("viewersKeys");
            PublicKey editorsKeys[] = new PublicKey[editorsKeyStrings.length()];
            PublicKey viewersKeys[] = new PublicKey[viewersKeyStrings.length()];
            for (int i = 0; i < editorsKeyStrings.length(); i++) {
                editorsKeys[i] = getPubKeyFromJSON(editorsKeyStrings.getString(i));
            }
            for (int i = 0; i < viewersKeyStrings.length(); i++) {
                viewersKeys[i] = getPubKeyFromJSON(viewersKeyStrings.getString(i));
            }
            EncFilePacket efp = generateAndEncFile(file, name, editorsKeys, viewersKeys);
            fso.put("file", efp.encFile);
            fso.put("fsoName", efp.encFileName);
            fso.put("editors", editors);
            fso.put("editorsKeys", efp.editorsFileSK);
            fso.put("viewers", viewers);
            fso.put("viewersKeys", efp.viewersFileSK);
            fso.put("fileIV", efp.fileIV);
            fso.put("fsoNameIV", efp.fsoNameIV);

            sendJson(fso, sslSocket);
            JSONObject fsoAck = receiveJson(sslSocket);
            if (fsoAck.get("msgType").equals("uploadAck")) {
                int fileSentid = fsoAck.getInt("fsoid");
                if (DEBUG_MODE) {
                    System.out.println(fileSentid);
                }
                if (fileSentid == -1) {
                    throw new FileControllerException("Failed to add file");
                }
            } else if (fsoAck.getString("msgType").equals("error")) {
                throw new FileControllerException(fsoAck.getString("message"));
            } else {
                throw new FileControllerException("Received bad response " +
                        "from server");
            }
        } else if (keysAck.getString("msgType").equals("error")) {
            throw new FileControllerException(keysAck.getString("message"));
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
            FileControllerException, InvalidKeySpecException {

        Timestamp lastModified = new Timestamp(System.currentTimeMillis());
        if (Validator.validFileName(folderName)) {
            JSONObject keys = new JSONObject();
            keys.put("msgType","uploadKeys");
            keys.put("fsoid", parentFolder.getId());
            keys.put("uid", user.getId());

            sendJson(keys, sslSocket);
            JSONObject keysAck = receiveJson(sslSocket);
            if (keysAck.get("msgType").equals("uploadKeysAck")) {
                JSONObject fso = new JSONObject();
                fso.put("msgType","upload");
                fso.put("uid", user.getId());
                fso.put("parentFolderid", parentFolder.getId());

                fso.put("size", "0");
                fso.put("lastModified", lastModified);
                fso.put("isFile", false);

                JSONArray editors = keysAck.getJSONArray("editors");
                JSONArray viewers = keysAck.getJSONArray("viewers");
                JSONArray editorsKeyStrings = keysAck.getJSONArray("editorsKeys");
                JSONArray viewersKeyStrings = keysAck.getJSONArray("viewersKeys");
                PublicKey editorsKeys[] = new PublicKey[editorsKeyStrings.length()];
                PublicKey viewersKeys[] = new PublicKey[viewersKeyStrings.length()];
                for (int i = 0; i < editorsKeyStrings.length(); i++) {
                    editorsKeys[i] = getPubKeyFromJSON(editorsKeyStrings.getString(i));
                }
                for (int i = 0; i < viewersKeyStrings.length(); i++) {
                    viewersKeys[i] = getPubKeyFromJSON(viewersKeyStrings.getString(i));
                }

                EncFilePacket efp = generateAndEncFileName(folderName, editorsKeys, viewersKeys);
                fso.put("fsoName", efp.encFileName);
                fso.put("editors", editors);
                fso.put("editorsKeys", efp.editorsFileSK);
                fso.put("viewers", viewers);
                fso.put("viewersKeys", efp.viewersFileSK);
                fso.put("fsoNameIV", efp.fsoNameIV);

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
            } else if (keysAck.getString("msgType").equals("error")) {
                throw new FileControllerException(keysAck.getString("message"));
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
            if (fileResponse.getString("msgType").equals("overwriteAck")) {
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
            if (CAN_KEYS_BE_DESTROYED) {
                try {
                    fileSK.destroy();
                } catch (DestroyFailedException e) {
                    e.printStackTrace();
                }
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
            throws ClassNotFoundException,
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
            if (fileResponse.getString("msgType").equals("renameAck")) {
                if (fileResponse.getInt("fsoid") != systemObject.getId() ||
                        fileResponse.getInt("uid") != user.getId()) {
                    throw new FileControllerException("Received bad response " +
                            "from server - details don't match the file " +
                            "changed");
                } else {
                    systemObject.rename(newName);
                }
            } else if (fileResponse.getString("msgType").equals("error")) {
                throw new FileControllerException(fileResponse.getString("message"));
            } else {
                throw new FileControllerException("Received bad response " +
                        "from server");
            }
            if (CAN_KEYS_BE_DESTROYED) {
                try {
                    fileSK.destroy();
                } catch (DestroyFailedException e) {
                    e.printStackTrace();
                }
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
            if (CAN_KEYS_BE_DESTROYED) {
                try {
                    fileSK.destroy();
                } catch (DestroyFailedException e) {
                    e.printStackTrace();
                }
            }
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

    static List<FileSystemObject> getChildrenWithId(int folderid, int
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
                if (CAN_KEYS_BE_DESTROYED) {
                    try {
                        fileSK.destroy();
                    } catch (DestroyFailedException e) {
                        e.printStackTrace();
                    }
                }
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
    public void deleteForAll(FileSystemObject fso, Folder parentFolder) throws
            FileControllerException, IOException, ClassNotFoundException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", "deleteForAll");
        jsonObject.put("fsoid", fso.getId());
        jsonObject.put("uid", user.getId());
        sendJson(jsonObject, sslSocket);

        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("deleteForAllAck")) {
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
     * Deletes the file only for the user so they can't see it any more.
     * @param fso is the file to be deleted
     * @param parentFolder is parentFolder of the fso associated with the id
     * @throws FileControllerException If file cannot be deleted
     */
    public void deleteForUser(FileSystemObject fso, Folder parentFolder) throws
            FileControllerException, IOException, ClassNotFoundException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", "deleteForUser");
        jsonObject.put("fsoid", fso.getId());
        jsonObject.put("uid", user.getId());
        sendJson(jsonObject, sslSocket);

        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("deleteForUserAck")) {
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

    public void addEditor(FileSystemObject systemObject, int newUserId)
            throws IOException, ClassNotFoundException,
            FileControllerException {
        JSONObject json = new JSONObject();
        json.put("msgType", "addEditor");
        json.put("fsoid", systemObject.getId());
        json.put("uid", user.getId());
        json.put("newUid", newUserId);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);

        if (response.getString("msgType").equals("addEditorAck")) {
            if (response.getInt("newUid") != newUserId)
                throw new FileControllerException("User that was added as " +
                        "editor was not the user requested");
        } else if (response.getString("msgType").equals("error")) {
            throw new FileControllerException(response.getString("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    public void addNewViewer(FileSystemObject systemObject, int newUserId)
            throws IOException, ClassNotFoundException,
            FileControllerException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeySpecException {
        int fsoid = systemObject.getId();
        int uid = user.getId();
        JSONObject jsonKeys = new JSONObject();
        jsonKeys.put("msgType", "addViewerKeys");
        jsonKeys.put("fsoid", fsoid);
        jsonKeys.put("uid", uid);
        jsonKeys.put("newUid", newUserId);
        sendJson(jsonKeys, sslSocket);
        JSONObject responseKeys = receiveJson(sslSocket);

        if (responseKeys.getString("msgType").equals("addViewerKeysAck")) {
            String pubKeyString = responseKeys.getString("pubKey");
            JSONArray fileSKArr = responseKeys.getJSONArray("secretKey");
            JSONObject hash = responseKeys.getJSONObject("fsoid");
            //JSONArray fsoidArr = responseKeys.getJSONArray("fsoid");
            PublicKey pubKey = getPubKeyFromJSON(pubKeyString);

            List<String> encSKList = new ArrayList<>();
            List<Integer> fsoIdList = new ArrayList<>();
            List<Integer> fsoParentList = new ArrayList<>();
            for (int i = 0; i < fileSKArr.length(); i++) {
                SecretKey fileSK = decFileSecretKey(Base64.getDecoder().decode
                        (fileSKArr.getString(i)), user.getPrivKey());
                byte encFileSK[] = encFileSecretKey(fileSK, pubKey);
                encSKList.add(Base64.getEncoder().encodeToString(encFileSK));

                if (CAN_KEYS_BE_DESTROYED) {
                    try {
                        fileSK.destroy();
                    } catch (DestroyFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Iterator<?> keys = hash.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                fsoIdList.add(Integer.valueOf(key));
                fsoParentList.add(hash.getInt(key));
            }
            addViewer(fsoIdList, fsoParentList, uid, newUserId, encSKList);
        } else if (responseKeys.getString("msgType").equals("error")) {
            throw new FileControllerException(responseKeys.getString
                    ("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    private void addViewer(List<Integer> fsoid, List<Integer> parentid, int uid,  int newUserId,
                           List<String> encSecretKey) throws IOException, ClassNotFoundException,
            FileControllerException {
        JSONObject jsonViewer = new JSONObject();
        jsonViewer.put("msgType", "addViewer");
        jsonViewer.put("encSecretKey", encSecretKey);
        jsonViewer.put("fsoid", fsoid);
        jsonViewer.put("parentid", parentid);
        jsonViewer.put("uid", uid);
        jsonViewer.put("newUid", newUserId);
        sendJson(jsonViewer, sslSocket);

        JSONObject responseViewer = receiveJson(sslSocket);
        if (responseViewer.getString("msgType").equals("addViewerAck")) {
            if (responseViewer.getInt("newUid") != newUserId)
                throw new FileControllerException("User that was added as " +
                        "viewer was not the user requested");
        } else if (responseViewer.getString("msgType").equals("error")) {
            throw new FileControllerException(responseViewer.getString
                    ("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    public void changeEditorToViewer(FileSystemObject systemObject, int newUserId)
        throws IOException, ClassNotFoundException,
        FileControllerException {
        int fsoid = systemObject.getId();
        int uid = user.getId();
        JSONObject jsonDemote = new JSONObject();
        jsonDemote.put("msgType", "demoteEditor");
        jsonDemote.put("fsoid", fsoid);
        jsonDemote.put("uid", uid);
        jsonDemote.put("demoteUid", newUserId);
        sendJson(jsonDemote, sslSocket);

        JSONObject responseDemote = receiveJson(sslSocket);
        if (responseDemote.getString("msgType").equals("demoteEditorAck")) {
            if (responseDemote.getInt("newUid") != newUserId)
                throw new FileControllerException("User that was demoted to " +
                        "viewer was not the user requested");
        } else if (responseDemote.getString("msgType").equals("error")) {
            throw new FileControllerException(responseDemote.getString
                    ("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    public void removeEditor(FileSystemObject systemObject, int removeUid) throws IOException,
            ClassNotFoundException, FileControllerException,
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeySpecException {
        removePriv(systemObject, removeUid, "editor");
    }

    public void removeViewer(FileSystemObject systemObject, int removeUid) throws IOException,
            ClassNotFoundException, FileControllerException,
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeySpecException{
        removePriv(systemObject, removeUid, "viewer");
    }

    /**
     * Removes privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     */
    private void removePriv(FileSystemObject systemObject, int removeUid,
                            String userType) throws IOException,
            ClassNotFoundException, FileControllerException,
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeySpecException {
        JSONObject request = new JSONObject();
        request.put("msgType","removePriv");
        request.put("fsoid", systemObject.getId());
        request.put("uid", user.getId());
        request.put("removeUid", removeUid);
        request.put("userType", userType);

        sendJson(request, sslSocket);

        JSONObject removeAck = receiveJson(sslSocket);

        if (removeAck.getString("msgType").equals("removePrivAck")) {
            if (removeAck.getInt("removeUid") != removeUid)
                throw new FileControllerException("User that was removed " +
                        "was not the user requested");
        } else if (removeAck.getString("msgType").equals("error")) {
            throw new FileControllerException(removeAck.getString
                    ("message"));
        } else {
            throw new FileControllerException("Received bad response " +
                    "from server");
        }
    }

    public List<String> getFileLogs(int fsoid) throws IOException,
            ClassNotFoundException, FileControllerException {
        JSONObject request = new JSONObject();
        request.put("msgType", "getFileLogs");
        request.put("fsoid", fsoid);
        request.put("uid", user.getId());
        sendJson(request, sslSocket);

        JSONArray response = receiveJsonArray(sslSocket);

        List<String> logs = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            JSONObject logEntry = response.getJSONObject(i);
            if (!logEntry.getString("msgType").equals("getFileLogAck") ||
                    logEntry.getInt("fsoid") != fsoid) {
                throw new FileControllerException("Received bad response " +
                        "from server");
            }
            int uid = logEntry.getInt("uid");
            String username = checkUsername(uid);
            String lastModified = logEntry.getString("lastModified");
            String actionType = logEntry.getString("actionType");
            String status = logEntry.getString("status");

            String logMsg = null;
            if (actionType.equals("ADD_EDITOR") || actionType.equals
                    ("ADD_VIEWER") || actionType.equals("REMOVE_EDITOR") ||
                    actionType.equals("REMOVE_VIEWER")) {
                int newUid = logEntry.getInt("newUid");
                String newUser = checkUsername(newUid);

                switch (status) {
                    case "SUCCESS":
                        switch (actionType) {
                            case "ADD_EDITOR":
                                logMsg = username + " successfully added " + newUser +
                                        " as an editor on " + lastModified;
                                break;
                            case "ADD_VIEWER":
                                logMsg = username + " successfully added " + newUser +
                                        " as an viewer on " + lastModified;
                                break;
                            case "REMOVE_EDITOR":
                                logMsg = username + " successfully removed " + newUser +
                                        " as an editor on " + lastModified;
                                break;
                            case "REMOVE_VIEWER":
                                logMsg = username + " successfully removed " + newUser +
                                        " as a viewer on " + lastModified;
                                break;
                            default:
                                throw new FileControllerException("Received bad response " +
                                        "from server: action type = " + actionType);
                        }
                        break;
                    case "FAILURE":
                        switch (actionType) {
                            case "ADD_EDITOR":
                                logMsg = username + " failed to add " + newUser +
                                        " as an editor on " + lastModified;
                                break;
                            case "ADD_VIEWER":
                                logMsg = username + " failed to add " + newUser +
                                        " as an viewer on " + lastModified;
                                break;
                            case "REMOVE_EDITOR":
                                logMsg = username + " failed to remove " + newUser +
                                        " as an editor on " + lastModified;
                                break;
                            case "REMOVE_VIEWER":
                                logMsg = username + " failed to remove " + newUser +
                                        " as an viewer on " + lastModified;
                                break;
                            default:
                                throw new FileControllerException("Received bad response " +
                                        "from server: action type = " + actionType);
                        }
                        break;
                    default:
                        throw new FileControllerException("Received bad response " +
                                "from server");
                }
            } else {
                if (status.equals("SUCCESS")) {
                    switch (actionType) {
                        case "CREATE_FOLDER":
                            logMsg = username + " created this folder on " +
                                    lastModified;
                            break;
                        case "UPLOAD_FILE":
                            logMsg = username + " uploaded this file on "
                                    + lastModified;
                            break;
                        case "RENAME":
                            logMsg = username + " renamed this file on "
                                    + lastModified;
                            break;
                        case "OVERWRITE":
                            logMsg = username + " overwrote this file on "
                                    + lastModified;
                            break;
                        case "REMOVE_FSO":
                            logMsg = username + " deleted this file on "
                                    + lastModified;
                            break;
                        default:
                            throw new FileControllerException("Received bad response " +

                                    "from server: action type = " + actionType);
                    }
                } else if (status.equals("FAILURE")) {
                    switch (actionType) {
                        case "CREATE_FOLDER":
                            logMsg = username + " failed to create this " +
                                    "folder on " +
                                    lastModified;
                            break;
                        case "UPLOAD_FILE":
                            logMsg = username + " failed to upload this file " +
                                    "on "
                                    + lastModified;
                            break;
                        case "RENAME":
                            logMsg = username + " failed to rename this file " +
                                    "on "
                                    + lastModified;
                            break;
                        case "OVERWRITE":
                            logMsg = username + " failed to overwrite this " +
                                    "file on "
                                    + lastModified;
                            break;
                        case "REMOVE_FSO":
                            logMsg = username + " failed to delete this file on "
                                    + lastModified;
                            break;
                        default:
                            throw new FileControllerException("Received bad response " +
                                    "from server: action type = " + actionType);
                    }
                }
            }
            if (logMsg != null)
                logs.add(logMsg);
        }
        return logs;
    }

    /**
     * Gets username from uid that's locally stored
     * @param uid User id of the user
     * @return Username of the corresponding user
     */
    private String checkUsername(int uid) throws IOException,
            ClassNotFoundException, FileControllerException {
        String username;
        if (uidToUsername.containsKey(uid))
            username = uidToUsername.get(uid);
        else {
            username = getUsername(uid);
            uidToUsername.put(uid, username);
        }
        return username;
    }

    /**
     * Requests for username from server that corresponds to a certain user id
     * @param uid User id of user
     * @return Username of the corresponding user
     */
    private String getUsername(int uid) throws IOException,
            ClassNotFoundException, FileControllerException {
        JSONObject json = new JSONObject();
        json.put("msgType","username");
        json.put("uid", uid);
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("usernameAck"))
            return response.getString("username");
        else if (response.getString("msgType").equals("error"))
            throw new FileControllerException(response.getString("message"));
        else
            throw new FileControllerException("Received bad response from " +
                    "server");
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

    public int getLoggedInUid() {
        return user.getId();
    }

    public class FileControllerException extends Exception {
        FileControllerException(String message) {
            super(message);
        }
    }
}
