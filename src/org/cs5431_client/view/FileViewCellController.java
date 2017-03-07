package org.cs5431_client.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.model.File;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.Folder;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.cs5431_client.view.Client.printNonFatalError;

public class FileViewCellController implements Initializable {
    @FXML
    private HBox hBox;

    @FXML
    private ImageView imgFile;

    @FXML
    private HBox nameBox;

    @FXML
    private Label fileName;

    @FXML
    private Label lastModified;

    @FXML
    private Label size;

    private FileController fileController;
    private FileSystemObject fso;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileName.setOnMouseClicked(click -> {
            if (click.getButton() == MouseButton.PRIMARY &&
                    click.getClickCount() == 2) {
                renameFile();
            }
        });
    }

    private void renameFile() {
        Label currLabel = fileName;
        nameBox.getChildren().remove(0);
        TextField renameBox = new TextField(fileName.getText());
        renameBox.setPrefWidth(300);
        nameBox.getChildren().add(0,renameBox);

        renameBox.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                performRename(renameBox, currLabel);
            }
        });

        renameBox.focusedProperty().addListener(
            (arg0, oldPropertyValue, newPropertyValue) -> {
            if (!newPropertyValue) {
                performRename(renameBox, currLabel);
            }
        });
    }

    private void performRename(TextField renameBox, Label currLabel) {
        if (fileController.rename(fso,renameBox.getCharacters().toString())) {
            currLabel.setText(renameBox.getCharacters().toString());
        }
        nameBox.getChildren().remove(0);
        nameBox.getChildren().add(0, currLabel);
    }

    public FileViewCellController(FileController fileController) {
        this.fileController = fileController;
        final URL r = getClass().getResource("file_view_cell.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(r);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            printNonFatalError("Failed to load layout");
            e.printStackTrace();
        }
    }

    public void setInfo(FileSystemObject fso)
    {
        this.fso = fso;
        fileName.setText(fso.getFileName());
        lastModified.setText(fso.getLastModified().toString());
        size.setText(Long.toString(fso.getFileSize()));
        //default image is file, so if it's folder change img to folder
        if (fso instanceof File)
            imgFile.setImage(
                    new Image("/org/cs5431_client/assets/ic_file.png"));
        else
            imgFile.setImage(
                    new Image("/org/cs5431_client/assets/ic_folder.png"));
        imgFile.setPreserveRatio(true);
        //TODO: don't hardcode this number?
        imgFile.setFitHeight(20);
    }

    public HBox getBox()
    {
        return hBox;
    }
}
