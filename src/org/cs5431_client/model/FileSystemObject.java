package org.cs5431_client.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class FileSystemObject {
    protected int id;
    protected int parentFolderId;
    protected int ownerId;
    protected List<Integer> editorIds;
    protected List<Integer> viewerIds;
    protected String name;
    protected int size;
    protected Date lastModified;
    protected FSOType type;

    public FileSystemObject (String name, int ownerId, int parentFolderId, int size) {
        this.name = name;
        this.ownerId = ownerId;
        this.parentFolderId = parentFolderId;
        this.viewerIds = new ArrayList<>();
        this.editorIds = new ArrayList<>();
        this.editorIds.add(ownerId);
        this.size = size;
        //TODO: date modified set by DB timestamp
        //TODO: remove following line that's currently used for display purposes
        lastModified = new Date();
    }


    public List<Integer> getEditors() {return this.editorIds; }

    public List<Integer> getViewers() {return this.viewerIds; }

    public String getFileName() {return name; }

    public int getFileSize() {return size; }

    public Date getLastModified() {return lastModified; }

    public int getId() {return id;}
}
