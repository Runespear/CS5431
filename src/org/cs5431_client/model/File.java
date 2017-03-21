package org.cs5431_client.model;

import java.sql.Timestamp;

public class File extends FileSystemObject {
    //TODO: THIS SHOULD NOT BE A STRING
    private String fileContents;

    //TODO: populate from database instead?
    public File(int id, String name, Folder parentFolder, int ownerId, long size, Timestamp lastModified) {
        super(id, name, parentFolder, ownerId, lastModified);
        this.size = size;
        this.type = FSOType.FILE;
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
    }

    public String getFileContents() {return fileContents; }


}

