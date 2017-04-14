package org.cs5431.model;

public class FileLogEntry {
    private int userId;
    public int timestamp;
    private FileActionType action;

    public FileLogEntry(int userId, FileActionType action) {
        this.userId = userId;
        this.action = action;
    }
}
