package org.cs5431_client.model;

import java.util.List;

public class FileLog {
    private int fileId;
    private List<FileLogEntry> logEntries;

    public void addLogEntry(FileLogEntry logEntry){this.logEntries.add(logEntry); }

    public List<FileLogEntry> getLogEntries(){return this.logEntries; }
}