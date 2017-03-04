package org.cs5431_client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.cs5431_client.model.File;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.Folder;

import java.io.IOException;
import java.net.URL;

import static org.cs5431_client.view.Client.printNonFatalError;

public class FileViewCellController {
    @FXML
    private HBox hBox;

    @FXML
    private ImageView imgFile;

    @FXML
    private Label fileName;

    @FXML
    private Label lastModified;

    @FXML
    private Label size;


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
        fileName.setText(fso.getFileName());
        lastModified.setText(fso.getLastModified().toString());
        size.setText(Integer.toString(fso.getFileSize()));
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
