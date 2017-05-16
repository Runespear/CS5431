package org.cs5431.model;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;

public class User extends Account {
    private Folder userParentFolder;
    private String email;
    private PrivateKey privKey;
    private PublicKey pubKey;
    private int has2fa;
    private String phoneNo;
    private Timestamp keyLastUpdated;  //get when key was last updated

    public User(int id, String username, String email, Folder
            userParentFolder, PrivateKey privKey, PublicKey pubKey, int has2fa,
                String phoneNo, Timestamp keyLastUpdated) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.userParentFolder = userParentFolder;
        this.privKey = privKey;
        this.pubKey = pubKey;
        this.has2fa = has2fa;
        this.phoneNo = phoneNo;
        if (keyLastUpdated != null)
            this.keyLastUpdated = new Timestamp(keyLastUpdated.getTime());
        else
            this.keyLastUpdated = null;
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

    public int getHas2fa() {return has2fa;}

    public void setHas2fa(int has2fa) {this.has2fa = has2fa;}

    public String getPhoneNo() {return phoneNo;}

    public void setEmail(String email) {this.email = email;}

    public void setPhoneNo(String phoneNo) {this.phoneNo = phoneNo;}

    public Timestamp getKeyLastUpdated() {
        if (keyLastUpdated == null)
            return null;
        return new Timestamp(keyLastUpdated.getTime());
    }
}
