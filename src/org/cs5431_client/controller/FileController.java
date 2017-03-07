package org.cs5431_client.controller;

import org.cs5431_client.model.*;
import org.cs5431_client.util.client_tcp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;

import static org.cs5431_client.model.FileActionType.*;

//TODO: should a FileController control all files or just a single file?
public class FileController {
    private User user;
    private FileLogController logController;
    private String serverIP;
    private String serverPort;

    public FileController(User user, String serverIP, String serverPort) {
        this.user = user;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Returns true if the user has the permission to perform the action.
     * @param action FileActionType that the user intends to perform
     * @return true if the user has the permission; false otherwise
     */
    public boolean isAllowed(FileActionType action, FileSystemObject fso) {
        //TODO: get permissions of the fileObject, iterate through
        List<Integer> usersWithPermission = fso.getEditors();
        if (action == DOWNLOAD) {
            usersWithPermission.addAll(fso.getViewers());
        }
        System.out.print("user id:" + user.getId());
        return (usersWithPermission.contains(user.getId()));
    }

    /**
     * Creates new file and uploads it to the server along with its log entry. Adds the file as a child
     * of the parent folder.
     * @param file File that is was returned from the javaFX dialogue box
     * @param parentFolder Folder where the file is to be uploaded
     * @return file created if the user file upload to server was successful; false otherwise
     */
    public File uploadFile(java.io.File file, Folder parentFolder) throws IOException {
        String name = file.getName();
        //TODO get file contents and size from java.io.file
        FileReader reader = new FileReader(file);

        File dbFile = new File(name, parentFolder, user.getId(),0, "");
        boolean canUpload = isAllowed(UPLOAD_FILE, parentFolder);
        if (canUpload) {
            FileLogEntry logEntry = new FileLogEntry(user.getId(), UPLOAD_FILE);
            FileSystemObject fileSent = sendFSOToServer(dbFile, logEntry);
            if (fileSent != null) {
                parentFolder.addChild(fileSent);
                return (File) fileSent;
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
    public Folder createFolder(String folderName, Folder parentFolder) {

        boolean canCreateFolder = isAllowed(CREATE_FOLDER, parentFolder);
        if (canCreateFolder) {
            if (isAcceptableInput(folderName)) {
                Folder newFolder = new Folder(folderName, user.getUserParentFolder(), user.getId());
                FileLogEntry logEntry = new FileLogEntry(user.getId(), CREATE_FOLDER);
                FileSystemObject folderSent = sendFSOToServer(newFolder, logEntry);
                if (folderSent != null) {
                    parentFolder.addChild(folderSent);
                    return (Folder) folderSent;
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
     * the server along with its log entry. If rename is successful on the server,
     * the system object on client side is renamed as well.
     * @param systemObject is the file/folder to be renamed
     * @param newName New name of the file/folder
     * @return true if the name of the file/folder is successfully modified; false otherwise
     */
    public boolean rename(FileSystemObject systemObject, String newName) {

        boolean canRename = isAllowed(RENAME, systemObject);
        if (canRename && isAcceptableInput(newName)) {
            FileLogEntry logEntry = new FileLogEntry(user.getId(), RENAME);
            FileSystemObject fileSent = modifyFSO(systemObject, logEntry);
            if (fileSent != null) {
                systemObject.rename(newName);
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
    public boolean addPriv(FileSystemObject systemObject, PrivType priv) {
        boolean canAddPriv = isAllowed(ADD_PRIV, systemObject);
        if (canAddPriv) {
            FileLogEntry logEntry = new FileLogEntry(user.getId(), ADD_PRIV);
            FileSystemObject fsoSent = modifyFSO(systemObject, logEntry);
            return (fsoSent != null);
        }
        return false;
    }

    /**
     * Removes privileges the file/folder and sends the changes to the server.
     * @param systemObject Privileges are added to this file/folder.
     * @return true if privilege was added successfully; false otherwise.
     */
    public boolean removePriv(FileSystemObject systemObject, PrivType priv) {
        boolean canRemovePriv = isAllowed(ADD_PRIV, systemObject);
        if (canRemovePriv) {
            FileLogEntry logEntry = new FileLogEntry(user.getId(), ADD_PRIV);
            FileSystemObject fsoSent = modifyFSO(systemObject, logEntry);
            return (fsoSent != null);
        }
        return false;
    }

    public void rollback(int rollbackToThisfileId) {
        //TODO: how???
    }

    /**
     * Sends the file/folder to the server with the serverIP attribute of the fileController.
     * @param systemObject file/folder to be sent to the server
     * @return the file/folder that is uploaded to server if successful; null otherwise
     */
    private FileSystemObject sendFSOToServer(FileSystemObject systemObject, FileLogEntry logEntry) {
        return null;
    }

    //might actually need to repeat this function depending on the api we have to send changes to the object
    //or send the entire file again since we want to rollback
    private FileSystemObject modifyFSO(FileSystemObject systemObject, FileLogEntry logEntry) {
        return null;
    }

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
