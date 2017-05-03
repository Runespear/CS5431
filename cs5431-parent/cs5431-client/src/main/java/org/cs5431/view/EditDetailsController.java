package org.cs5431.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.cs5431.Validator;
import org.cs5431.controller.UserController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @FXML
    public Button deleteButton;

    @FXML
    public Circle passwordCircle;

    private Stage stage;
    private UserController userController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtOldPassword.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtNewPassword.requestFocus();
            }
        });

        txtNewPassword.textProperty().addListener((ov, oldS, newS) -> {
            if (newS.length() == 0) {
                passwordCircle.setVisible(false);
            } if (newS.length() < 16) {
                passwordCircle.setVisible(true);
                passwordCircle.setFill(Color.RED);
            }
            else {
                passwordCircle.setVisible(true);
                passwordCircle.setFill(Color.GREEN);
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

        passwordCircle.setVisible(false);
        passwordCircle.setFill(Color.RED);
        Tooltip pwdTooltip = new Tooltip("Minimum 16 characters");
        passwordCircle.getProperties().put("Minimum 16 characters", pwdTooltip);
        Tooltip.install(passwordCircle, pwdTooltip);

        saveButton.setOnAction(e -> trySaveDetails());

        exitButton.setOnAction(e -> exit());

        deleteButton.setOnAction(e -> delete());

        txtOldPassword.requestFocus();
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
        
        List<String> pwdMessages = new ArrayList<>();
        List<String> emailMessages = new ArrayList<>();

        boolean pwdTaskRunning = false;
        boolean emailTaskRunning = false;
        //Tries to change the password if the password fields are not blank.
        if (!oldPassword.isEmpty() || !newPassword.isEmpty() ||
            !confirmNewPassword.isEmpty()) {
            if (oldPassword.isEmpty() || newPassword.isEmpty() ||
                confirmNewPassword.isEmpty()) {
                pwdMessages.add("At least one password field is empty.");
            } else if (!newPassword.equals(confirmNewPassword)){
                pwdMessages.add("New passwords don't match.");
            } else if (!Validator.validPassword(newPassword)) {
                pwdMessages.add("Passwords should be at least 16 characters " +
                        "long.");
            } else {
                pwdTaskRunning = true;
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        userController.changePassword(oldPassword, newPassword);
                        return null;
                    }
                };
                task.setOnFailed(t -> {
                    pwdMessages.add("Wrong password.");
                    showMessages(pwdMessages);
                });
                task.setOnSucceeded(t -> {
                    pwdMessages.add("Password successfully changed.");
                    showMessages(pwdMessages);
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
            if (oldEmail.isEmpty() || confirmNewEmail.isEmpty()) {
                emailMessages.add("New email cannot be empty.");
            } else if (!newEmail.equals(confirmNewEmail)){
                emailMessages.add("New emails don't match.");
            } else if (!Validator.validEmail(newEmail)) {
                emailMessages.add("The email entered is invalid.");
            } else {
                emailTaskRunning = true;
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        userController.changeEmail(oldEmail, newEmail);
                        return null;
                    }
                };
                task.setOnFailed(t -> {
                    emailMessages.add("Email change failed.");
                    showMessages(emailMessages);
                });
                task.setOnSucceeded(t -> {
                    emailMessages.add("Email successfully changed.");
                    showMessages(emailMessages);
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

        //prints all success+failure messages if not shown in a task
        if (!pwdTaskRunning)
            showMessages(pwdMessages);
        if (!emailTaskRunning) {
            showMessages(emailMessages);
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

    private void delete() {
        TextInputDialog dialog = new TextInputDialog("username");
        dialog.setTitle("Delete this account");
        dialog.setContentText("Please enter the username of this account to " +
                "confirm");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            if (userController.isLoggedInUser(username)) {
                TextInputDialog dialog2 = new TextInputDialog("password");
                dialog2.setTitle("Delete this account");
                dialog2.setContentText("Please enter the password of this " +
                        "account to confirm");
                Optional<String> result2 = dialog2.showAndWait();
                result2.ifPresent(password-> {
                    Task<Void> task = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            userController.deleteUser(username, password);
                            return null;
                        }
                    };
                    task.setOnFailed(t -> showError("Failed to delete this account - please " +
                            "double check your password."));
                    task.setOnSucceeded(t -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Deleted successfully");
                        alert.setContentText("Account deleted successfully. " +
                                "Taking you back to the login page...");
                        alert.showAndWait();
                        Scene scene = stage.getScene();
                        scene.setRoot(Client.loginNode);
                        stage.show();
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
                });
            } else {
                showError("Failed to delete this account - please " +
                        "double check your username.");
            }
        });
    }

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();
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
