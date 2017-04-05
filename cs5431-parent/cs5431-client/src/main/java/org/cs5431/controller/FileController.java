package org.cs5431.controller;

import javafx.concurrent.Task;
import org.cs5431.model.*;
import org.cs5431.model.File;
import org.cs5431.SQL_Connection;
import org.cs5431.Validator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.cs5431.JSON.receiveJson;
import static org.cs5431.JSON.receiveJsonArray;
import static org.cs5431.JSON.sendJson;
import static org.cs5431.model.FileActionType.DOWNLOAD;
import static org.cs5431.model.FileActionType.OVERWRITE;
import static org.cs5431.Constants.DEBUG_MODE;

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
     * Creates new file and uploads it to the server along with its log entry. Adds the file as a child
     * of the parent folder.
     * @param file File that is was returned from the javaFX dialogue box
     * @param parentFolder Folder where the file is to be uploaded
     * @return file created if the user file upload to server was successful; false otherwise
     */
    public File uploadFile(java.io.File file, Folder parentFolder) throws
            IOException, JSONException, NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, ClassNotFoundException, UploadFailException {
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

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128, new SecureRandom());
        SecretKey fileSK = kg.generateKey();
        SecureRandom random = new SecureRandom();
        byte iv[] = new byte[16];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        fso.put("fileIV", Base64.getEncoder().encodeToString(ivSpec.getIV()));

        byte[] encFile = encryptFile(file, fileSK, ivSpec);
        //TODO change to more appropriate format?
        fso.put("file", Base64.getEncoder().encodeToString(encFile));

        random.nextBytes(iv);
        ivSpec = new IvParameterSpec(iv);
        fso.put("fsoNameIV", Base64.getEncoder().encodeToString(ivSpec.getIV()));
        String encFileName = Base64.getEncoder().encodeToString(encryptFileName(name,
                fileSK, ivSpec));
        fso.put("fsoName", encFileName);
        String encFileKey = Base64.getEncoder().encodeToString
                (encFileSecretKey(fileSK, user.getPubKey()));
        fso.put("encSK", encFileKey);
        sendJson(fso, sslSocket);
        JSONObject fsoAck = receiveJson(sslSocket);
        if (fsoAck.get("msgType").equals("uploadAck")) {
            int fileSentid = fsoAck.getInt("fsoid");
            if (DEBUG_MODE) {
                System.out.println(fileSentid);
            }
            if (fileSentid != -1) {
                File fileSent = new File(fileSentid, name, parentFolder, size, lastModified);

                parentFolder.addChild(fileSent);
                fileSent.addPriv(PrivType.EDIT, user.getId());
                if (DEBUG_MODE) {
                    System.out.print(parentFolder.getChildren());
                }
                return fileSent;
            } else {
                throw new UploadFailException("Failed to add file");
            }
        }
        return null;
    }

    public class UploadFailException extends Exception {
        UploadFailException(String message) {
            super(message);
        }
    }

    private byte[] encryptFile(java.io.File file, SecretKey secretKey,
                               IvParameterSpec ivSpec) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException,
            IOException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        FileInputStream inputStream = new FileInputStream(file);
        byte[] filebytes = new byte[inputStream.available()];
        inputStream.read(filebytes);
        inputStream.close();
        return cipher.doFinal(filebytes);
    }

    private byte[] encryptFileName(String fileName, SecretKey secretKey,
                                   IvParameterSpec ivSpec) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(fileName.getBytes());
    }

    private byte[] encFileSecretKey (SecretKey secretKey, PublicKey
            userPubKey) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        Cipher cipher = Cipher.getInstance
                ("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, userPubKey);
        return cipher.doFinal(secretKey.getEncoded());
    }

    private boolean decryptFile(byte[] encFile, String fileName,
                             SecretKey secretKey, IvParameterSpec ivSpec,
                                java.io.File directory)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] fileDec = cipher.doFinal(encFile);
        java.io.File fileToWrite = new java.io.File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(fileToWrite);
        fos.write(fileDec);
        fos.close();
        return true;
    }

    private String decryptFileName(byte[] encFileName, SecretKey fileSK,
                                   IvParameterSpec ivSpec) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, fileSK, ivSpec);
        byte fileName[] = cipher.doFinal(encFileName);
        return new String(fileName);
    }

    private SecretKey decFileSecretKey(byte[] encSK, PrivateKey userPrivKey)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException{
        Cipher cipher = Cipher.getInstance
                ("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, userPrivKey);
        byte SKbytes[] = cipher.doFinal(encSK);
        return new SecretKeySpec(SKbytes, 0, SKbytes.length, "AES");
    }

    /**
     * Creates a new folder and uploads it to the server along with its log entry. Adds the new folder as a
     * child of the parent folder.
     * @param folderName is the name of the folder that is to be created
     * @param parentFolder Folder where the file is to be uploaded
     * @return the folder that is created and uploaded to server successfully; null otherwise
     */
    public Folder createFolder(String folderName, Folder parentFolder) throws
            Exception {
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());
        if (isAcceptableInput(folderName)) {
            JSONObject fso = new JSONObject();
            fso.put("msgType","upload");
            fso.put("uid", user.getId());
            fso.put("parentFolderid", parentFolder.getId());

            fso.put("size", "0");
            fso.put("lastModified", lastModified);
            fso.put("isFile", false);

            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128, new SecureRandom());
            SecretKey fileSK = kg.generateKey();
            SecureRandom random = new SecureRandom();
            byte iv[] = new byte[16];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            fso.put("fsoNameIV", Base64.getEncoder().encodeToString(ivSpec.getIV()));
            String encFileName = Base64.getEncoder().encodeToString
                    (encryptFileName(folderName,fileSK, ivSpec));
            fso.put("fsoName", encFileName);
            String encFileKey = Base64.getEncoder().encodeToString
                    (encFileSecretKey(fileSK, user.getPubKey()));
            fso.put("encSK", encFileKey);

            sendJson(fso, sslSocket);
            JSONObject fsoAck = receiveJson(sslSocket);
            int folderSentId = fsoAck.getInt("fsoid");

            if (folderSentId != -1) {
                Folder folderSent = new Folder(folderSentId, folderName, parentFolder, lastModified);
                folderSent.addPriv(PrivType.EDIT, user.getId()); //TODO: do we even need this
                parentFolder.addChild(folderSent);
                return folderSent;
            }
        }
        return null;
    }

    /**
     * Attempts to change the file contents to the originalFile contents of file. If the user has the permission,
     * the changes are sent to the server along with its log entry.
     * @param originalFile is the file to be overwritten
     * @param file object returned by the javaFX file picker dialogue box
     * @return the file that is modified and uploaded to server successfully; null otherwise.
     */
    public File overwrite(File originalFile, java.io.File file) {
        Task<File> task = new Task<File>() {
            @Override
            protected File call() throws Exception {
            boolean canOverWrite = isAllowed(OVERWRITE, originalFile);
            if (canOverWrite) {
                //originalFile.setFileContents(newFileContent);
                FileLogEntry logEntry = new FileLogEntry(user.getId(), OVERWRITE);
                FileSystemObject fileSent = modifyFSOContents(originalFile.getId(), file, logEntry);
                fileSent.getFileLog().addLogEntry(logEntry);
                return (File) fileSent;
            }
            return null; // to return null or throw exception?
            }
        };
        final File[] ret = new File[1];
        task.setOnSucceeded(t -> ret[0] = task.getValue());
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        return ret[0];
    }

    /**
     * Attempts to rename the file. If the user has the permission, the changes are sent to
     * the server along with its log entry. If rename is successful on the server,
     * the system object on client side is renamed as well.
     * @param systemObject is the file/folder to be renamed
     * @param newName New name of the file/folder
     * @return true if the name of the file/folder is successfully modified; false otherwise
     */
    public boolean rename(FileSystemObject systemObject, String newName)
            throws IOException, ClassNotFoundException{
        JSONObject json = new JSONObject();
        json.put("msgType", "rename");
        json.put("fsoid", systemObject.getId());
        json.put("uid", user.getId());
        json.put("newName", newName);   //TODO encryption
        sendJson(json, sslSocket);
        JSONObject response = receiveJson(sslSocket);
        if (response.getString("msgType").equals("renameAck"))
            return true;
        else    //TODO error handling? e.g. not enough permissions?
            return false;
    }

    /**
     * Gets the file/folder contents from server and decrpyts it.
     * @param fsoId is ID of the file to be downloaded
     * @return true if successful
     */
    public boolean download(int fsoId, String fsoName, java.io.File dir) throws
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
            return decryptFile(fsoBytes, fsoName, fileSK, iv, dir);
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
     * @return true if delete is successful; false otherwise
     */
    public boolean delete(FileSystemObject fso, Folder parentFolder) {
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                //TODO: remove from db
                parentFolder.removeChild(fso);
                return false;
            }
        };
        final Boolean[] ret = new Boolean[1];
        task.setOnSucceeded(t -> ret[0] = task.getValue());
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        return ret[0];
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

    /**
     * Sanitizes the input to ensure that it is not at risk of causing SQL injection
     * @param input raw data that is to be used in the sql query
     * @return true if the input string is safe; false otherwise
     */
    private boolean isAcceptableInput(String input) {
        //TODO move this into Validator and do an actual check
        return true;
    }

    public class FileControllerException extends Exception {
        public FileControllerException(String message) {
            super(message);
        }
    }
}
