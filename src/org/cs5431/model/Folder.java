package org.cs5431.model;

import java.util.List;

public class Folder extends FileSystemObject {
    private List<FileSystemObject> children;

    //TODO: populated from database instead?
    public Folder(List<FileSystemObject> children) {
        this.children = children;
    }

    public void addChild(FileSystemObject child) {
        this.children.add(child);
        //TODO: database stuff?
    }

    public List<FileSystemObject> getChildren() {
        return children;
    }

    //TODO
}
