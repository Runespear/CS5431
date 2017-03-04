package org.cs5431_client.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.controller.UserController;
import org.cs5431_client.model.FileActionType;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FileViewController implements Initializable {
    @FXML
    public ImageView imgBack;

    @FXML
    public ImageView imgCreateFolder;

    @FXML
    public ImageView imgUpload;

    @FXML
    public ImageView imgDownload;

    @FXML
    public ImageView imgViewLog;

    @FXML
    public ImageView imgEdit;

    @FXML
    public ImageView imgShare;

    @FXML
    public ImageView imgDelete;

    @FXML
    public ImageView imgUserPicture;

    @FXML
    public Hyperlink txtUsername;

    @FXML
    public Hyperlink txtLogout;

    @FXML
    public ListView<FileSystemObject> fileList;

    private Stage stage;
    private User user;
    private String ip;
    private String port;
    private UserController userController;
    private FileController fileController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtUsername.setPadding(new Insets(0,0,0,4));
        txtLogout.setPadding(new Insets(0,0,0,4));

        imgBack.setOnMouseClicked(e -> gotoParentFolder());

        imgCreateFolder.setOnMouseClicked(e -> createFolder());

        imgUpload.setOnMouseClicked(e -> uploadFile());

        imgDownload.setOnMouseClicked(e -> downloadFile());

        imgViewLog.setOnMouseClicked(this::viewFileLog);

        imgEdit.setOnMouseClicked(e -> overwriteFile());

        imgShare.setOnMouseClicked(e -> changePrivileges());

        imgDelete.setOnMouseClicked(e -> deleteFile());

        imgUserPicture.setOnMouseClicked(this::tryEditDetails);

        txtUsername.setOnMouseClicked(this::tryEditDetails);

        txtLogout.setOnAction(e -> tryLogout());

        fileList.setOnMouseClicked(e -> {
            FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();
            showAppropriateImages(true,
                    fileController.isAllowed(FileActionType.OVERWRITE, fso));
        });
        //TODO: figure out how to get list view to lose its focus...
    }

    /**
     * Navigates back to the parent folder
     */
    private void gotoParentFolder() {
        //TODO: retrieve parent folder
        //TODO: set file controller based on that
        //TODO: repopulate list view
    }

    /**
     * Creates a new folder using the FileController, with the current user
     * as the owner.
     * Creates a new prompt to ask for the folder name.
     */
    private void createFolder() {
        //TODO: grab correct parent folder
        Folder parentFolder = new Folder("fake_folder",-1,-1,-1);

        TextInputDialog dialog = new TextInputDialog("folder name");
        dialog.setTitle("Create new folder");
        dialog.setContentText("Folder name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(folderName ->
                fileController.createFolder(folderName,parentFolder));

        //TODO: repopulate list of files/folders
    }

    /**
     * Uploads the file chosen in the file chooser dialog to the server.
     * The uploader is automatically set as the owner.
     */
    private void uploadFile() {
        //TODO: grab correct parent folder
        Folder parentFolder = new Folder("fake_folder",-1,-1,-1);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file to upload");
        File fileToUpload = fileChooser.showOpenDialog(stage);

        if (fileToUpload != null) {
            fileController.uploadFile(fileToUpload, parentFolder);
        }
    }

    /**
     * Downloads the file that is currently highlighted from the server.
     */
    private void downloadFile() {
        //TODO
    }

    /**
     * Overwrites the file that is currently highlighted by a file that is
     * chosen using a file chooser dialog.
     */
    private void overwriteFile() {
        //TODO
    }

    /**
     * Changes the privileges of the file that is currently highlighted: can
     * add privileges or remove privileges
     */
    private void changePrivileges() {
        //TODO
    }

    /**
     * Deletes the file that is currently highlighted.
     */
    private void deleteFile() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm deletion of file");
        //TODO: change "this file" to the actual filename
        alert.setContentText("Are you sure you want to delete this file?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            //TODO: change -1 to the actual file id that is highlighted
            fileController.delete(-1);
        }
    }

    /**
     * Tries to open the edit_details page where the user can edit
     * their password and email.
     */
    private void tryEditDetails(Event e) {
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("edit_details.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            EditDetailsController edc = fxmlLoader.getController();
            edc.setStage(stage);
            edc.setUserController(userController);
            scene.setRoot(root);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Tries to open the file_view page to view the file log associated with
     * the file that is currently highlighted
     */
    private void viewFileLog(Event e) {
        //TODO: think of how to retrieve the file id of highlighted file.
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("log_view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            LogViewController lvc = fxmlLoader.getController();
            lvc.setStage(stage);
            //TODO: change -1 to file id
            lvc.setDetails(fileController, -1);
            scene.setRoot(root);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Exits back to the login page.
     */
    private void tryLogout() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.loginNode);
        stage.show();
    }

    /**
     * When changing to file_view, it is necessary to pass along the
     * caller's stage so exit() knows how to restore it.
     * @param stage Stage of the caller
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * When changing to file_view, it is necessary to pass along the User, ip
     * and port that is associated with the logged in user.
     * @param user User (containing username, pwd, etc) that is logged in
     * @param ip Server IP that this user is connected to
     * @param port Server port that this user is connected to
     */
    void setUserDetails(User user, String ip, String port) {
        this.user = user;
        this.ip = ip;
        this.port = port;
        txtUsername.setText(user.getUsername());
        fileController = new FileController(user,ip,port);
        userController = new UserController(user,ip,port);
        populateListView();
    }

    private void populateListView() {
        //TODO: uncomment following line once UserController fully implemented
        // List<FileSystemObject> fsoList = userController.getFileSystemObjects();
        List<FileSystemObject> fsoList = new ArrayList<>();
        //populating with some dummy stuff instead
        org.cs5431_client.model.File dummyFile =
                new org.cs5431_client.model.File("fake file1",-1,-1,100,
                "lalala");
        fsoList.add(dummyFile);
        dummyFile =
                new org.cs5431_client.model.File("fake file2",-1,-1,100,
                        "lalala");
        fsoList.add(dummyFile);
        dummyFile =
                new org.cs5431_client.model.File("fake file3",-1,-1,100,
                        "lalala");
        fsoList.add(dummyFile);
        dummyFile =
                new org.cs5431_client.model.File("fake file4",-1,-1,100,
                        "lalala");
        fsoList.add(dummyFile);

        ObservableList<FileSystemObject> observableList =
                FXCollections.observableArrayList();
        observableList.setAll(fsoList);

        fileList.setItems(observableList);

        fileList.setCellFactory(
                listView ->  new FileViewCell());
    }
    
    private void showAppropriateImages(boolean fileSelected, boolean
            editAllowed) {
        imgDownload.setVisible(fileSelected);
        imgDownload.setDisable(!fileSelected);
        imgViewLog.setVisible(fileSelected);
        imgViewLog.setDisable(!fileSelected);
        imgEdit.setVisible(editAllowed);
        imgEdit.setDisable(!editAllowed);
        imgShare.setVisible(editAllowed);
        imgShare.setDisable(!editAllowed);
        imgDelete.setVisible(editAllowed);
        imgDelete.setDisable(!editAllowed);
    }
}
