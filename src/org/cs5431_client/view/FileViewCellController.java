package org.cs5431_client.view;

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
        System.out.println("Clicked " + fso.getId() + "!");

        renameBox.setOnKeyPressed(key -> {
            if (key.getCode().equals(KeyCode.ENTER)) {
                System.out.println(renameBox.getCharacters().toString());
                //TODO send request to server
                currLabel.setText(renameBox.getCharacters().toString());
                nameBox.getChildren().remove(0);
                nameBox.getChildren().add(0,currLabel);
            }
        });
    }

    public FileViewCellController()
    {
        final URL r = getClass().getResource("file_view_cell.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(r);
        fxmlLoader.setController(this);
        try
        {
            fxmlLoader.load();
        }
        catch (IOException e)
        {
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
