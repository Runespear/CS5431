package org.cs5431_client.controller;

import org.cs5431_client.model.*;
import org.cs5431_client.util.SQL_Connection;
import org.cs5431_client.util.Validator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.cs5431_client.model.FileActionType.DOWNLOAD;
import static org.cs5431_client.model.FileActionType.OVERWRITE;

//TODO: should a FileController control all files or just a single file?
public class FileController {
    private User user;
    private String serverIP;
    private String serverPort;
    private SQL_Connection sql_connection; //TODO: to pass or create new one each time

    public FileController(User user, String serverIP, String serverPort) {
        this.user = user;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.sql_connection = new SQL_Connection("localhost", 3306);
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
            BadPaddingException {
        String name = file.getName();
        long size = file.length();
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        JSONObject fso = new JSONObject();
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
        String encFileName = Base64.getEncoder().encodeToString(encryptFileName(name,
                fileSK, ivSpec));
        fso.put("fsoName", encFileName);
        String encFileKey = Base64.getEncoder().encodeToString
                (encFileSecretKey(fileSK, user.getPubKey()));
        fso.put("encSK", encFileKey);

        int fileSentid = sendFSO(fso, file);
        if (fileSentid != -1) {
            File fileSent = new File(fileSentid, name, parentFolder, size, lastModified);
            parentFolder.addChild(fileSent);
            fileSent.addPriv(PrivType.EDIT, user.getId());
            System.out.print(parentFolder.getChildren());
            return fileSent;
        }
        return null;
    }

    private byte[] encryptFile(java.io.File file, SecretKey secretKey,
                               IvParameterSpec ivSpec) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        //TODO encrypt the file using cipher
        //maybe something like http://www.itcsolutions.eu/2011/08/24/how-to-encrypt-decrypt-files-in-java-with-aes-in-cbc-mode-using-bouncy-castle-api-and-netbeans-or-eclipse/
        return null;
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

    private File decryptFile(byte[] encFile, String fileName,
                             SecretKey secretKey, IvParameterSpec ivSpec,
                             Timestamp dateModified, int size)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        //TODO decrypt the file using cipher
        return null;
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
    public Folder createFolder(String folderName, Folder parentFolder) throws JSONException {

        Timestamp lastModified = new Timestamp(System.currentTimeMillis());
        if (isAcceptableInput(folderName)) {
            JSONObject fso = new JSONObject();
            fso.put("uid", user.getId());
            fso.put("parentFolderid", parentFolder.getId());
            fso.put("fsoName", folderName);
            fso.put("size", "0");
            fso.put("lastModified", lastModified);
            fso.put("isFile", false);

            //TODO: create IV for name, file, enc file and name, generate sk

            int folderSentId = sendFSO(fso, null);
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

    /**
     * Attempts to rename the file. If the user has the permission, the changes are sent to
     * the server along with its log entry. If rename is successful on the server,
     * the system object on client side is renamed as well.
     * @param systemObject is the file/folder to be renamed
     * @param newName New name of the file/folder
     * @return true if the name of the file/folder is successfully modified; false otherwise
     */
    public boolean rename(FileSystemObject systemObject, String newName) {
        if (!Validator.validFileName(newName))
            return false;

        int edited = sql_connection.renameFso(systemObject.getId(), user.getId(), newName);
        if (edited != -1) {
            return true;
        }
        return false;
    }

    /**
     * Gets the file/folder contents from server and decrpyts it.
     * @param fsoId is ID of the file to be downloaded
     * @return file if download is successful; false otherwise
     */
    public File download(int fsoId) throws
        IOException, JSONException, NoSuchAlgorithmException,
        NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException, FileControllerException {
        JSONObject fileReq = new JSONObject();
        fileReq.put("messageType", "download");
        fileReq.put("fsoid", fsoId);
        fileReq.put("uid", user.getId());

        //TODO send fileReq here

        //TODO change fileAck to get from socket
        JSONObject fileAck = new JSONObject();
        if (fileAck.getString("messageType").equals("downloadAck")) {
            int fsoID = fileAck.getInt("fsoid");
            String ivString = fileAck.getString("fileIV");
            IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder()
                    .decode(ivString));
            byte encFileSKbytes[] = Base64.getDecoder()
                    .decode(fileAck.getString("encFileSK"));
            SecretKey fileSK = decFileSecretKey(encFileSKbytes, user.getPrivKey());
            byte fsoNamebytes[] = Base64.getDecoder().decode(fileAck
                    .getString("fsoName"));
            String fsoName = decryptFileName(fsoNamebytes, fileSK, iv);
            //TODO: ruixin, should the below be coerced from string
            //or does it need to be decoded
            Timestamp dateModified = (Timestamp) fileAck.get("dateModified");
            int size = fileAck.getInt("size");
            byte fsoBytes[] = Base64.getDecoder().decode(fileAck
                    .getString("encFile"));
            //TODO: what should we do with dateModified and size?
            return decryptFile(fsoBytes, fsoName, fileSK, iv,
                    dateModified, size);
        } else if (fileAck.getString("messageType").equals("error")) {
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
    public List<FileSystemObject> getChildren(Folder parentFolder) {
        int parentFolderid = parentFolder.getId();
        ArrayList<FileSystemObject> children = new ArrayList<>();
        JSONObject json = new JSONObject();
        json.put("msgType", "getChildren");
        json.put("fsoid", parentFolderid);
        json.put("uid", user.getId());
        JSONArray jsonChildren = sql_connection.getChildren(json);
        for (int i=0; i<jsonChildren.length(); i++) {
            JSONObject c = jsonChildren.getJSONObject(i);
            try {
                int id = c.getInt("id");
                String name = c.getString("name");
                String size = c.getString("size");
                Long longSize = Long.valueOf(size);
                Timestamp lastModified = (Timestamp) c.get("lastModified");
                String type = c.getString("FSOType");
                FileSystemObject child;
                if (type == "FOLDER") {
                    child = new Folder(id, name, parentFolder, lastModified);
                } else {
                    child = new File(id, name, parentFolder, longSize, lastModified);
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
        //TODO: remove from db
        parentFolder.removeChild(fso);
        return false;
    }

    /**
     * Adds privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public boolean addPriv(FileSystemObject systemObject, int userId, PrivType priv) {
        int newUser;
        JSONObject json = new JSONObject();
        json.put("msgType", "addPriv");
        json.put("fsoid", systemObject.getId());
        json.put("uid", user.getId());
        json.put("newUid", userId);
        if (priv == PrivType.EDIT) {
            newUser = sql_connection.addEditPriv(json);
        } else {
            newUser = sql_connection.addViewPriv(json);
        }
        if (newUser != -1) {
            systemObject.addPriv(priv, userId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public boolean removePriv(FileSystemObject systemObject, int userId, PrivType priv) {
        int rmUser;
        if (priv == PrivType.EDIT) {
            rmUser = sql_connection.removeEditPriv(systemObject.getId(), user.getId(), userId);
        } else {
            rmUser = sql_connection.removeViewPriv(systemObject.getId(), user.getId(), userId);
        }
        if (rmUser != -1) {
            systemObject.removePriv(priv, userId);
            return true;
        } else {
            return false;
        }
    }

    public void rollback(int rollbackToThisfileId) {
        //TODO: how???
    }

    /**
     * Sends the file/folder to the server with the serverIP attribute of the fileController.
     * @param file File to be sent to the server
     * @param fso Refers to the json of the file/folder to be sent to the db
     * @return the id of the file/folder that is uploaded to server if successful; null otherwise
     */
    private int sendFSO(JSONObject fso, java.io.File file) {
        int fsoid = sql_connection.createFso(fso);
        return fsoid;

        //msgType = “upload”, uid, parentFolderid, size, lastModified, isFile, fileIV, fsoNameIV, file, fsoName, encSK

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
        return true;
    }

    public class FileControllerException extends Exception {
        public FileControllerException(String message) {
            super(message);
        }
    }
}
