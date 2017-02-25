package org.cs5431.controller;

import org.cs5431.model.FSOType;
import org.cs5431.model.User;

//TODO: should a FileController control all files or just a single file?
public class FileController {
    private User user;
    private FileLogController logController;

    public FileController(User user, FileLogController logController) {
        this.user = user;
        this.logController = logController;
    }

    private boolean isAllowed() {
        return false;
    }

    public void uploadFile(){

    }

    /*
     *
     */
    public void createFolder(FSOType type, String filename, String path, String file) {
        //TODO: fill in args
        //TODO: we probably don't want to pass the path as a string?
    }

    /*
     * Overwrites the file/folder specified by path with newfile
     * Returns true on success, false otherwise
     */
    public void overwrite(FSOType type, String path, String newfile) {
        //TODO

        //TODO: check if user is allowed to do this
        //TODO: the thing
        //TODO: log it
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
