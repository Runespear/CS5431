package org.cs5431.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.cs5431.controller.AccountsController;
import org.cs5431.model.User;
import org.cs5431.Validator;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
    public Button registerButton;

    @FXML
    public Button cancelButton;

    private Stage stage;
    private AccountsController accountsController;

    private String server;
    private String port;

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
                registerButton.fire();
            }
        });

        registerButton.setOnAction(e -> tryRegister());

        cancelButton.setOnAction(e -> exit());
    }

    /**
     * Tries to register an account with the server specified in the form
     * above using the user credentials in the form above.
     * If the registration is successful, returns to the login page.
     * Otherwise displays an error message and remains on this page.
     */
    private void tryRegister() {
        String username = txtUsername.getCharacters().toString();
        String password = txtPassword.getCharacters().toString();
        String confirmPwd = txtConfirmPassword.getCharacters().toString();
        String email = txtEmail.getCharacters().toString();
        //Client side validation

        List<String> errMessages = new ArrayList<>();
        if (username.isEmpty())
            errMessages.add("The username field is required.");
        else if (!Validator.validUsername(username)) {
            errMessages.add("Username should consist of 5-30 characters " +
                    "that are alphanumeric, _ or -");
        }
        if (password.isEmpty() || confirmPwd.isEmpty()) {
            errMessages.add("The passwords field is required.");
        } else if (!password.equals(confirmPwd)) {
            errMessages.add("The passwords entered do not match.");
        } else if (!Validator.validPassword(password)) {
            errMessages.add("Passwords should be at least 16 characters " +
                    "long.");
        }

        if (!email.isEmpty() && !Validator.validEmail(email))
            errMessages.add("The email entered is invalid.");

        if (!errMessages.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Registration failed");
            StringBuilder content = new StringBuilder();
            for (String message : errMessages) {
                content.append(message);
                content.append('\n');
            }
            alert.setContentText(content.toString());
            alert.showAndWait();
        } else {
            Task<User> task = new Task<User>() {
                @Override
                protected User call() throws Exception {

                    return accountsController.createUser(username, password, email);

                }
            };
            task.setOnSucceeded(t -> {
                cancelButton.getScene().setCursor(Cursor.DEFAULT);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Registration successful");
                alert.setContentText("Registration successful! Bringing you " +
                        "back to the login page...");
                alert.showAndWait();
                exit();
            });
            task.setOnFailed(t -> {
                cancelButton.getScene().setCursor(Cursor.DEFAULT);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Registration failed");
                alert.setContentText("Could not create new user. Please " +
                        "try again.");
                alert.showAndWait();
            });
            cancelButton.getScene().setCursor(Cursor.WAIT);
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

    /**
     * Exits back to the login page.
     */
    private void exit() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.loginNode);
        stage.show();
    }

    /**
     * When changing to registration, it is necessary to pass along the
     * caller's stage so exit() knows how to restore it.
     * @param stage Stage of the caller
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * When changing to log_view, it is helpful to pass along the
     * AccountsController that will perform the registration.
     * @param accountsController AccountsController that will perform the
     *                           registration
     */
    void setAccountsController(AccountsController accountsController) {
        this.accountsController = accountsController;
    }

    void setConnectionDetails(String server, String port) {
        this.server = server;
        this.port = port;
    }
}
