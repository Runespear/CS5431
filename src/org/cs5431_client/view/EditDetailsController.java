package org.cs5431_client.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.cs5431_client.controller.UserController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
    public Button exitButton;

    private Stage stage;
    private UserController userController;

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

        exitButton.setOnAction(e -> exit());
    }

    private void trySaveDetails() {
        String oldPassword = txtOldPassword.getCharacters().toString();
        String newPassword = txtNewPassword.getCharacters().toString();
        String confirmNewPassword = txtConfirmNewPassword.getCharacters()
                .toString();
        String oldEmail = txtOldEmail.getCharacters().toString();
        String newEmail = txtNewEmail.getCharacters().toString();
        String confirmNewEmail = txtConfirmNewEmail.getCharacters()
                .toString();
        
        List<String> messages = new ArrayList<>();
        
        if (!oldPassword.equals("") || !newPassword.equals("") ||
            !confirmNewPassword.equals("")) {
            if (oldPassword.equals("") || newPassword.equals("") ||
                confirmNewPassword.equals("")) {
                //TODO someone please change this message
                messages.add("Some password field is empty");
            } else if (!newPassword.equals(confirmNewPassword)){
                messages.add("New passwords don't match");
            } else {
                userController.changePassword(oldPassword, newPassword);
                messages.add("Password successfully changed");
            }
        }

        //TODO add email validation
        if (!oldEmail.equals("") || !newEmail.equals("") ||
                !confirmNewEmail.equals("")) {
            if (oldEmail.equals("") || newEmail.equals("") ||
                    confirmNewEmail.equals("")) {
                //TODO someone please change this message
                messages.add("Some email field is empty");
            } else if (!newEmail.equals(confirmNewEmail)){
                messages.add("New emails don't match");
            } else {
                userController.changeEmail(oldEmail, newEmail);
                //TODO check here
                messages.add("Email successfully changed");
            }
        }

        for (String message : messages) {
            //TODO: print all the messages in alert instead of standard output
            System.out.println(message);
        }
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    private void exit() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.fileViewNode);
        stage.show();
    }

    void setUserController(UserController userController) {
        this.userController = userController;
    }
}
