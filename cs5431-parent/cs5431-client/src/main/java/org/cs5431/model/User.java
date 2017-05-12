package org.cs5431.model;

import java.security.PrivateKey;
import java.security.PublicKey;

public class User extends Account {
    private Folder userParentFolder;
    private String email;
    private PrivateKey privKey;
    private PublicKey pubKey;
    private boolean has2fa;

    public User(int id, String username, String email, Folder
            userParentFolder, PrivateKey privKey, PublicKey pubKey, boolean has2fa) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.userParentFolder = userParentFolder;
        this.privKey = privKey;
        this.pubKey = pubKey;
        this.has2fa = has2fa;
    }

    public Folder getUserParentFolder() {
        return userParentFolder;
    }

    public String getEmail() {
        return email;
    }

    public PrivateKey getPrivKey() {
        return privKey;
    }

    public PublicKey getPubKey() {
        return pubKey;
    }

    public boolean getHas2fa() {return has2fa;}
}
