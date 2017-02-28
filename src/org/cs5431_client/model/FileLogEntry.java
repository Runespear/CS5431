package org.cs5431_client.model;

public class FileLogEntry {
    //TODO: should these be private?
    public int userId;
    public int timestamp;
    public FileActionType action; //TODO: make this enum instead of string?
    public String rollbackFilePath; //TODO: this should not be a string
}
