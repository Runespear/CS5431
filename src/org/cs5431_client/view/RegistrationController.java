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
import org.cs5431_client.controller.AccountsController;

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
    private AccountsController accountsController;

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

        cancelButton.setOnAction(e -> exit());

        txtIPHelp.setOnAction(e -> displayServerHelp());

        txtPortHelp.setOnAction(e -> displayServerHelp());
    }

    /**
     * Tries to register an account with the server specified in the form
     * above using the user credentials in the form above.
     * If the registration is successful, returns to the login page.
     * Otherwise displays an error message and remains on this page.
     */
    private void tryRegister() {
        try {
            int userId = accountsController.createUser(
                    txtUsername.getCharacters().toString(),
                    txtPassword.getCharacters().toString(),
                    txtEmail.getCharacters().toString(),
                    txtIP.getCharacters().toString(),
                    txtPort.getCharacters().toString());

            //TODO: IDK what to do with the user id?
            if (userId != -1) {
                exit();
            }
        } catch (AccountsController.RegistrationFailException rfe) {
            //TODO change this to alert box
            System.out.println(rfe.getMessage());
        }
        System.out.println("Ding! register button pressed");
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
     * Displays a dialog box with help that explains the server IP and server
     * port fields.
     */
    private void displayServerHelp() {
        //TODO: change to dialog
        System.out.println("Hi! I'm Clippy. How can I help you?");
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
}
