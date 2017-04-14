package org.cs5431.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class PrivViewController implements Initializable{
    //TODO: currently we only have the user ids, we should translate them
    // into usernames
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
                        } else if (now.equals("Can View") && bundle.canEdit) {
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
            try {
                int userId = accountsController.getUserId(username);
                fileController.addPriv(fso, userId, PrivType.VIEW);
                ObservableList<PrivBundle> observableList = tableViewPriv.getItems();
                observableList.add(new PrivBundle(userId, fso, false,
                        true, accountsController));
                tableViewPriv.setItems(observableList);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("User id not valid: needs to be a number");
                alert.showAndWait();
            } catch (AccountsController.UserRetrieveException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
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
        this.fso = fso;
        txtFilename.setText("Sharing for " + fso.getFileName());

        ObservableList<PrivBundle> observableList =
                FXCollections.observableArrayList();
        for (Integer editor : fso.getEditors()) {
            observableList.add(new PrivBundle(editor, fso, true,true, accountsController));
        }
        for (Integer viewer : fso.getViewers()) {
            observableList.add(new PrivBundle(viewer, fso,false,true, accountsController));
        }

        tableViewPriv.setItems(observableList);
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
