package org.cs5431_client.view;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.controller.UserController;
import org.cs5431_client.model.Folder;
import org.cs5431_client.model.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class FileViewController implements Initializable {
    private Stage stage;

    @FXML
    public ImageView imgCreateFolder;

    @FXML
    public ImageView imgUpload;

    @FXML
    public ImageView imgDownload;

    @FXML
    public ImageView imgEdit;

    @FXML
    public ImageView imgShare;

    @FXML
    public ImageView imgDelete;

    @FXML
    public ImageView imgUserPicture;

    @FXML
    public Text txtUsername;

    @FXML
    public Hyperlink txtLogout;

    @FXML
    public TreeView foldersTree;

    private User user;
    private String ip;
    private String port;
    private UserController userController;
    private FileController fileController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imgCreateFolder.setOnMouseClicked(e -> createFolder());

        imgUserPicture.setOnMouseClicked(this::tryEditDetails);

        txtLogout.setOnAction(e -> tryLogout());
    }

    private void createFolder() {
        //TODO: get an alert box to name the folder?
        String folderName = "New Folder";
        Folder folder = new Folder(new ArrayList<>());
        fileController.createFolder(folderName, folder);
        //TODO: repopulate list of files/folders
    }

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

    private void tryLogout() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.loginNode);
        stage.show();
    }

    //TODO: check if can put this in some constructor instead? :/
    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setUserDetails(User user, String ip, String port) {
        this.user = user;
        this.ip = ip;
        this.port = port;
        txtUsername.setText(user.getUsername());
        fileController = new FileController(user,ip,port);
        userController = new UserController(user,ip,port);
    }
}
