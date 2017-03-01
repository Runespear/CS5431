package org.cs5431_client.model;

import java.util.Date;

public abstract class FileSystemObject {
    private int id;
    private FileSystemObject parentFolder;
    private int ownerId;
    private int editorIds[];
    private int viewerIds[];
    private String name;
    private int size;
    private Date lastModified;
    public FSOType type;

}
