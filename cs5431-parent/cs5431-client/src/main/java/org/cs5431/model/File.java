package org.cs5431.model;

import java.sql.Timestamp;

public class File extends FileSystemObject {
    public File(int id, String name, long size,
                Timestamp lastModified, boolean isEditor, boolean isViewer) {
        super(id, name, lastModified, isEditor, isViewer);
        this.size = size;
        this.type = FSOType.FILE;
    }
}

