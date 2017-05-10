package org.cs5431.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PwdRecoveryBundle {
    int userId;
    final StringProperty username;

    public PwdRecoveryBundle(int userId, String username) {
        this.userId = userId;
        this.username = new SimpleStringProperty(username);
    }
}
