package org.cs5431.view;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.cs5431.Validator;
import org.cs5431.controller.AccountsController;
import org.cs5431.model.User;

import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.cs5431.Constants.EMAIL_2FA;
import static org.cs5431.Constants.NO_2FA;
import static org.cs5431.Constants.PHONE_2FA;

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

    @FXML
    public Circle passwordCircle;

    @FXML
    public Hyperlink email2faLink;

    @FXML
    public RadioButton noneRadio;

    @FXML
    public RadioButton email2faRadio;

    @FXML
    public RadioButton phone2faRadio;

    @FXML
    public Button pwdRecoveryButton;

    @FXML
    public TextField txtPhoneNumber;

    private Stage stage;
    private AccountsController accountsController;

    private boolean hasRecovery = false;
    private List<Integer> nominatedUids = new ArrayList<>();
    private List<PublicKey> publicKeys = new ArrayList<>();
    private Integer neededUsers = 0;
    private final ToggleGroup group2fa = new ToggleGroup();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtUsername.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtPassword.requestFocus();
            }
        });

        txtPassword.textProperty().addListener((ov, oldS, newS) -> {
            if (newS.length() < 16)
                passwordCircle.setFill(Color.RED);
            else
                passwordCircle.setFill(Color.GREEN);
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
                txtPhoneNumber.requestFocus();
            }
        });

        txtPhoneNumber.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                registerButton.fire();
            }
        });

        registerButton.setOnAction(e -> tryRegister());

        cancelButton.setOnAction(e -> exit());

        email2faLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Email two-factor authentication");
            alert.setContentText("By activating 2fa, the system will email or sms you with a code every time you login.\n" +
                    "You will need to enter the code into this app.\n" +
                    "If you check this box, you will need to provide us with a valid email or phone number.");
            alert.showAndWait();
        });

        passwordCircle.setFill(Color.RED);
        Tooltip pwdTooltip = new Tooltip("Minimum 16 characters");
        passwordCircle.getProperties().put("Minimum 16 characters", pwdTooltip);
        Tooltip.install(passwordCircle, pwdTooltip);

        pwdRecoveryButton.setOnAction(this::trySetRecovery);

        noneRadio.setToggleGroup(group2fa);
        email2faRadio.setToggleGroup(group2fa);
        phone2faRadio.setToggleGroup(group2fa);
        noneRadio.setSelected(true);
    }

    private void trySetRecovery(Event e){
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("pwd_recovery.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            PwdRecoveryController prc = fxmlLoader.getController();
            prc.setUpFromRegistration(stage, Client.registrationNode, accountsController, this,
                    hasRecovery, nominatedUids, neededUsers);
            scene.setRoot(root);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    void setRecoveryInfo(boolean hasRecovery, List<Integer> nominatedUids, int neededUsers,
                         List<PublicKey> publicKeys) {
        this.hasRecovery = hasRecovery;
        this.nominatedUids = nominatedUids;
        this.neededUsers = neededUsers;
        this.publicKeys = publicKeys;
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
        String phoneNumber = txtPhoneNumber.getCharacters().toString();
        int twoFa = 0;
        RadioButton selectedRadioButton = (RadioButton) group2fa.getSelectedToggle();
        String value = selectedRadioButton.getText();
        if (value.equals("None"))
            twoFa = NO_2FA;
        else if (value.equals("Email"))
            twoFa = EMAIL_2FA;
        else if (value.equals("Phone"))
            twoFa = PHONE_2FA;
        else {
            System.err.println("Ack, value of selected radio button is weird: " + value);
        }
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

        if (twoFa == EMAIL_2FA && email.isEmpty()) {
            errMessages.add("If you activate email 2fa, you must provide a valid email.");
        }

        if (twoFa == PHONE_2FA && phoneNumber.isEmpty()) {
            errMessages.add("If you activate phone 2fa, you must provide a phone number.");
        }

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
            int finalTwoFa = twoFa;
            Task<User> task = new Task<User>() {
                @Override
                protected User call() throws Exception {
                    return accountsController.createUser(username, password, email, phoneNumber, finalTwoFa,
                            hasRecovery, nominatedUids, neededUsers, publicKeys);
                }
            };
            task.setOnSucceeded(t -> {
                cancelButton.getScene().setCursor(Cursor.DEFAULT);
                registerButton.setDisable(false);
                cancelButton.setDisable(false);
                pwdRecoveryButton.setDisable(false);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Registration successful");
                alert.setContentText("Registration successful! Bringing you " +
                        "back to the login page...");
                alert.showAndWait();
                exit();
            });
            cancelButton.getScene().setCursor(Cursor.WAIT);
            registerButton.setDisable(true);
            cancelButton.setDisable(true);
            pwdRecoveryButton.setDisable(true);
            Client.exec.submit(task);
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                    cancelButton.getScene().setCursor(Cursor.DEFAULT);
                    registerButton.setDisable(false);
                    cancelButton.setDisable(false);
                    pwdRecoveryButton.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Registration failed");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
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
}
