package org.cs5431_client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.cs5431_client.model.FileSystemObject;

import java.io.IOException;
import java.net.URL;

import static org.cs5431_client.view.Client.printNonFatalError;

public class FileViewCellController {
    @FXML
    private HBox hBox;

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
    }

    public HBox getBox()
    {
        return hBox;
    }
}
