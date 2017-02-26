package org.cs5431.GUI;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtServer;

    @FXML
    private Button loginButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginButton.setOnAction(e -> {
            System.out.println("you clicked login");
        });

        txtUsername.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtPassword.requestFocus();
            }
        });

        txtPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtServer.requestFocus();
            }
        });

        txtServer.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                loginButton.fire();
            }
        });
    }
}
