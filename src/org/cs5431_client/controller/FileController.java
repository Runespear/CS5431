package org.cs5431_client.controller;

import org.cs5431_client.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.cs5431_client.model.FileActionType.*;

//TODO: should a FileController control all files or just a single file?
public class FileController {
    private User user;
    private FileLogController logController;
    private String serverIP;

    public FileController(User user, FileLogController logController, String serverIP) {
        this.user = user;
        this.logController = logController;
        this.serverIP = serverIP;
    }

    /**
     * Returns true if the user has the permission to perform the action.
     * @param action FileActionType that the user intends to perform
     * @return true if the user has the permission; false otherwise
     */
    private boolean isAllowed(FileActionType action, FileSystemObject fileObject) {
        //TODO: get permissions of the fileObject, iterate through

        return false;
    }

    /**
     * Creates new file and uploads it to the server along with its log entry.
     * @param file File that is was returned from the javaFX dialogue box
     * @param parentFolder Folder where the file is to be uploaded
     * @return file created if the user file upload to server was successful; false otherwise
     */
    public File uploadFile(java.io.File file, Folder parentFolder){
        String name = file.getName();
        File dbFile = new File("");
        boolean canUpload = isAllowed(UPLOAD_FILE, parentFolder);
        if (canUpload) {
            //TODO: log first?? in case file sent but log not successful
            FileLogEntry logEntry = new FileLogEntry(user.getId(), UPLOAD_FILE);
            FileSystemObject fileSent = sendFSOToServer(dbFile, logEntry);
            return (File) fileSent;
        }
        return null;
    }

    /**
     * Creates a new folder and uploads it to the server along with its log entry.
     * @param folderName is the name of the folder that is to be created
     * @param parentFolder Folder where the file is to be uploaded
     * @return the folder that is created and uploaded to server successfully; null otherwise
     */
    public Folder createFolder(String folderName, Folder parentFolder) {

        boolean canCreateFolder = isAllowed(CREATE_FOLDER, parentFolder);
        if (canCreateFolder) {
            if (isAcceptableName(folderName)) {
                Folder newFolder = new Folder(new ArrayList<FileSystemObject>());
                FileLogEntry logEntry = new FileLogEntry(user.getId(), CREATE_FOLDER);
                FileSystemObject folderSent = sendFSOToServer(newFolder, logEntry);
                return (Folder) folderSent;
            }
            else {
                //TODO: unacceptable name
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

        String newFileContent = ""; // TODO: how to get file content from java.io.File object

        boolean canOverWrite = isAllowed(OVERWRITE, originalFile);
        if (canOverWrite) {
            originalFile.setFileContents(newFileContent);
            FileLogEntry logEntry = new FileLogEntry(user.getId(), OVERWRITE);
            FileSystemObject fileSent = modifyFSO(originalFile, logEntry);
            //TODO are we going to send in an entire new file?? or function to modify
            return (File) fileSent;
        }
        return null; //TODO: to return null or throw exception?
    }

    /**
     * Attempts to rename the file. If the user has the permission, the changes are sent to
     * the server along with its log entry.
     * @param systemObject is the file/folder to be renamed
     * @param newName New name of the file/folder
     * @return true if the name of the file/folder is successfully modified; false otherwise
     */
    public boolean rename(FileSystemObject systemObject, String path, String newName) {

        boolean canRename = isAllowed(RENAME, systemObject);
        if (canRename) {
            if (isAcceptableName(newName)) {
                FileLogEntry logEntry = new FileLogEntry(user.getId(), RENAME);
                FileSystemObject fileSent = modifyFSO(systemObject, logEntry);
                return (fileSent != null);
            }
            else {
                //TODO: unacceptable name
            }
        }
        return false;
    }

    /**
     * Gets the file from server and decrpyts it.
     * @param fileId is ID of the file to be downloaded
     * @return file if download is successful; false otherwise
     */
    public File download(int fileId) {
        //TODO: can we download folders as well?
        //TODO: get file from server or is it already somewhere local since we have the name?
        //TODO: decrypt the file
        return null;
    }

    /**
     * Deletes the file from server entirely. No one is able to access it anymore.
     * @param fileId is ID of the file to be deleted
     * @return true if delete is successful; false otherwise
     */
    public boolean delete(int fileId) {
        //TODO
        return false;
    }

    /**
     * Adds privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public void addPriv(FileSystemObject systemObject, PrivType priv) {
        boolean canAddPriv = isAllowed(ADD_PRIV, systemObject);
        if (canAddPriv) {
            FileLogEntry logEntry = new FileLogEntry(user.getId(), ADD_PRIV);
            FileSystemObject fsoSent = modifyFSO(systemObject, logEntry);
            return (fsoSent != null);
        }
    }

    /**
     * Removes privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public void removePriv(FileSystemObject systemObject, PrivType priv) {
        //TODO
    }

    public void rollback() {
        //TODO
    }

    /**
     * Sends the file/folder to the server with the serverIP attribute of the fileController.
     * @param systemObject file/folder to be sent to the server
     * @return the file/folder that is uploaded to server if successful; null otherwise
     */
    private FileSystemObject sendFSOToServer(FileSystemObject systemObject, FileLogEntry logEntry) {
        return null;
    }

    private FileSystemObject modifyFSO(FileSystemObject systemObject, FileLogEntry logEntry) {
        return null;
    }

    private boolean isAcceptableName(String name) {
        return false;
    }
}
