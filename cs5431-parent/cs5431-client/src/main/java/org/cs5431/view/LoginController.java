package org.cs5431.view;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.cs5431.Validator;
import org.cs5431.controller.AccountsController;
import org.cs5431.model.User;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Constants.EMAIL_2FA;
import static org.cs5431.Constants.NO_2FA;
import static org.cs5431.controller.SSLController.connect_SSLServerSocket;

public class LoginController implements Initializable {
    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button loginButton;

    @FXML
    private Button disconnectButton;

    @FXML
    public Hyperlink txtNoAcct;

    private AccountsController accountsController;
    private Stage stage;
    private Socket sslSocket;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accountsController = new AccountsController();

        loginButton.setOnAction(this::tryLogin);
        disconnectButton.setOnAction(e -> disconnect());

        txtUsername.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtPassword.requestFocus();
            }
        });

        txtPassword.setOnKeyPressed(key -> {
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
        if (!Validator.validUsername(username) ||
                !Validator.validPassword(password)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login error");
            alert.setContentText("Username or password not valid.");
            alert.showAndWait();
            return;
        }
        Task<User> task = new Task<User>() {
            @Override
            protected User call() throws Exception {
                JSONObject login = accountsController.login(username, password);
                if (login.getInt("has2fa") != NO_2FA) {
                    return new User(login.getInt("uid"), null, null, null, null, null, login.getInt("has2fa"));
                } else {
                    return accountsController.parseLogin(username, password, login, NO_2FA);
                }
            }
        };
        task.setOnSucceeded(t -> {
            if(task.getValue().getHas2fa() != NO_2FA) {
                TextInputDialog dialog = new TextInputDialog("otp");
                dialog.setTitle("Two Factor Authentication");
                if (task.getValue().getHas2fa() == EMAIL_2FA)
                    dialog.setContentText("Please enter the code that has been sent to your email:");
                else
                    dialog.setContentText("Please enter the code that has been sent to your phone:");

                final String[] otp = {null};
                while (otp[0] == null) {    //TODO: is this too harsh?
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(enteredOTP -> otp[0] = enteredOTP);
                }
                do2fa(e, otp[0], task.getValue().getId(), username, password, task.getValue().getHas2fa());
            } else {
                changeToFileView(e, task.getValue());
            }
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login error");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void changeToFileView(Event e, User currUser) {
        try {
            if (!DEBUG_MODE)
                txtPassword.setText("");
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("file_view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            Client.fileViewNode = root;
            FileViewController fvc = fxmlLoader.getController();

            fvc.setUserDetails(currUser, sslSocket, accountsController);
            fvc.setStage(stage);
            scene.setRoot(root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void do2fa(Event e, String otp, int userId, String username, String password, int twofa) {
        Task<User> task = new Task<User>() {
            @Override
            protected User call() throws Exception {
                JSONObject response2fa = accountsController.do2fa(otp, userId);
                return accountsController.parseLogin(username, password, response2fa, twofa);
            }
        };
        task.setOnSucceeded(t -> changeToFileView(e, task.getValue()));
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
            Client.registrationNode = root;
            RegistrationController rc = fxmlLoader.getController();
            rc.setStage(stage);
            rc.setAccountsController(accountsController);
            scene.setRoot(root);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    /**
     * When changing to registration, it is necessary to pass along the
     * caller's stage so disconnect() knows how to restore it.
     * @param stage Stage of the caller
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }

    private void disconnect() {
        //TODO close secure channel
        Scene scene = stage.getScene();
        scene.setRoot(Client.connectNode);
        stage.show();
    }

    void setConnectionDetails(String server, String serverName, String sslPort)
            throws Exception {
        Socket s = connect_SSLServerSocket(server,Integer.parseInt(sslPort), "./user-config/"+serverName+".jks");
        accountsController.setSocket(s);
        sslSocket = s;
    }
}