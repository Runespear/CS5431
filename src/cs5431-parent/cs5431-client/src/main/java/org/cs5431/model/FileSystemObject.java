package org.cs5431.model;

import java.sql.Timestamp;

public abstract class FileSystemObject {
    protected int id;
    private boolean isEditor; //is logged in user an editor
    private String name;
    protected long size;
    private Timestamp lastModified;
    private Timestamp keyLastUpdated;
    FSOType type;

    FileSystemObject(int id, String name, Timestamp lastModified, boolean
            isEditor, Timestamp keyLastUpdated) {
        this.id = id;
        this.name = name;
        if (lastModified != null)
            this.lastModified = new Timestamp(lastModified.getTime());
        else
            this.lastModified = null;
        this.isEditor = isEditor;
        this.keyLastUpdated = keyLastUpdated;
    }

    public String getFileName() {return name; }

    public long getFileSize() {return size; }

    public Timestamp getLastModified() {
        if (lastModified == null)
            return null;        return new Timestamp(lastModified.getTime()); }

    public Timestamp getKeyLastUpdated() {
        if (keyLastUpdated == null)
            return null;
        return new Timestamp(keyLastUpdated.getTime());
    }

    public int getId() {return id;}

    public void rename(String newName) { this.name = newName; }

    public boolean isEditor() {return isEditor;}
}
