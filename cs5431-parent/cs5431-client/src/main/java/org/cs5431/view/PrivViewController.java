package org.cs5431.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.cs5431.controller.AccountsController;
import org.cs5431.controller.FileController;
import org.cs5431.model.FileSystemObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PrivViewController implements Initializable{
    @FXML
    public Text txtFilename;

    @FXML
    public ImageView imgExit;

    @FXML
    public ImageView imgAdd;

    @FXML
    public TableView<PrivBundle> tableViewPriv;

    @FXML
    public TableColumn<PrivBundle, String> usernameColumn;

    @FXML
    public TableColumn<PrivBundle, PrivBundle> privColumn;

    @FXML
    public TableColumn<PrivBundle, PrivBundle> deleteColumn;

    private Stage stage;
    private FileController fileController;
    private AccountsController accountsController;
    private FileSystemObject fso;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialising priv view controller");

        imgExit.setOnMouseClicked(e -> exit());

        imgAdd.setOnMouseClicked(e -> addUser());

        usernameColumn.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );

        privColumn.setCellValueFactory(
                param -> new ReadOnlyObjectWrapper<>(param.getValue())
        );
        privColumn.setCellFactory(param -> new TableCell<PrivBundle,
                PrivBundle>() {
            ObservableList<String> options =
                    FXCollections.observableArrayList("Can Edit", "Can View");
            private final ComboBox<String> comboBox = new ComboBox<>(options);
            private final Label selfLabel = new Label("Can Edit");
            @Override
            protected void updateItem(PrivBundle bundle, boolean empty) {
                super.updateItem(bundle, empty);

                if (bundle == null) {
                    setGraphic(null);
                    return;
                }

                if (bundle.userId == fileController.getLoggedInUid()) {
                    setGraphic(selfLabel);
                } else {
                    comboBox.setValue(bundle.privilege);
                    setGraphic(comboBox);

                    comboBox.valueProperty().addListener((observableValue, prev, now) -> {
                        if (!prev.equals(now) && now.equals("Can Edit") &&
                                !bundle.canEdit) {
                            bundle.canEdit = true;
                            bundle.canView = true;
                            changeToEditor(bundle, comboBox);
                        } else if (!prev.equals(now) && now.equals("Can " +
                                "View") && bundle.canEdit) {
                            bundle.canEdit = false;
                            bundle.canView = true;
                            changeToViewer(bundle, comboBox);
                        }
                    });
                }
            }
        });

        deleteColumn.setCellValueFactory(
                param -> new ReadOnlyObjectWrapper<>(param.getValue())
        );
        deleteColumn.setCellFactory(param -> new TableCell<PrivBundle,
                PrivBundle>() {
            private final Button deleteButton = new Button("Delete");

            @Override
            protected void updateItem(PrivBundle bundle, boolean empty) {
                super.updateItem(bundle, empty);

                if (bundle == null) {
                    setGraphic(null);
                    return;
                }

                if (bundle.userId != fileController.getLoggedInUid()) {
                    setGraphic(deleteButton);
                    deleteButton.setOnAction(event -> removePriv(bundle, getTableView()));
                }
            }
        });
    }

    private void removePriv(PrivBundle bundle, TableView tableView) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            if (bundle.canEdit)
                fileController.removeEditor(bundle.fso, bundle.userId);
            else
                fileController.removeViewer(bundle.fso, bundle.userId);
            return null;
            }
        };
        task.setOnFailed(t -> showError("Failed to remove sharing " +
                "permissions of user: " + bundle.getUsername()));
        task.setOnSucceeded(t -> tableView.getItems().remove(bundle));
        Client.exec.submit(task);
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
    }

    private void changeToEditor(PrivBundle bundle, ComboBox comboBox) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fileController.addEditor(bundle.fso, bundle.userId);
                return null;
            }
        };
        task.setOnFailed(t -> showError("Failed to change sharing " +
                "permissions of user: " + bundle.getUsername()));
        task.setOnSucceeded(t -> bundle.canEdit = true);
        Client.exec.submit(task);
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
                bundle.canEdit = false;
                comboBox.getSelectionModel().select(2);
            }
        });
    }

    private void changeToViewer(PrivBundle bundle, ComboBox comboBox) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fileController.changeEditorToViewer(bundle.fso, bundle.userId);
                return null;
            }
        };
        task.setOnFailed(t -> showError("Failed to change sharing " +
                "permissions of user: " + bundle.getUsername()));
        task.setOnSucceeded(t -> bundle.canEdit = false);
        Client.exec.submit(task);
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
                bundle.canEdit = true;
                comboBox.getSelectionModel().select(1);
            }
        });
    }

    /**
     * Exits back to the file viewer.
     */
    private void exit() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.fileViewNode);
        stage.show();
    }

    /**
     * Opens a dialog to add a new user
     */
    private void addUser() {
        TextInputDialog dialog = new TextInputDialog("username");
        dialog.setTitle("Add new user");
        dialog.setContentText("Please enter the username you wish to add:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            for (PrivBundle bundle : tableViewPriv.getItems()) {
                if (bundle.getUsername().equals(username)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Sharing error");
                    alert.setContentText("This file is already shared with " +
                            "this user.");
                    alert.showAndWait();
                    return;
                }
            }
            Task<PrivBundle> task = new Task<PrivBundle>() {
                @Override
                protected PrivBundle call() throws Exception {
                    int userId = accountsController.getUserId(username);

                    fileController.addNewViewer(fso, userId);
                    return new PrivBundle(userId, retrieveUsername(userId), fso,
                            false,true);
                }
            };
            task.setOnFailed(t -> showError("Failed to share with this " +
                    "user - they might not exist."));
            task.setOnSucceeded(t -> {
                ObservableList<PrivBundle> observableList = tableViewPriv.getItems();
                observableList.add(task.getValue());
                tableViewPriv.setItems(observableList);
            });
            Client.exec.submit(task);
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                    if (task.getValue() != null && tableViewPriv.getItems()
                            .contains(task.getValue()))
                        tableViewPriv.getItems().remove(task.getValue());
                }
            });
        });
    }

    /**
     * When changing to priv_view, it is necessary to pass along the
     * caller's stage so exit() knows how to restore it.
     * @param stage Stage of the caller
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }


    /**
     * When changing to priv_view, it is necessary to pass along the
     * FileController that will retrieve all information needed as well as
     * the file id that is associated with these privileges
     * @param fileController FileController that is associated with this user
     * @param fso FileSystemObject associated the privileges that are being
     *            edited
     */
    void setDetails(FileController fileController, FileSystemObject fso,
                    AccountsController accountsController) {
        this.fileController = fileController;
        this.accountsController = accountsController;
        this.fso = fso;
        txtFilename.setText("Sharing for " + fso.getFileName());

        ObservableList<PrivBundle> observableList =
                FXCollections.observableArrayList();

        Task<List<PrivBundle>> task = new Task<List<PrivBundle>>() {
            @Override
            protected List<PrivBundle> call() throws Exception {
                JSONObject response = fileController.getEditorsViewers(fso);
                List<PrivBundle> privBundles = new ArrayList<>();
                JSONArray editors = response.getJSONArray("editors");
                JSONArray viewers = response.getJSONArray("viewers");
                for (int i = 0; i < editors.length(); i++) {
                    privBundles.add(new PrivBundle(editors.getInt(i), retrieveUsername(editors
                            .getInt(i)), fso, true, true));
                }
                for (int i = 0; i < viewers.length(); i++) {
                    privBundles.add(new PrivBundle(viewers.getInt(i),
                            retrieveUsername(viewers.getInt(i)), fso, false,
                            true));
                }
                return privBundles;
            }
        };
        task.setOnFailed(t -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Sharing error");
            alert.setContentText("Could not retrieve list of editors and " +
                    "viewers");
            alert.showAndWait();
        });
        task.setOnSucceeded(t -> {
            observableList.addAll(task.getValue());
            tableViewPriv.setItems(observableList);
        });
        Client.exec.submit(task);
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
    }

    private String retrieveUsername(int userId) {
        try {
            return accountsController.getUsername(userId);
        } catch (AccountsController.UserRetrieveException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }
}
