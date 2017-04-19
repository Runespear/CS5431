package org.cs5431.view;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cs5431.controller.AccountsController;
import org.cs5431.controller.FileController;
import org.cs5431.controller.UserController;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.Folder;
import org.cs5431.model.User;

import java.io.File;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cs5431.Constants.DEBUG_MODE;

public class FileViewController implements Initializable {
    @FXML
    public ImageView imgBack;

    @FXML
    public ImageView imgRefresh;

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
    private UserController userController;
    private FileController fileController;
    private AccountsController accountsController;
    private Folder currParent;
    private List<Folder> path;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtUsername.setPadding(new Insets(0,0,0,4));
        txtLogout.setPadding(new Insets(0,0,0,4));

        imgBack.setOnMouseClicked(e -> gotoParentFolder());

        imgRefresh.setOnMouseClicked(e -> populateListView());

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
                    fileController.isEditor(fso),
                    fso instanceof org.cs5431.model.File);
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
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    fileController.createFolder(folderName, currParent);
                    return null;
                }
            };
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
            task.setOnSucceeded(t -> populateListView());
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                }
            });
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

        if (currParent == null) {
            System.out.println("currparent is null");
        } else {
            System.out.println("currparent is not null");
        }

        if (fileToUpload != null) {
            System.out.println(fileToUpload.getName());
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    fileController.uploadFile(fileToUpload, currParent);
                    return null;
                }
            };
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();

            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                }
            });
            task.setOnSucceeded(t -> populateListView());
            task.setOnFailed(t -> populateListView());
        }

    }

    /**
     * Downloads the file that is currently highlighted from the server.
     */
    private void downloadFile() {
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose folder to download to");
        File dir = directoryChooser.showDialog(stage);

        if (dir != null) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    fileController.download(fso.getId(), fso
                            .getFileName(), dir);
                    return null;
                }
            };
            task.setOnSucceeded(t-> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Download successful");
                alert.setContentText("Successfully downloaded " + fso
                        .getFileName() + "!");
                alert.showAndWait();
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();

            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    //TODO alert?
                    Exception ex = (Exception) newValue;
                    ex.printStackTrace();
                }
            });
        }
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
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    fileController.overwrite((org.cs5431.model.File) fso, fileToUpload);
                    return null;
                }
            };
            task.setOnSucceeded(t -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Download successful");
                alert.setContentText("Successfully overwrote " + fso.getFileName() + "!");
                alert.showAndWait();
                populateListView();
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                //TODO don't throw exception if due to not enough privileges?
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            });
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
            pvc.setDetails(fileController, fso, accountsController);
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
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (fso.isEditor())
                        fileController.deleteForAll(fso, currParent);
                    else
                        fileController.deleteForUser(fso, currParent);
                    return null;
                }
            };
            task.setOnSucceeded(t -> {
                //TODO check return value?
                populateListView();
                showAppropriateImages(false, false, false);
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                //TODO don't throw exception if due to not enough privileges?
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            });
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
        FileSystemObject fso = fileList.getSelectionModel().getSelectedItem();

        if (fso != null) {
            try {
                Node node = (Node) e.getSource();
                Stage stage = (Stage) node.getScene().getWindow();
                Scene scene = stage.getScene();

                final URL r = getClass().getResource("log_view.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(r);
                Parent root = fxmlLoader.load();
                LogViewController lvc = fxmlLoader.getController();
                lvc.setStage(stage);
                lvc.setDetails(fileController, fso.getId(), fso.getFileName());
                scene.setRoot(root);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Exits back to the login page.
     */
    private void tryLogout() {
        try {
            userController.logout(); //has to block the UI thread for correctness
            //user.getPrivKey().destroy();
            showAppropriateImages(false, false, false);
            Scene scene = stage.getScene();
            scene.setRoot(Client.loginNode);
            stage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Logout error");
            alert.setContentText("Logout unsuccessful. Please try again.");
            alert.showAndWait();
            e.printStackTrace();
        }
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
     */
    void setUserDetails(User user, Socket sslSocket, AccountsController accountsController) {
        this.user = user;
        txtUsername.setText(user.getUsername());
        currParent = user.getUserParentFolder();
        path = new ArrayList<>();
        path.add(currParent);
        fileController = new FileController(user,sslSocket);
        userController = new UserController(user,sslSocket);
        this.accountsController = accountsController;
        currParent = user.getUserParentFolder();
        //initFakeFiles();
        populateListView();
    }

    private void populateListView() {
        Task<List<FileSystemObject>> task = new Task<List<FileSystemObject>>() {
            @Override
            protected List<FileSystemObject> call() throws Exception {
                return fileController.getChildren(currParent);
            }
        };
        task.setOnSucceeded(t -> {
            if (DEBUG_MODE) {
                System.out.println("Succeeded with " + task.getValue().size() +
                        " children");
            }
            List<FileSystemObject> children = task.getValue();
            ObservableList<FileSystemObject> observableList =
                    FXCollections.observableArrayList();
            observableList.setAll(children);

            fileList.setItems(observableList);
            fileList.setCellFactory(listView -> new FileViewCell(fileController));

            fileList.setVisible(!children.isEmpty());
            fileList.setDisable(children.isEmpty());

            imgUpload.setVisible(currParent.isEditor());
            imgUpload.setDisable(!currParent.isEditor());
            });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Upload error");
                alert.setContentText("Upload unsuccessful. Please try again.");
                alert.showAndWait();
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
    }
    
    private void showAppropriateImages(boolean fileSelected, boolean
            editAllowed, boolean isFile) {
        if (fileSelected) {
            imgDownload.setVisible(isFile);
            imgDownload.setDisable(!isFile);
            imgEdit.setVisible(isFile && editAllowed);
            imgEdit.setDisable(!isFile && editAllowed);
        }
        imgViewLog.setVisible(fileSelected);
        imgViewLog.setDisable(!fileSelected);
        imgShare.setVisible(editAllowed);
        imgShare.setDisable(!editAllowed);
        imgDelete.setVisible(fileSelected);
        imgDelete.setDisable(!fileSelected);
    }
}
