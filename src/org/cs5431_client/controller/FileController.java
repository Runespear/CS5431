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
     * Creates new file and uploads it to the server
     * @param file File that is was returned from the javaFX dialogue box
     * @param parentFolder Folder where the file is to be uploaded
     * @return file created if the user file upload to server was successful; false otherwise
     */
    public File uploadFile(java.io.File file, Folder parentFolder){
        String name = file.getName();
        File dbFile = new File("");
        boolean canUpload = isAllowed(UPLOAD_FILE, parentFolder);
        if (canUpload) {
            //TODO: upload dbFile to server
            return dbFile;
        }

        return null;
    }

    /**
     * Creates a new folder and uploads it to the server
     * @param folderName is the name of the folder that is to be created
     * @param parentFolder Folder where the file is to be uploaded
     * @return the folder is created and uploaded to server successfully; null otherwise
     */
    public Folder createFolder(String folderName, Folder parentFolder) {

        boolean canCreateFolder = isAllowed(CREATE_FOLDER, parentFolder);
        if (canCreateFolder) {
            Folder newFolder = new Folder(new ArrayList<FileSystemObject>());
            //TODO: send to server
            return newFolder;
        }

        return null;
    }

    /**
     * Attempts to change the file contents to the originalFile contents of file. If the user has the permission,
     * the changes are sent to the server. If successfully received by server, a log entry is created and sent to
     * the server.
     * @param originalFile is the file to be overwritten
     * @param parentFolder Folder where the file is to be uploaded
     * @param file object returned by the javaFX file picker dialogue box
     * @return the file that is modified and uploaded to server successfully; null otherwise
     */
    public File overwrite(File originalFile, Folder parentFolder, java.io.File file) {

        String newFileContent = ""; // TODO: how to get file content from java.io.File object

        boolean canOverWrite = isAllowed(OVERWRITE, originalFile);
        if (canOverWrite) {
            originalFile.setFileContents(newFileContent);
            //TODO: send modification to server

            FileLogEntry logEntry = new FileLogEntry(user.getId(), OVERWRITE);
            //TODO: send log to server

            return originalFile;
        }
        return null; //TODO: to return null or throw exception?
    }

    /*
     * Renames the file/folder specified by path to newname
     * Returns true on success, false otherwise
     */
    public boolean rename(FSOType type, String path, String newname) {
        //TODO
        return false;
    }

    public void download() {
        //TODO
    }

    public void delete() {
        //TODO
    }

    public void addPriv() {
        //TODO
    }

    public void removePriv() {
        //TODO
    }

    public void rollback() {
        //TODO
    }
}
