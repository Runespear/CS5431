package org.cs5431.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.security.PublicKey;

public class PwdRecoveryBundle {
    int userId;
    final StringProperty username;
    PublicKey publicKey;

    public PwdRecoveryBundle(int userId, String username, PublicKey publicKey) {
        this.userId = userId;
        this.username = new SimpleStringProperty(username);
        this.publicKey = publicKey;
    }
}
