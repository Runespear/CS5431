package org.cs5431_client.model;

public class FileLogEntry {
    //TODO: should these be private?
    public int userId;
    public int timestamp;
    public FileActionType action;
    public String rollbackFilePath; //TODO: this should not be a string

    public FileLogEntry(int userId, FileActionType action) {
        this.userId = userId;
        this.action = action;
    }
}
