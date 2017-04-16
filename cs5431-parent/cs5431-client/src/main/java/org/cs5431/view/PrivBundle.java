package org.cs5431.view;

import javafx.beans.property.SimpleStringProperty;
import org.cs5431.model.FileSystemObject;

public class PrivBundle {
    int userId;
    FileSystemObject fso;
    boolean canEdit;
    boolean canView;
    private final SimpleStringProperty username;
    String privilege;

    public PrivBundle(int userId, String username, FileSystemObject fso, boolean
            canEdit, boolean canView) {
        this.userId = userId;
        this.fso = fso;
        this.canEdit = canEdit;
        this.canView = canView;
        if (canEdit && canView)
            privilege = "Can Edit";
        else if (canView)
            privilege = "Can View";
        else
            privilege = "Corrupted";
        this.username = new SimpleStringProperty(username);
    }

    public String getUsername() {return username.get();}
}
