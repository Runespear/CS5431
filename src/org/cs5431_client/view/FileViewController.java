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

import java.net.URL;
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

    private String username;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (username == null) {
            Client.printNonFatalError("No username set?!");
            username = "username";
        }
        System.out.println(username);
        txtUsername.setText(username);

        txtLogout.setOnAction(e -> tryLogout());

        imgUserPicture.setOnMouseClicked(this::tryEditDetails);
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

    void setUsername(String username) {
        this.username = username;
    }
}
