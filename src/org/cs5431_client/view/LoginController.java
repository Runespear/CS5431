package org.cs5431_client.view;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.cs5431_client.controller.AccountsController;
import org.cs5431_client.model.User;

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
    private TextField txtPort;

    @FXML
    private Button loginButton;

    @FXML
    public Hyperlink txtNoAcct;

    private AccountsController accountsController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accountsController = new AccountsController();

        loginButton.setOnAction(this::tryLogin);

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
                txtPort.requestFocus();
            }
        });

        txtPort.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                loginButton.fire();
            }
        });

        txtNoAcct.setOnAction(this::goToRegistration);
    }

    /**
     * Tries to login to the server specified in the form above using the
     * user credentials in the form above.
     * If the login is successful, tries to open the file_view page to view
     * the user's files.
     */
    private void tryLogin(Event e) {
        String username = txtUsername.getCharacters().toString();
        String password = txtPassword.getCharacters().toString();
        String server = txtServer.getCharacters().toString();
        String port = txtPort.getCharacters().toString();
        //TODO: catch login failure
        int userId = accountsController.login(username, password, server,
                port);
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("file_view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            Client.fileViewNode = root;
            FileViewController fvc = fxmlLoader.getController();
            fvc.setUserDetails(new User(userId,username, password),server,port);
            fvc.setStage(stage);
            scene.setRoot(root);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Tries to open the registration page to allow the user to create an
     * account with a server.
     */
    private void goToRegistration(Event e) {
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("registration.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            RegistrationController rc = fxmlLoader.getController();
            rc.setStage(stage);
            rc.setAccountsController(accountsController);
            scene.setRoot(root);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
