package org.cs5431.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.cs5431.controller.FileController;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LogViewController implements Initializable {

    @FXML
    public Text txtFilename;

    @FXML
    public ImageView imgExit;

    @FXML
    public ListView<String> listViewLog;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imgExit.setOnMouseClicked(e -> exit());
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
     * @param fsoid File ID associated with the log that is being viewed
     * @param fileName File name associated with the log that is being viewed
     */
    void setDetails(FileController fileController, int fsoid, String fileName) {
        txtFilename.setText(fileName+" log");

        Task<List<String>> task = new Task<List<String>>() {
            @Override
            protected List<String>call() throws Exception {
                return fileController.getFileLogs(fsoid);
            }
        };
        task.setOnFailed(t -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Log retrieval error");
            alert.setContentText("Could not read the logs from the server");
            alert.showAndWait();
        });
        task.setOnSucceeded(t -> {
            ObservableList<String> items = FXCollections.observableArrayList();
            items.addAll(task.getValue());
            listViewLog.setItems(items);
        });
        Client.exec.submit(task);
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
    }

}
