package org.cs5431_client.model;

public class User extends Account {
    private int userLogId;
    private Folder userParentFolder;
    private String email;

    public User(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public int getUserLogId() {
        return userLogId;
    }

    public Folder getUserParentFolder() {
        return userParentFolder;
    }

    public String getEmail() {
        return email;
    }
}
