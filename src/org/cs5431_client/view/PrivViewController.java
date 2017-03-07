package org.cs5431_client.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.model.FileSystemObject;

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
    public ListView<PrivBundle> listViewPriv;

    private Stage stage;
    private FileController fileController;
    private FileSystemObject fso;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imgExit.setOnMouseClicked(e -> exit());

        imgAdd.setOnMouseClicked(e -> addUser());
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
        TextInputDialog dialog = new TextInputDialog("user id");
        dialog.setTitle("Add new user");
        dialog.setContentText("Please enter the user id you wish to add:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(s -> {
            try {
                int userId = Integer.parseInt(s);
                //TODO check if user exists
                ObservableList<PrivBundle> observableList = listViewPriv.getItems();

                observableList.add(new PrivBundle(userId, fso, false, false));
                listViewPriv.setItems(observableList);
                listViewPriv.setCellFactory(listView ->
                        new PrivViewCell(fileController, fso));
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("User id not valid: needs to be a number");
                alert.showAndWait();
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
    void setDetails(FileController fileController, FileSystemObject fso) {
        this.fileController = fileController;
        this.fso = fso;
        txtFilename.setText("Sharing for " + fso.getFileName());

        ObservableList<PrivBundle> observableList =
                FXCollections.observableArrayList();
        for (Integer editor : fso.getEditors()) {
            observableList.add(new PrivBundle(editor, fso, true,true));
        }
        for (Integer viewer : fso.getViewers()) {
            observableList.add(new PrivBundle(viewer, fso,false,true));
        }

        listViewPriv.setItems(observableList);
        listViewPriv.setCellFactory(listView ->
                new PrivViewCell(fileController, fso));
    }
}

class PrivBundle {
    int userId;
    FileSystemObject fso;
    boolean canEdit;
    boolean canView;

    PrivBundle(int userId, FileSystemObject fso, boolean canEdit, boolean
            canView) {
        this.userId = userId;
        this.fso = fso;
        this.canEdit = canEdit;
        this.canView = canView;
    }
}
