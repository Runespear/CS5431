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
import org.cs5431.model.PrivType;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
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

            @Override
            protected void updateItem(PrivBundle bundle, boolean empty) {
                super.updateItem(bundle, empty);

                if (bundle == null) {
                    setGraphic(null);
                    return;
                }

                comboBox.setValue(bundle.privilege);
                setGraphic(comboBox);

                comboBox.valueProperty().addListener((observableValue, prev, now) -> {
                    try {
                        if (now.equals("Can Edit") && !bundle.canEdit) {
                            fileController.addPriv(bundle.fso, bundle.userId,
                                    PrivType.EDIT);
                            bundle.canEdit = true;
                        } else if (now.equals("Can View") && bundle.canView) {
                            fileController.removePriv(bundle.fso, bundle.userId,
                                    PrivType.EDIT);
                            bundle.canEdit = false;
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
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

                setGraphic(deleteButton);
                deleteButton.setOnAction(event -> {
                    fileController.removePriv(bundle.fso, bundle.userId,
                            PrivType.VIEW);
                    getTableView().getItems().remove(bundle);
                });
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
            Task<Integer> task = new Task<Integer>() {
                @Override
                protected Integer call() throws Exception {
                    int userId = accountsController.getUserId(username);
                    fileController.addPriv(fso, userId, PrivType.VIEW);
                    return userId;
                }
            };
            task.setOnFailed(t -> showError("Failed to share with this " +
                    "user - they might not exist."));
            task.setOnSucceeded(t -> {
                ObservableList<PrivBundle> observableList = tableViewPriv.getItems();
                observableList.add(new PrivBundle(task.getValue(), fso,
                        false,true, accountsController));
                tableViewPriv.setItems(observableList);
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

        Task<JSONObject> task = new Task<JSONObject>() {
            @Override
            protected JSONObject call() throws Exception {
                return fileController.getEditorsViewers(fso);
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
            JSONObject response = task.getValue();
            @SuppressWarnings("unchecked") List<Integer> editors = (List<Integer>) response.get("editors");
            @SuppressWarnings("unchecked") List<Integer> viewers = (List<Integer>) response.get("viewers");
            for (Integer editor : editors) {
                observableList.add(new PrivBundle(editor, fso, true,true, accountsController));
            }
            for (Integer viewer : viewers) {
                observableList.add(new PrivBundle(viewer, fso,false,true, accountsController));
            }
            tableViewPriv.setItems(observableList);
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

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }
}

class PrivBundle {
    int userId;
    FileSystemObject fso;
    boolean canEdit;
    boolean canView;
    String username;
    String privilege;

    PrivBundle(int userId, FileSystemObject fso, boolean canEdit, boolean
            canView, AccountsController accountsController) {
        this.userId = userId;
        this.fso = fso;
        this.canEdit = canEdit;
        this.canView = canView;
        if (canEdit && canView)
            privilege = "Can Edit";
        else if (canView)
            privilege = "Can View";
        else
            privilege = "Corrupted";

        try {
            this.username = accountsController.getUsername(userId);
        } catch (AccountsController.UserRetrieveException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
