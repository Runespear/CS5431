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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.cs5431.Encryption;
import org.cs5431.SSS;
import org.cs5431.Validator;
import org.cs5431.controller.AccountsController;
import org.cs5431.controller.UserController;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cs5431.Constants.EMAIL_2FA;
import static org.cs5431.Constants.NO_2FA;
import static org.cs5431.Constants.PHONE_2FA;
import static org.cs5431.Encryption.encryptSecrets;
import static org.cs5431.Encryption.getPubKeyFromJSON;

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

    @FXML
    public Hyperlink email2faLink;

    @FXML
    public Button pwdRecoveryButton;
    
    @FXML
    public TextField txtOldPhoneNo;
    
    @FXML
    public TextField txtNewPhoneNo;
    
    @FXML
    public TextField txtConfirmNewPhoneNo;

    @FXML
    public Button helpRecoveryButton;

    @FXML
    public RadioButton noneRadio;

    @FXML
    public RadioButton email2faRadio;

    @FXML
    public RadioButton phone2faRadio;

    private Stage stage;
    private AccountsController accountsController;
    private UserController userController;
    private final ToggleGroup group2fa = new ToggleGroup();

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
                txtOldPhoneNo.requestFocus();
            }
        });

        txtOldPhoneNo.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtNewPhoneNo.requestFocus();
            }
        });

        txtNewPhoneNo.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                txtConfirmNewPhoneNo.requestFocus();
            }
        });

        txtConfirmNewPhoneNo.setOnKeyPressed(key -> {
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

        email2faLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Email two-factor authentication");
            alert.setContentText("By activating 2fa, the system will email or sms you with a code every time you login.\n" +
                    "You will need to enter the code into this app.\n" +
                    "If you check this box, you will need to provide us with a valid email or phone number.");
            alert.showAndWait();
        });

        pwdRecoveryButton.setOnAction(this::goToRecovery);

        helpRecoveryButton.setOnAction(e -> helpRecover());

        txtOldPassword.requestFocus();

        noneRadio.setToggleGroup(group2fa);
        email2faRadio.setToggleGroup(group2fa);
        phone2faRadio.setToggleGroup(group2fa);
        noneRadio.setSelected(true);
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
        String oldPhone = txtOldPhoneNo.getCharacters().toString();
        String newPhone = txtNewPhoneNo.getCharacters().toString();
        String confirmNewPhone = txtConfirmNewPhoneNo.getCharacters()
                .toString();
        
        List<String> pwdMessages = new ArrayList<>();
        List<String> emailMessages = new ArrayList<>();
        List<String> phoneMessages = new ArrayList<>();

        boolean pwdTaskRunning = false;
        boolean pwdNoChange = false;
        boolean emailTaskRunning = false;
        boolean emailNoChange = false;
        boolean phoneTaskRunning = false;
        boolean phoneNoChange = false;
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
                    tryChangeRecovery(newPassword);
                    pwdMessages.add("Password successfully changed.");
                    showMessages(pwdMessages);
                });
                try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                    if(newValue != null) {
                        Exception ex = (Exception) newValue;
                        ex.printStackTrace();
                    }
                });
            }
        } else {
            pwdNoChange = true;
        }

        //Tries to change the email if the email fields are not blank.
        if (!oldEmail.isEmpty() || !newEmail.isEmpty() ||
                !confirmNewEmail.isEmpty()) {
            if (!newEmail.equals(confirmNewEmail)){
                emailMessages.add("New emails don't match.");
            } else if (!Validator.validEmail(newEmail)) {
                emailMessages.add("The email entered is invalid.");
            } else {
                String currEmail = userController.getEmail();
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
                    userController.setEmail(newEmail);
                    emailMessages.add("Email successfully changed.");
                    showMessages(emailMessages);
                });
                try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                    if(newValue != null) {
                        Exception ex = (Exception) newValue;
                        ex.printStackTrace();
                        userController.setEmail(currEmail);
                    }
                });
            }
        } else {
            emailNoChange = true;
        }

        //Tries to change the phone number if the phone number fields are not blank.
        if (!oldPhone.isEmpty() || !newPhone.isEmpty() ||
                !confirmNewPhone.isEmpty()) {
            if (!newPhone.equals(confirmNewPhone)){
                phoneMessages.add("New phone numbers don't match.");
            } else {
                String currPhone = userController.getPhoneNo();
                phoneTaskRunning = true;
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        userController.changePhoneNumber(oldPhone, newPhone);
                        return null;
                    }
                };
                task.setOnFailed(t -> {
                    phoneMessages.add("Wrong phone number.");
                    showMessages(phoneMessages);
                });
                task.setOnSucceeded(t -> {
                    userController.setPhoneNo(newPhone);
                    phoneMessages.add("Phone number successfully changed.");
                    showMessages(phoneMessages);
                });
                try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                    if(newValue != null) {
                        Exception ex = (Exception) newValue;
                        ex.printStackTrace();
                        userController.setPhoneNo(currPhone);
                    }
                });
            }
        } else {
            phoneNoChange = true;
        }

        //to make things simpler, always attempt to save 2fa if email/phone number provided when needed
        int twoFa = 0;
        RadioButton selectedRadioButton = (RadioButton) group2fa.getSelectedToggle();
        String value = selectedRadioButton.getText();
        switch (value) {
            case "None":
                twoFa = NO_2FA;
                break;
            case "Email":
                twoFa = EMAIL_2FA;
                break;
            case "Phone":
                twoFa = PHONE_2FA;
                break;
            default:
                System.err.println("Ack, value of selected radio button is weird: " + value);
                break;
        }
        boolean do2fa = false;
        if (twoFa == NO_2FA) {
            do2fa = true;
        } else if (twoFa == EMAIL_2FA) {
            //not trying to change email, current email is not empty
            if (!emailTaskRunning && !userController.getEmail().isEmpty())
                do2fa = true;
            //trying to change email, new email is not empty
            if (emailTaskRunning && !newEmail.isEmpty())
                do2fa = true;
        } else {
            if (!phoneTaskRunning && !userController.getPhoneNo().isEmpty())
                do2fa = true;
            if (phoneTaskRunning && !newPhone.isEmpty())
                do2fa = true;
        }
        if (do2fa) {
            int currTwoFa = userController.getHas2fa();
            int finalTwoFa = twoFa;
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    userController.save2fa(finalTwoFa);
                    return null;
                }
            };
            task.setOnFailed(t -> {
                List<String> twoFaMsgs = new ArrayList<>();
                twoFaMsgs.add("Saving two factor authentication information failed.");
                showMessages(twoFaMsgs);
            });
            task.setOnSucceeded(t -> {
                List<String> twoFaMsgs = new ArrayList<>();
                twoFaMsgs.add("Two factor authentication information saved.");
                showMessages(twoFaMsgs);
                userController.setHas2fa(finalTwoFa);
            });
            try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            task.exceptionProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                    userController.setHas2fa(currTwoFa);
                }
            });
        } else {
            showError("Could not save two factor authentication information: check whether" +
                    " email/phone number is provided.");
        }

        //prints all success+failure messages if not shown in a task
        if (!pwdTaskRunning && !pwdNoChange) {
            showMessages(pwdMessages);
        }
        if (!emailTaskRunning && !emailNoChange) {
            showMessages(emailMessages);
        }
        if (!phoneTaskRunning && !phoneNoChange)
            showMessages(phoneMessages);
    }

    private void tryChangeRecovery(String newPassword) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            JSONObject json = userController.getRecoveryInfo();
            boolean hasRecovery = json.getBoolean("hasPwdRec");
            if (hasRecovery) {
                int neededUsers = json.getInt("neededUsers");
                List<Integer> groupId = new ArrayList<>();
                JSONArray groupIdArr = json.getJSONArray("groupUid");
                List<PublicKey> publicKeys = new ArrayList<>();
                JSONArray pubKeysArr = json.getJSONArray("pubKeys");
                for (int i = 0; i < groupIdArr.length(); i++) {
                    groupId.add(groupIdArr.getInt(i));
                    publicKeys.add(getPubKeyFromJSON(pubKeysArr.getString(i)));
                }
                SSS secretGen = new SSS(groupId.size(), neededUsers,
                        new BigInteger(newPassword.getBytes(StandardCharsets.UTF_8)));
                List<String> encSecrets = encryptSecrets(publicKeys, secretGen.generateSecrets());
                userController.saveRecoveryInfo(true, neededUsers, groupId, encSecrets);
            }
            return null;
            }
        };
        try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
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

    private void goToRecovery(Event e) {
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("pwd_recovery.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            PwdRecoveryController prc = fxmlLoader.getController();
            prc.setUpFromEditDetails(stage, Client.editDetailsNode, accountsController, userController);
            scene.setRoot(root);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
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
                    try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    private void helpRecover() {
        TextInputDialog dialog = new TextInputDialog("Code");
        dialog.setTitle("Help someone else recover their password");
        dialog.setContentText("You should have received a code from someone else." +
                "Please enter it here:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            try {
                String secret = userController.decryptSecret(code);
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(secret);
                clipboard.setContent(content);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Secret");
                alert.setContentText("Please share this new code with the person you got this from: "
                        + secret + "\nThe code has also been copied to your clipboard.");
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
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
    void setControllers(UserController userController, AccountsController accountsController) {
        this.userController = userController;
        this.accountsController = accountsController;
    }
}
