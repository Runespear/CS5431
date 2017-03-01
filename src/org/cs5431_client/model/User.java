package org.cs5431_client.model;

public class User extends Account {
    private int userLogId;
    private int userParentFolderId;
    private String email;

    public User(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public int getUserLogId() {
        return userLogId;
    }

    public int getUserParentFolderId() {
        return userParentFolderId;
    }

    public String getEmail() {
        return email;
    }
}
