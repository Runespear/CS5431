package org.cs5431_client.model;

import java.util.ArrayList;
import java.util.List;

public class Folder extends FileSystemObject {
    private List<Integer> children;

    //TODO: populated from database instead?

    public void addChild(Integer child) {
        this.children.add(child);
        //TODO: database stuff?
    }

    public Folder (String name, int ownerId, int parentFolderId, int size) {
        super(name, ownerId, parentFolderId, size);
        this.type = FSOType.FOLDER;
        this.children = new ArrayList<>();
        //TODO: date modified set by DB timestamp
    }

    public List<Integer> getChildren() {
        return children;
    }

    public boolean addChildren(int child) {
        return children.add(child);
    }

    public int getFolderId() {return id; }
    //TODO
}
