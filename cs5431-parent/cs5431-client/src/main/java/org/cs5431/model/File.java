package org.cs5431.model;

import java.sql.Timestamp;

public class File extends FileSystemObject {
    public File(int id, String name, Folder parentFolder, long size, Timestamp lastModified) {
        super(id, name, parentFolder, lastModified);
        this.size = size;
        this.type = FSOType.FILE;
    }
}

