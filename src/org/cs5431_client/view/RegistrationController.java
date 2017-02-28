package org.cs5431_client.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class RegistrationController implements Initializable {
    @FXML
    public TextField txtUsername;

    @FXML
    public PasswordField txtPassword;

    @FXML
    public PasswordField txtConfirmPassword;

    @FXML
    public TextField txtEmail;

    @FXML
    public TextField txtIP;

    @FXML
    public TextField txtPort;

    @FXML
    public Hyperlink txtIPHelp;

    @FXML
    public Hyperlink txtPortHelp;

    @FXML
    public Button registerButton;

    @FXML
    public Button cancelButton;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtUsername.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtPassword.requestFocus();
            }
        });

        txtPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtConfirmPassword.requestFocus();
            }
        });

        txtConfirmPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtEmail.requestFocus();
            }
        });

        txtEmail.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtIP.requestFocus();
            }
        });

        txtIP.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtPort.requestFocus();
            }
        });

        txtPort.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                registerButton.fire();
            }
        });

        registerButton.setOnAction(e -> tryRegister());

        cancelButton.setOnAction(e -> tryCancel());

        txtIPHelp.setOnAction(e -> displayServerHelp());

        txtPortHelp.setOnAction(e -> displayServerHelp());
    }

    private void tryRegister() {
        System.out.println("Ding! register button pressed");
    }

    private void tryCancel() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.loginNode);
        stage.show();
    }

    private void displayServerHelp() {
        System.out.println("Hi! I'm Clippy. How can I help you?");
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }
}
