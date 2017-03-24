package org.cs5431_client.model;

import javax.crypto.SecretKey;

public class User extends Account {
    private Folder userParentFolder;
    private String email;
    private SecretKey privKey;
    private SecretKey pubKey;

    public User(int id, String username, String email, Folder userParentFolder, SecretKey privKey, SecretKey pubKey) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.userParentFolder = userParentFolder;
        //TODO: get user's logid from controller/server
        this.privKey = privKey;
        this.pubKey = pubKey;
    }

    public Folder getUserParentFolder() {
        return userParentFolder;
    }

    public String getEmail() {
        return email;
    }

    public SecretKey getPrivKey() {
        return privKey;
    }
}
