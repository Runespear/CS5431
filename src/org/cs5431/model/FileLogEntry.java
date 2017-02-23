package org.cs5431.model;

public class FileLogEntry {
    //TODO: should these be private?
    public int userId;
    public int timestamp;
    public String action; //TODO: make this enum instead of string?
    public String rollbackFilePtr; //TODO: this should not be a string
}
