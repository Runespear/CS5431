package org.cs5431_client.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class LogViewController implements Initializable {

    @FXML
    public Text txtFilename;

    @FXML
    public ImageView imgExit;

    @FXML
    public ListView listViewLog;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
