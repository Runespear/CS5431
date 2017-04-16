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
                    if (now.equals("Can Edit") && !bundle.canEdit) {
                        changeToEditor(bundle);
                    } else if (now.equals("Can View") && bundle.canView) {
                        changeToViewer(bundle);
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

    private void changeToEditor(PrivBundle bundle) {
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

    private void changeToViewer(PrivBundle bundle) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fileController.addViewer(bundle.fso, bundle.userId);
                return null;
            }
        };
        task.setOnFailed(t -> showError("Failed to change sharing " +
                "permissions of user: " + bundle.getUsername()));
        task.setOnSucceeded(t -> bundle.canEdit = false);
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
            Task<PrivBundle> task = new Task<PrivBundle>() {
                @Override
                protected PrivBundle call() throws Exception {
                    int userId = accountsController.getUserId(username);
                    fileController.addViewer(fso, userId);
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
