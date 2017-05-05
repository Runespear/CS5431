package org.cs5431.view;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.cs5431.controller.AccountsController;
import org.cs5431.controller.UserController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PwdRecoveryController implements Initializable {

    @FXML
    public ImageView imgExit;

    @FXML
    public ImageView imgAdd;

    @FXML
    public Hyperlink helpLink;

    @FXML
    public CheckBox pwdRecoveryCheck;

    @FXML
    public TextField neededUsersField;

    @FXML
    public TableView<String> nominatedUsersTable;

    @FXML
    public TableColumn<String, String> usernameColumn;

    @FXML
    public TableColumn<String, String> deleteColumn;

    private List<Integer> nominatedUids;
    private Stage stage;
    private Parent parentNode;
    private AccountsController ac;
    private RegistrationController rc;
    private UserController uc;
    private boolean changed = false;    //TODO set changed for removed user

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imgExit.setOnMouseClicked(e -> saveAndExit());

        imgAdd.setOnMouseClicked(e -> {
            if (pwdRecoveryCheck.isSelected())
                addUser();
        });

        //assume nomination is set to false originally
        imgAdd.setDisable(true);
        nominatedUsersTable.setDisable(true);
        nominatedUsersTable.setOpacity(0.7);

        pwdRecoveryCheck.selectedProperty().addListener((ov, oldS, newS) -> {
            if (oldS != newS) {
                changed = true;
            }
            if (newS) {
                imgAdd.setDisable(false);
                nominatedUsersTable.setDisable(false);
                nominatedUsersTable.setOpacity(1.0);
            } else {
                imgAdd.setDisable(true);
                nominatedUsersTable.setDisable(true);
                nominatedUsersTable.setOpacity(0.7);
            }
        });

        nominatedUids = new ArrayList<>();
    }

    public void setUpFromRegistration(Stage stage, Parent parentNode, AccountsController ac,
                      RegistrationController rc) {
        this.stage = stage;
        this.parentNode = parentNode;
        this.ac = ac;
        this.rc = rc;
        //todo load if the user changes his mind?
    }

    public void setUpFromEditDetails(Stage stage, Parent parentNode, AccountsController ac,
                                     UserController uc) {
        this.stage = stage;
        this.parentNode = parentNode;
        this.ac = ac;
        this.uc = uc;
        loadRecoveryInfo();
    }

    private void addUser() {
        TextInputDialog dialog = new TextInputDialog("username");
        dialog.setTitle("Add new user");
        dialog.setContentText("Please enter the username of the user you wish to nominate");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            for (String currUser : nominatedUsersTable.getItems()) {
                if (currUser.equals(username)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Sharing error");
                    alert.setContentText("This user is already nominated");
                    alert.showAndWait();
                    return;
                }
            }
            Task<Integer> task = new Task<Integer>() {
                @Override
                protected Integer call() throws Exception {
                    return ac.getUserForPwdRecovery(username);
                }
            };
            task.setOnFailed(t -> showError("Failed to share with this " +
                    "user - they might not exist."));
            task.setOnSucceeded(t -> {
                ObservableList<String> observableList = nominatedUsersTable.getItems();
                observableList.add(username);
                nominatedUsersTable.setItems(observableList);
                nominatedUids.add(task.getValue());
                changed = true;
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                    if (task.getValue() != null && nominatedUsersTable.getItems()
                            .contains(username)) {
                        nominatedUsersTable.getItems().remove(username);
                        nominatedUids.remove(task.getValue());
                    }
                }
            });
        });

        //TODO columns
    }

    private void loadRecoveryInfo() {
        //TODO
    }

    private void updateRecoveryInfo(boolean hasRecovery, int neededUsers) throws Exception {
        uc.saveRecoveryInfo(hasRecovery, neededUsers, nominatedUids);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Successfully saved password nomination information");
        alert.showAndWait();
    }

    /**
     * Saves the values and returns back to whoever opened this view
     */
    private void saveAndExit() {
        boolean hasRecovery = pwdRecoveryCheck.isSelected();
        Integer neededUsers = 0;
        if (hasRecovery) {
            try {
                neededUsers = Integer.parseInt(neededUsersField.getCharacters().toString());
            } catch (NumberFormatException e) {
                showError("Please enter a valid number.");
                return;
            }

            if (neededUsers < 0 || neededUsers > nominatedUids.size()) {
                showError("Please nominate a valid number of users.");
                return;
            }

            if (neededUsers == 1) {
                showError("You cannot nominate only one user as this user will know your password.\n" +
                        "Please nominate more users.");
                return;
            }
        }
        //save
        if (uc != null && changed) {
            try {
                updateRecoveryInfo(hasRecovery, neededUsers);
            } catch (Exception e) {
                showError("Failed to save password recovery information...");
                return; //TODO: to exit or not to exit?
            }
        } else {
            rc.setRecoveryInfo(hasRecovery, nominatedUids, neededUsers);
        }

        //exit
        Scene scene = stage.getScene();
        scene.setRoot(parentNode);
        stage.show();

        //todo wipe information? (esp. for registration)
    }

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }
}
