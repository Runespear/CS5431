package org.cs5431.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.cs5431.controller.AccountsController;
import org.cs5431.controller.UserController;
import org.json.JSONArray;
import org.json.JSONObject;

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
    public TableView<PwdRecoveryBundle> nominatedUsersTable;

    @FXML
    public TableColumn<PwdRecoveryBundle, String> usernameColumn;

    @FXML
    public TableColumn<PwdRecoveryBundle, PwdRecoveryBundle> deleteColumn;

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
                imgAdd.setOpacity(1.0);
                nominatedUsersTable.setDisable(false);
                nominatedUsersTable.setOpacity(1.0);
                neededUsersField.setDisable(false);
                neededUsersField.setOpacity(1.0);
            } else {
                imgAdd.setDisable(true);
                imgAdd.setOpacity(0.7);
                nominatedUsersTable.setDisable(true);
                nominatedUsersTable.setOpacity(0.7);
                neededUsersField.setDisable(true);
                neededUsersField.setOpacity(0.7);
            }
        });

        usernameColumn.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );

        deleteColumn.setCellValueFactory(
                param -> new ReadOnlyObjectWrapper<>(param.getValue())
        );
        deleteColumn.setCellFactory(param -> new TableCell<PwdRecoveryBundle,
                PwdRecoveryBundle>() {
            private final Button deleteButton = new Button("Delete");

            @Override
            protected void updateItem(PwdRecoveryBundle bundle, boolean empty) {
                super.updateItem(bundle, empty);

                if (bundle == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(deleteButton);
                deleteButton.setOnAction(event -> nominatedUsersTable.getItems().remove(bundle));
            }
        });

        helpLink.setOnAction(e -> showHelp());
    }

    void setUpFromRegistration(Stage stage, Parent parentNode, AccountsController ac,
                               RegistrationController rc, boolean hasRecovery,
                               List<Integer> nominatedUids, int neededUsers) {
        this.stage = stage;
        this.parentNode = parentNode;
        this.ac = ac;
        this.rc = rc;
        //in case user changes his mind
        pwdRecoveryCheck.setSelected(hasRecovery);
        loadUsernames(nominatedUids);
        neededUsersField.setText(Integer.toString(neededUsers));
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
            for (PwdRecoveryBundle bundle : nominatedUsersTable.getItems()) {
                if (bundle.username.toString().equals(username)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Sharing error");
                    alert.setContentText("This user is already nominated");
                    alert.showAndWait();
                    return;
                }
            }
            Task<PwdRecoveryBundle> task = new Task<PwdRecoveryBundle>() {
                @Override
                protected PwdRecoveryBundle call() throws Exception {
                    return ac.getUserForPwdRecovery(username);
                }
            };
            task.setOnFailed(t -> showError("Failed to share with this " +
                    "user - they might not exist."));
            task.setOnSucceeded(t -> {
                ObservableList<PwdRecoveryBundle> observableList = nominatedUsersTable.getItems();
                observableList.add(task.getValue());
                nominatedUsersTable.setItems(observableList);
                changed = true;
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                    if (task.getValue() != null) {
                        PwdRecoveryBundle bundle = task.getValue();
                        if (nominatedUsersTable.getItems().contains(bundle)) {
                            nominatedUsersTable.getItems().remove(bundle);
                        }
                    }
                }
            });
        });
    }

    private void loadRecoveryInfo() {
        Task<JSONObject> task = new Task<JSONObject>() {
            @Override
            protected JSONObject call() throws Exception {
                return uc.getRecoveryInfo();
            }
        };
        task.setOnFailed(t -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Sharing error");
            alert.setContentText("Could not retrieve password recovery information of this user!");
            alert.showAndWait();
        });
        task.setOnSucceeded(t -> {
            JSONObject response = task.getValue();
            //TODO check names of fields
            pwdRecoveryCheck.setSelected(response.getBoolean("hasPwdRec"));
            if (response.getBoolean("hasPwdRec")) {
                neededUsersField.setText(Integer.toString(response.getInt("neededUsers")));
                JSONArray uidArray = response.getJSONArray("groupUid");
                List<Integer> uidList = new ArrayList<>();
                for (int i = 0; i < uidArray.length(); i++) {
                    uidList.add(uidArray.getInt(i));
                }
                loadUsernames(uidList);
            }
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

    private void loadUsernames(List<Integer> nominatedUids) {
        ObservableList<PwdRecoveryBundle> observableList =
                FXCollections.observableArrayList();

        Task<List<PwdRecoveryBundle>> task = new Task<List<PwdRecoveryBundle>>() {
            @Override
            protected List<PwdRecoveryBundle> call() throws Exception {
                List<PwdRecoveryBundle> bundleList = new ArrayList<>();
                for (int uid : nominatedUids) {
                    String username = ac.getUsername(uid);
                    bundleList.add(ac.getUserForPwdRecovery(username));
                }
                return bundleList;
            }
        };
        task.setOnFailed(t -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Sharing error");
            alert.setContentText("Could not retrieve usernames of nominated users");
            alert.showAndWait();
        });
        task.setOnSucceeded(t -> {
            observableList.addAll(task.getValue());
            nominatedUsersTable.setItems(observableList);
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

    private void updateRecoveryInfo(boolean hasRecovery, int neededUsers,
                                    List<Integer> nominatedUids, List<String> encSecrets) throws Exception {
        uc.saveRecoveryInfo(hasRecovery, neededUsers, nominatedUids, encSecrets);
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

            if (neededUsers < 0 || neededUsers > nominatedUsersTable.getItems().size()) {
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
        //TODO: gen secrets here
        List<Integer> nominatedUids = new ArrayList<>();
        List<String> encSecrets = new ArrayList<>();
        for (PwdRecoveryBundle bundle : nominatedUsersTable.getItems()) {
            nominatedUids.add(bundle.userId);
            //TODO: encrypt secret here
        }
        if (uc != null && changed) {
            try {
                updateRecoveryInfo(hasRecovery, neededUsers, nominatedUids, encSecrets);
            } catch (Exception e) {
                showError("Failed to save password recovery information...");
                return; //TODO: to exit or not to exit?
            }
        } else if (rc != null) {
            rc.setRecoveryInfo(hasRecovery, nominatedUids, neededUsers); //TODO: add encSecrets
        }

        //exit
        Scene scene = stage.getScene();
        scene.setRoot(parentNode);
        stage.show();

        //todo wipe information? (esp. for registration)
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password recovery");
        alert.setContentText("If you activate password recovery, on the event of a password loss," +
                " we will email your friends a code that is needed to reconstruct your password.");
        alert.showAndWait();
    }

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }
}
