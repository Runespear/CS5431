package org.cs5431_client.model;

import java.util.ArrayList;
import java.util.List;

public class Folder extends FileSystemObject {
    private List<FileSystemObject> children;

    //TODO: populated from database instead?

    public void addChild(FileSystemObject child) {
        this.children.add(child);
        //TODO: database stuff?
    }

    public Folder (String name, Folder parentFolder, int ownerId) {
        super(name, parentFolder, ownerId);
        this.type = FSOType.FOLDER;
        this.children = new ArrayList<>();
        this.size = 0;
        //TODO: date modified set by DB timestamp
    }

    public List<FileSystemObject> getChildren() {
        return children;
    }

    public void removeChild(FileSystemObject fso) { this.children.remove(fso); }

    public int getFolderId() {return id; }

    //TODO: update size to be sum of size of children

}
