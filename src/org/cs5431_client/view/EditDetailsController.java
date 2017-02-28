package org.cs5431_client.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class EditDetailsController implements Initializable {
    @FXML
    public PasswordField txtOldPassword;

    @FXML
    public PasswordField txtNewPassword;

    @FXML
    public PasswordField txtConfirmNewPassword;

    @FXML
    public TextField txtOldEmail;

    @FXML
    public TextField txtNewEmail;

    @FXML
    public TextField txtConfirmNewEmail;

    @FXML
    public Button saveButton;

    @FXML
    public Button cancelButton;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtOldPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtNewPassword.requestFocus();
            }
        });

        txtNewPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtConfirmNewPassword.requestFocus();
            }
        });

        txtConfirmNewPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtOldEmail.requestFocus();
            }
        });

        txtOldEmail.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtNewEmail.requestFocus();
            }
        });

        txtNewEmail.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtConfirmNewEmail.requestFocus();
            }
        });

        txtConfirmNewEmail.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                saveButton.fire();
            }
        });

        saveButton.setOnAction(e -> trySaveDetails());

        cancelButton.setOnAction(e -> cancel());
    }

    private void trySaveDetails() {
        System.out.println("Ding! save button pressed");
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    private void cancel() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.fileViewNode);
        stage.show();
    }
}
