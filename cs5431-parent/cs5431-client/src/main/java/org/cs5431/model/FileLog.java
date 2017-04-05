package org.cs5431.model;

import java.util.ArrayList;
import java.util.List;

public class FileLog {
    private int fileId;
    private List<FileLogEntry> logEntries;

    public FileLog() {
        logEntries = new ArrayList<>();
    }

    public void addLogEntry(FileLogEntry logEntry){this.logEntries.add(logEntry); }

    public List<FileLogEntry> getLogEntries(){return this.logEntries; }
}