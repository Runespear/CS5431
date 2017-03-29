package org.cs5431_client.model;

import java.sql.Timestamp;
import java.util.*;

public abstract class FileSystemObject {
    protected int id;
    protected Folder parentFolder;
    //protected int ownerId;
    protected List<Integer> editorIds;
    protected List<Integer> viewerIds;
    protected String name;
    protected long size;
    protected Timestamp lastModified;
    protected FSOType type;
    protected  FileLog fileLog;

    //ownerId only used when creating new file; otherwise not saved
    public FileSystemObject (int id, String name, Folder parentFolder, Timestamp lastModified) {
        this.id = id;
        this.name = name;
        this.parentFolder = parentFolder;
        this.viewerIds = new ArrayList<>();
        this.editorIds = new ArrayList<>();
        this.fileLog = new FileLog();
        //TODO: date modified set by DB timestamp
        //TODO: remove following line that's currently used for display purposes
        this.lastModified = new Timestamp(lastModified.getTime());
    }

    public List<Integer> getEditors() {return this.editorIds; }

    public List<Integer> getViewers() {return this.viewerIds; }

    public String getFileName() {return name; }

    public long getFileSize() {return size; }

    public Timestamp getLastModified() {return new Timestamp(lastModified
            .getTime()); }

    public int getId() {return id;}

    public void rename(String newName) { this.name = newName; }

    public void removePriv(PrivType priv, int userId) {
        if (priv == PrivType.EDIT) {
            editorIds.remove(userId);
        } else {
            viewerIds.remove(userId);
        }
    }

    public void addPriv(PrivType priv, int userId) {
        if (priv == PrivType.EDIT) {
            editorIds.add(userId);
        } else {
            viewerIds.add(userId);
        }
    }

    public FileLog getFileLog() {return this.fileLog; }
}
