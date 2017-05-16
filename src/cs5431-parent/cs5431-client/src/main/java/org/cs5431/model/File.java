package org.cs5431.model;

import java.sql.Timestamp;

public class File extends FileSystemObject {
    public File(int id, String name, long size,
                Timestamp lastModified, boolean isEditor, Timestamp keyLastModified) {
        super(id, name, lastModified, isEditor, keyLastModified);
        this.size = size;
        this.type = FSOType.FILE;
    }
}

