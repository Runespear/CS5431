package org.cs5431_client.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.cs5431_client.controller.FileController;

import java.net.URL;
import java.util.ResourceBundle;

public class LogViewController implements Initializable {

    @FXML
    public Text txtFilename;

    @FXML
    public ImageView imgExit;

    @FXML
    public ListView<String> listViewLog;

    private Stage stage;
    private FileController fileController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imgExit.setOnMouseClicked(e -> exit());
    }

    /**
     * Exits back to the file viewer.
     */
    private void exit() {
        System.out.println("EXIT!!!");
        Scene scene = stage.getScene();
        scene.setRoot(Client.fileViewNode);
        stage.show();
    }

    /**
     * When changing to log_view, it is necessary to pass along the
     * caller's stage so exit() knows how to restore it.
     * @param stage Stage of the caller
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }


    /**
     * When changing to log_view, it is necessary to pass along the
     * FileController that will retrieve all information needed as well as
     * the file id that is associated with this specific file log.
     * @param fileController FileController that is associated with this user
     * @param fileId File ID associated with the log that is being viewed
     */
    void setDetails(FileController fileController, int fileId) {
        this.fileController = fileController;
        //TODO: update txtFilename based on file id?

        ObservableList<String> items = FXCollections.observableArrayList();
        //TODO: populate listViewLog
        items.add("Sample log text");
        listViewLog.setItems(items);
    }

}
