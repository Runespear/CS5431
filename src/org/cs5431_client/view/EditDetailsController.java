package org.cs5431_client.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.cs5431_client.controller.UserController;
import org.cs5431_client.model.User;
import org.cs5431_client.util.Validator;

import java.io.IOException;
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

    /**
     * Tries to save the details in the form above using the UserController.
     * If the password fields are blank, no request to change the password is
     * made. Likewise for the email fields.
     * Nothing is done if all fields are blank.
     */
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

        boolean msgShownLater = false;
        //Tries to change the password if the password fields are not blank.
        if (!oldPassword.isEmpty() || !newPassword.isEmpty() ||
            !confirmNewPassword.isEmpty()) {
            if (oldPassword.isEmpty() || newPassword.isEmpty() ||
                confirmNewPassword.isEmpty()) {
                messages.add("At least one password field is empty.");
            } else if (!newPassword.equals(confirmNewPassword)){
                messages.add("New passwords don't match.");
            } else if (!Validator.validPassword(newPassword)) {
                messages.add("Passwords should be at least 16 characters " +
                        "long.");
            } else {
                msgShownLater = true;
                Task<Integer> task = new Task<Integer>() {
                    @Override
                    protected Integer call() throws Exception {
                        return userController.changePassword(oldPassword, newPassword);
                    }
                };
                task.setOnFailed(t -> {
                    messages.add("Wrong password.");
                    showMessages(messages);
                });
                task.setOnSucceeded(t -> {
                    messages.add("Password successfully changed.");
                    showMessages(messages);
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
                task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                    if(newValue != null) {
                        Exception ex = (Exception) newValue;
                        ex.printStackTrace();
                    }
                });
            }
        }

        //Tries to change the email if the email fields are not blank.
        if (!oldEmail.isEmpty() || !newEmail.isEmpty() ||
                !confirmNewEmail.isEmpty()) {
            if (oldEmail.isEmpty() || newEmail.isEmpty() ||
                    confirmNewEmail.isEmpty()) {
                messages.add("At least one email field is empty.");
            } else if (!newEmail.equals(confirmNewEmail)){
                messages.add("New emails don't match.");
            } else if (!Validator.validEmail(newEmail)) {
                messages.add("The email entered is invalid.");
            } else {
                userController.changeEmail(oldEmail, newEmail);
                //TODO check here
                messages.add("Email successfully changed.");
            }
        }

        //prints all success+failure messages if not shown in a task
        if (!msgShownLater) {
            showMessages(messages);
        }
    }

    private void showMessages(List<String> messages) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        StringBuilder content = new StringBuilder();
        for (String message : messages) {
            content.append(message);
            content.append("\n");
        }
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * When changing to edit_details, it is necessary to pass along the
     * caller's stage so exit() knows how to restore it.
     * @param stage Stage of the caller
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Exits back to the file viewer.
     */
    private void exit() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.fileViewNode);
        stage.show();
    }

    /**
     * When changing to edit_details, it is necessary to pass along the
     * UserController that will perform the password and email changes.
     * @param userController UserController associated with this user
     */
    void setUserController(UserController userController) {
        this.userController = userController;
    }
}
