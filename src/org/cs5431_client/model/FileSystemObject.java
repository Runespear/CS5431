package org.cs5431_client.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class FileSystemObject {
    protected int id;
    protected Folder parentFolder;
    //protected int ownerId;
    protected List<Integer> editorIds;
    protected List<Integer> viewerIds;
    protected String name;
    protected long size;
    protected Date lastModified;
    protected FSOType type;

    public FileSystemObject (String name, Folder parentFolder, int ownerId) {
        this.name = name;
        this.parentFolder = parentFolder;
        this.viewerIds = new ArrayList<>();
        this.editorIds = new ArrayList<>();
        this.editorIds.add(ownerId);
        //TODO: date modified set by DB timestamp
        //TODO: remove following line that's currently used for display purposes
        lastModified = new Date();
    }


    public List<Integer> getEditors() {return this.editorIds; }

    public List<Integer> getViewers() {return this.viewerIds; }

    public String getFileName() {return name; }

    public long getFileSize() {return size; }

    public Date getLastModified() {return lastModified; }

    public int getId() {return id;}

    public void rename(String newName) { this.name = newName; }
}
