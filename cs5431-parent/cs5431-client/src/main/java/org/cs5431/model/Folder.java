package org.cs5431.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Folder extends FileSystemObject {
    private List<FileSystemObject> children;

    public void addChild(FileSystemObject child) {
        this.children.add(child);
    }

    public Folder (int id, String name, Timestamp
            lastModified, boolean isEditor, Timestamp keyLastModified) {
        super(id, name, lastModified, isEditor, keyLastModified);
        this.type = FSOType.FOLDER;
        this.children = new ArrayList<>();
        this.size = 0;
    }

    public List<FileSystemObject> getChildren() {
        return children;
    }

    public void removeChild(FileSystemObject fso) { this.children.remove(fso); }
}
