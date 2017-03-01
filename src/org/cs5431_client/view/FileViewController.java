package org.cs5431_client.view;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.controller.UserController;
import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;

import java.net.URL;
import java.util.ArrayList;
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
    public TreeView foldersTree;

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

        imgCreateFolder.setOnMouseClicked(e -> createFolder());

        imgUserPicture.setOnMouseClicked(this::tryEditDetails);

        txtUsername.setOnMouseClicked(this::tryEditDetails);

        txtLogout.setOnAction(e -> tryLogout());
    }

    /**
     * Creates a new folder using the FileController, with the current user
     * as the owner.
     * Creates a new prompt to ask for the folder name.
     */
    private void createFolder() {
        //TODO: get an alert box to name the folder?
        String folderName = "New Folder";
        Folder folder = new Folder(new ArrayList<>());
        fileController.createFolder(folderName, folder);
        //TODO: repopulate list of files/folders
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
    private void tryViewFileLog(Event e) {
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
    //TODO: check if can put this in some constructor instead? :/
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
    }
}
