package org.cs5431.model;

import java.sql.Timestamp;
import java.util.*;

public abstract class FileSystemObject {
    protected int id;
    private boolean isEditor; //is logged in user an editor
    private boolean isViewer; //is logged in user a viewer
    private String name;
    protected long size;
    private Timestamp lastModified;
    FSOType type;

    FileSystemObject(int id, String name, Timestamp lastModified, boolean
            isEditor,
                     boolean isViewer) {
        this.id = id;
        this.name = name;
        if (lastModified != null)
            this.lastModified = new Timestamp(lastModified.getTime());
        else
            this.lastModified = null;
        this.isEditor = isEditor;
        this.isViewer = isViewer;
    }

    public String getFileName() {return name; }

    public long getFileSize() {return size; }

    public Timestamp getLastModified() {
        if (lastModified == null)
            return null;        return new Timestamp(lastModified.getTime()); }

    public int getId() {return id;}

    public void rename(String newName) { this.name = newName; }

    public boolean isEditor() {return isEditor;}

    public boolean isViewer() {return isViewer;}

    public void setEditor(boolean isEditor) {this.isEditor = isEditor;}

    public void setViewer(boolean isViewer) {this.isViewer = isViewer;}
}
