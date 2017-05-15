package org.cs5431.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ReconstructController implements Initializable {
    @FXML
    public VBox boxen;

    @FXML
    public Button recoverButton;

    @FXML
    public ImageView imgExit;

    @FXML
    public Label neededTxt;

    private Stage stage;
    private Integer uid;
    private String encPK;
    private Integer neededUsers;
    List<TextField> fields = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        recoverButton.setOnAction(e -> tryRecover());

        imgExit.setOnMouseClicked(e -> tryExit());
    }

    void tryRecover() {

    }

    void tryExit() {
        Scene scene = stage.getScene();
        scene.setRoot(Client.loginNode);
        stage.show();
    }

    public void setUp(Stage stage, int uid, String encPK, int neededUsers) {
        this.stage = stage;
        this.uid = uid;
        this.encPK = encPK;
        this.neededUsers = neededUsers;

        for (int i = 1; i <= neededUsers; i++) {
            HBox box = new HBox();
            Label label = new Label();
            label.setText(i + ")");
            TextField text = new TextField();
            box.getChildren().addAll(label, text);
            fields.add(text);
        }

        neededTxt.setText(neededUsers + " codes are needed from your friends:");
    }
}
