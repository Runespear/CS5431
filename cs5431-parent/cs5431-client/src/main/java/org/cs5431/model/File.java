package org.cs5431.model;

import java.sql.Timestamp;

public class File extends FileSystemObject {
    //TODO: THIS SHOULD NOT BE A STRING
    private String fileContents;

    //TODO: populate from database instead?
    public File(int id, String name, Folder parentFolder, long size, Timestamp lastModified) {
        super(id, name, parentFolder, lastModified);
        this.size = size;
        this.type = FSOType.FILE;
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
    }

    public String getFileContents() {return fileContents; }


}

