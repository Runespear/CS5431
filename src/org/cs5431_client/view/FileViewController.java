package org.cs5431_client.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.controller.UserController;
import org.cs5431_client.model.FileActionType;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;
import org.cs5431_client.util.client_tcp;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
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
    private Folder currParent;
    private List<Folder> path;

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

        imgShare.setOnMouseClicked(this::tryChangePrivileges);

        imgDelete.setOnMouseClicked(e -> deleteFile());

        imgUserPicture.setOnMouseClicked(this::tryEditDetails);

        txtUsername.setOnMouseClicked(this::tryEditDetails);

        txtLogout.setOnAction(e -> tryLogout());

        fileList.setOnMouseClicked(e -> {
            FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();
            if (fso != null) {
                showAppropriateImages(true,
                    fileController.isAllowed(FileActionType.OVERWRITE, fso),
                    fso instanceof org.cs5431_client.model.File);
            } else {
                showAppropriateImages(false, false, false);
            }

            if (e.getButton() == MouseButton.PRIMARY &&
                    e.getClickCount() == 2 && fso instanceof Folder) {
                path.add(currParent);
                currParent = (Folder) fso;
                populateListView();
                imgBack.setOpacity(1.0);
                imgBack.setDisable(false);
                showAppropriateImages(false, false, false);
            }
        });
        //TODO: figure out how to get list view to lose its focus...
    }

    /**
     * Navigates back to the parent folder
     */
    private void gotoParentFolder() {
        currParent = path.get(path.size()-1);
        path.remove(path.size()-1);
        populateListView();
        showAppropriateImages(false, false, false);
        if (currParent == user.getUserParentFolder()) {
            imgBack.setOpacity(0.7);
            imgBack.setDisable(true);
        }
    }

    /**
     * Creates a new folder using the FileController, with the current user
     * as the owner.
     * Creates a new prompt to ask for the folder name.
     */
    private void createFolder() {

        TextInputDialog dialog = new TextInputDialog("folder name");
        dialog.setTitle("Create new folder");
        dialog.setContentText("Folder name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(folderName -> {
                    try {
                        fileController.createFolder(folderName, currParent);
                        populateListView();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
                //currParent.addChild(new Folder(folderName, currParent, user.getId())));
    }

    /**
     * Uploads the file chosen in the file chooser dialog to the server.
     * The uploader is automatically set as the owner.
     */
    private void uploadFile() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file to upload");
        File fileToUpload = fileChooser.showOpenDialog(stage);

        if (fileToUpload != null) {
            try {
                fileController.uploadFile(fileToUpload, currParent);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //currParent.addChild(new org.cs5431_client.model.File
                    //(fileToUpload.getName(), currParent, user.getId(), fileToUpload.length(), ""));
        }
        populateListView();
    }

    /**
     * Downloads the file that is currently highlighted from the server.
     */
    private void downloadFile() {
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();
        org.cs5431_client.model.File file = fileController.download(fso.getId
                ());
        //TODO use file somehow

        //TODO: remove after demo: simply to demo our tcp connection
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                //SIMULATE A FILE DOWNLOAD
                client_tcp clientTcp = new client_tcp();
                clientTcp.start();
                try {
                    clientTcp.connectToServer();
                    clientTcp.requestHardCodedFile();
                } catch(IOException e) {
                    System.err.println("Encountered IOException when trying to " +
                            "connect to server");
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    /**
     * Overwrites the file that is currently highlighted by a file that is
     * chosen using a file chooser dialog.
     */
    private void overwriteFile() {
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file to upload");
        File fileToUpload = fileChooser.showOpenDialog(stage);

        if (fileToUpload != null) {
            fileController.overwrite((org.cs5431_client.model.File) fso, fileToUpload);
        }
    }

    /**
     * Changes the privileges of the file that is currently highlighted: can
     * add privileges or remove privileges
     */
    private void tryChangePrivileges(Event e) {
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("priv_view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            PrivViewController pvc = fxmlLoader.getController();
            pvc.setStage(stage);
            pvc.setDetails(fileController, fso);
            scene.setRoot(root);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Deletes the file that is currently highlighted.
     */
    private void deleteFile() {
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm deletion of file");
        //TODO: change "this file" to the actual filename
        alert.setContentText("Are you sure you want to delete this file?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            fileController.delete(fso, currParent);
        }
        populateListView();
        showAppropriateImages(false, false, false);
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
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();

        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("log_view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            LogViewController lvc = fxmlLoader.getController();
            lvc.setStage(stage);
            lvc.setDetails(fileController, fso.getId());
            scene.setRoot(root);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Exits back to the login page.
     */
    private void tryLogout() {
        showAppropriateImages(false, false, false);
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
        currParent = user.getUserParentFolder();
        path = new ArrayList<>();
        path.add(currParent);
        fileController = new FileController(user,ip,port);
        userController = new UserController(user,ip,port);
        currParent = user.getUserParentFolder();
        initFakeFiles();
        populateListView();
    }

    private void initFakeFiles() {
        //TODO: uncomment following line once UserController fully implemented
        // List<FileSystemObject> fsoList = userController.getFileSystemObjects();
        System.out.print("Initializing fake files...");
        currParent = user.getUserParentFolder();
        //populating with some dummy stuff instead
        Folder dummyFolder = new Folder(1, "fake folder1", currParent, new Timestamp(System.currentTimeMillis()));
        org.cs5431_client.model.File dummyChild =
                new org.cs5431_client.model.File(2, "fake child1", dummyFolder,100,
                new Timestamp(System.currentTimeMillis()));
        currParent.addChild(dummyFolder);
        dummyFolder.addChild(dummyChild);
        dummyChild =
                new org.cs5431_client.model.File(3, "fake child2", dummyFolder, 100,
                        new Timestamp(System.currentTimeMillis()));
        dummyFolder.addChild(dummyChild);
        Folder dummyFolder2 = new Folder(4,"fake folder2", currParent, new Timestamp(System.currentTimeMillis()));
        currParent.addChild(dummyFolder2);
        org.cs5431_client.model.File dummyFile;
        dummyFile =
                new org.cs5431_client.model.File(5,"fake file1", currParent,100,
                        new Timestamp(System.currentTimeMillis()));
        currParent.addChild(dummyFile);
        dummyFolder.addChild(dummyFile);
        dummyFolder2.addChild(dummyFile);
        dummyFile =
                new org.cs5431_client.model.File(6,"fake file2", currParent, 100,
                        new Timestamp(System.currentTimeMillis()));
        currParent.addChild(dummyFile);
        dummyFolder.addChild(dummyFile);
        dummyFolder2.addChild(dummyFile);
        dummyFile =
                new org.cs5431_client.model.File(7,"fake file3", currParent, 100,
                        new Timestamp(System.currentTimeMillis()));
        currParent.addChild(dummyFile);
        dummyFolder.addChild(dummyFile);
        dummyFile =
                new org.cs5431_client.model.File(8, "fake file4", currParent, 100,
                        new Timestamp(System.currentTimeMillis()));
        currParent.addChild(dummyFile);
        dummyFolder2.addChild(dummyFolder);
    }

    public void populateListView() {
        List<FileSystemObject> children = fileController.getChildren(currParent);

        ObservableList<FileSystemObject> observableList =
                FXCollections.observableArrayList();
        observableList.setAll(children);

        fileList.setItems(observableList);
        fileList.setCellFactory(listView -> new FileViewCell(fileController));

        fileList.setVisible(!children.isEmpty());
        fileList.setDisable(children.isEmpty());
    }
    
    private void showAppropriateImages(boolean fileSelected, boolean
            editAllowed, boolean isFile) {
        if (fileSelected) {
            imgDownload.setVisible(isFile);
            imgDownload.setDisable(!isFile);
            imgEdit.setVisible(isFile);
            imgEdit.setDisable(!isFile);
        }
        imgViewLog.setVisible(fileSelected);
        imgViewLog.setDisable(!fileSelected);
        /*TODO uncomment
        imgShare.setVisible(editAllowed);
        imgShare.setDisable(!editAllowed);
        imgDelete.setVisible(editAllowed);
        imgDelete.setDisable(!editAllowed);
        */
        imgShare.setVisible(fileSelected);
        imgShare.setDisable(!fileSelected);
        imgDelete.setVisible(fileSelected);
        imgDelete.setDisable(!fileSelected);
    }
}
