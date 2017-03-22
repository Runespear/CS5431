package org.cs5431_client.controller;

import org.cs5431_client.model.*;
import org.cs5431_client.util.SQL_Connection;
import org.cs5431_client.util.Validator;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.cs5431_client.model.FileActionType.*;

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
    public File uploadFile(java.io.File file, Folder parentFolder) throws IOException, JSONException {
        String name = file.getName();
        long size = file.length();
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        boolean canUpload = isAllowed(UPLOAD_FILE, parentFolder);
        if (canUpload) {
            JSONObject fso = new JSONObject();
            fso.put("uid", user.getId());
            fso.put("parentFolderid", parentFolder.getId());
            fso.put("fsoName", name);
            fso.put("size", String.valueOf(size));
            fso.put("lastModified", lastModified);
            fso.put("isFile", true);
            int fileSentid = sendFSO(fso, file);
            if (fileSentid != -1) {
                File fileSent = new File(fileSentid, name, parentFolder, user.getId(), size, lastModified);
                parentFolder.addChild(fileSent);
                System.out.print(parentFolder.getChildren());
                return fileSent;
            }
        }
        return null;
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

        boolean canCreateFolder = isAllowed(CREATE_FOLDER, parentFolder);
        if (canCreateFolder) {
            if (isAcceptableInput(folderName)) {
                JSONObject fso = new JSONObject();
                fso.put("uid", user.getId());
                fso.put("parentFolderid", parentFolder.getId());
                fso.put("fsoName", folderName);
                fso.put("size", "0");
                fso.put("lastModified", lastModified);
                fso.put("isFile", false);
                int folderSentId = sendFSO(fso, null);
                if (folderSentId != -1) {
                    Folder folderSent = new Folder(folderSentId, folderName, parentFolder, user.getId(), lastModified);
                    parentFolder.addChild(folderSent);
                    return folderSent;
                }
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

        boolean canRename = isAllowed(RENAME, systemObject);
        if (canRename && isAcceptableInput(newName)) {
            FileLogEntry logEntry = new FileLogEntry(user.getId(), RENAME);
            FileSystemObject fileSent = modifyFSOName(systemObject.getId(), newName, logEntry);
            if (fileSent != null) {
                systemObject.rename(newName);
                fileSent.getFileLog().addLogEntry(logEntry);
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the file/folder contents from server and decrpyts it.
     * @param fsoId is ID of the file to be downloaded
     * @return file if download is successful; false otherwise
     */
    public File download(int fsoId) {
        FileSystemObject file = getFSO(fsoId);
        //TODO: decrypt the file
        return (File) file;
    }

    /**
     * Gets all children of the folderId
     * @param folderId downloads the children of folderId
     * @return List of fso if download is successful; false null
     */
    public List<FileSystemObject> downloadFolder(int folderId) {
        //TODO: get from server all children
        return null;
    }

    public List<FileSystemObject> getChildren(Folder parentFolder) {
        int parentFolderid = parentFolder.getId();
        ArrayList<FileSystemObject> children = new ArrayList<>();
        ArrayList<JSONObject> jsonChildren = sql_connection.getChildren(parentFolderid, user.getId());
        for (JSONObject c : jsonChildren) {
            try {
                int id = c.getInt("id");
                String name = c.getString("name");
                String size = c.getString("size");
                Long longSize = Long.valueOf(size);
                Timestamp lastModified = (Timestamp) c.get("lastModified");
                String type = c.getString("FSOType");
                FileSystemObject child;
                if (type == "FOLDER") {
                    child = new Folder(id, name, parentFolder, 0, lastModified);
                } else {
                    child = new File(id, name, parentFolder, 0, longSize, lastModified);
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
        boolean canAddPriv = isAllowed(ADD_PRIV, systemObject);
        if (canAddPriv) {
            FileLogEntry logEntry = new FileLogEntry(userId, ADD_PRIV);
            FileSystemObject fsoSent = addFSOPriv(systemObject.getId(), userId, priv, logEntry);
            if (fsoSent != null) {
                systemObject.addPriv(priv, userId);
                fsoSent.getFileLog().addLogEntry(logEntry);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public boolean removePriv(FileSystemObject systemObject, int userId, PrivType priv) {
        boolean canRemovePriv = isAllowed(ADD_PRIV, systemObject);
        if (canRemovePriv) {
            FileLogEntry logEntry = new FileLogEntry(userId, ADD_PRIV);
            FileSystemObject fsoSent = removeFSOPriv(systemObject.getId(), userId, priv, logEntry);
            if (fsoSent != null) {
                systemObject.removePriv(priv, userId);
                fsoSent.getFileLog().addLogEntry(logEntry);
                return true;
            }
        }
        return false;
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
        int fsoid = sql_connection.createFso(fso, "");
        return fsoid;
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
     * Retrieves file/folder associated with the fsoId
     * @param fsoId ID that is used to query the database
     * @return the corresponding file/folder that had been uploaded to server if successful; null otherwise
     */
    private FileSystemObject getFSO(int fsoId) {
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
}
