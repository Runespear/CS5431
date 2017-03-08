package org.cs5431_client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

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

    //TODO: update size to be sum of size of children
    public void addPriv(PrivType priv, int userId) {
        if (priv == PrivType.EDIT) {
            editorIds.add(userId);
                PriorityQueue<FileSystemObject> fsoToAddPriv = new PriorityQueue<>();
                for (int i = 0; i < children.size(); i++) {
                    fsoToAddPriv.add(children.get(i));
                }
                while (!fsoToAddPriv.isEmpty()) {
                    FileSystemObject fso = fsoToAddPriv.poll();
                    this.editorIds.add(fso.getId());
                    if (fso.type == FSOType.FOLDER) {
                        for (int i = 0; i < children.size(); i++) {
                            fsoToAddPriv.add(children.get(i));
                        }
                    }
                }
        } else {
            viewerIds.add(userId);
            PriorityQueue<FileSystemObject> fsoToAddPriv = new PriorityQueue<>();
            for (int i = 0; i < children.size(); i++) {
                fsoToAddPriv.add(children.get(i));
            }
            while (!fsoToAddPriv.isEmpty()) {
                FileSystemObject fso = fsoToAddPriv.poll();
                this.viewerIds.add(fso.getId());
                if (fso.type == FSOType.FOLDER) {
                    for (int i = 0; i < children.size(); i++) {
                        fsoToAddPriv.add(children.get(i));
                    }
                }
            }
        }
    }

    public void removePriv(PrivType priv, int userId) {
        if (priv == PrivType.EDIT) {
            editorIds.remove((Integer) userId);
            PriorityQueue<FileSystemObject> fsoToAddPriv = new PriorityQueue<>();
            for (int i = 0; i < children.size(); i++) {
                fsoToAddPriv.add(children.get(i));
            }
            while (!fsoToAddPriv.isEmpty()) {
                FileSystemObject fso = fsoToAddPriv.poll();
                this.editorIds.remove((Integer) fso.getId());
                if (fso.type == FSOType.FOLDER) {
                    for (int i = 0; i < children.size(); i++) {
                        fsoToAddPriv.add(children.get(i));
                    }
                }
            }
        } else {
            viewerIds.remove((Integer) userId);
            PriorityQueue<FileSystemObject> fsoToAddPriv = new PriorityQueue<>();
            for (int i = 0; i < children.size(); i++) {
                fsoToAddPriv.add(children.get(i));
            }
            while (!fsoToAddPriv.isEmpty()) {
                FileSystemObject fso = fsoToAddPriv.poll();
                this.viewerIds.remove((Integer) fso.getId());
                if (fso.type == FSOType.FOLDER) {
                    for (int i = 0; i < children.size(); i++) {
                        fsoToAddPriv.add(children.get(i));
                    }
                }
            }
        }
    }
}
