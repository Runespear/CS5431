package org.cs5431.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.cs5431.SSS;
import org.cs5431.SSS_Reconstructor;
import org.cs5431.controller.AccountsController;

import java.math.BigInteger;
import java.net.URL;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cs5431.Encryption.getPrivKeyFromJSON;
import static org.cs5431.controller.UserController.completeChangePwd;

public class ReconstructController implements Initializable {
    @FXML
    public VBox boxen;

    @FXML
    public Button recoverButton;

    @FXML
    public ImageView imgExit;

    @FXML
    public Label neededTxt;

    private Stage stage;
    private Integer uid;
    private String encPK;
    private Integer neededUsers;
    private List<TextField> fields = new ArrayList<>();
    private String salt;
    private AccountsController ac;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        recoverButton.setOnAction(e -> tryRecover());

        imgExit.setOnMouseClicked(e -> tryExit());
    }

    private void tryRecover() {
        List<String> secrets = new ArrayList<>();
        for (int i = 0; i < neededUsers; i++) {
            String secret = fields.get(i).getCharacters().toString();
            if (secret.isEmpty()) {
                showError("You must fill in all the boxes!");
                return;
            }
            secrets.add(secret);
        }
        SSS_Reconstructor rec = new SSS_Reconstructor();
        try {
            BigInteger pwdBytes = rec.recreateSecret(secrets, neededUsers);
            String pwd = new String(pwdBytes.toByteArray());
            PrivateKey privateKey = getPrivKeyFromJSON(encPK, salt, pwd);

            final String[] newPassword = {null};
            TextInputDialog dialog = new TextInputDialog("Password");
            dialog.setTitle("New Password");
             dialog.setContentText("Please enter a new password:");
            while(newPassword[0] == null) {
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(password -> newPassword[0] = password);
            }

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String username = ac.getUsername(uid);
                    completeChangePwd(newPassword[0], privateKey, salt, username, pwd,
                            uid, ac.getSSLSocket());
                    return null;
                }
            };
            task.setOnSucceeded(t -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Success");
                alert.setContentText("Your password has been successfully changed.\n" +
                        "Bringing you back to the login page...");
                alert.showAndWait();
                tryExit();
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

        } catch (SSS.NotEnoughSubsetsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error when recovering your password. Please make sure the secrets are correct");
            e.printStackTrace();
        }
    }

    private void tryExit() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.loginNode);
        stage.show();
    }

    public void setUp(Stage stage, int uid, String encPK, int neededUsers,
                      String salt, AccountsController ac) {
        this.stage = stage;
        this.uid = uid;
        this.encPK = encPK;
        this.neededUsers = neededUsers;
        this.salt = salt;
        this.ac = ac;

        for (int i = 1; i <= neededUsers; i++) {
            HBox box = new HBox();
            Label label = new Label();
            label.setText(i + ")");
            TextField text = new TextField();
            box.getChildren().addAll(label, text);
            boxen.getChildren().add(box);
            fields.add(text);
        }

        neededTxt.setText(neededUsers + " codes are needed from your friends:");
    }

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }
}
