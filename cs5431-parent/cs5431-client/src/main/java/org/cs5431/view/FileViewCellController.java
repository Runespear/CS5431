package org.cs5431.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import org.cs5431.controller.FileController;
import org.cs5431.model.File;
import org.cs5431.model.FileSystemObject;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import static org.cs5431.view.Client.printNonFatalError;

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
    private boolean isEditor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
            fileName.setOnMouseClicked(click -> {
                if (click.getButton() == MouseButton.PRIMARY &&
                        click.getClickCount() == 2 && isEditor) {
                    renameFile();
                    click.consume();
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
                lastModified.requestFocus();
            }
        });

        renameBox.focusedProperty().addListener(
            (arg0, oldPropertyValue, newPropertyValue) -> {
            if (!newPropertyValue) {
                performRename(renameBox, currLabel);
            }
        });
        renameBox.requestFocus();
        renameBox.selectAll();
    }

    private void performRename(TextField renameBox, Label currLabel) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fileController.rename(fso,renameBox.getCharacters().toString());
                return null;
            }
        };
        task.setOnSucceeded(t -> {
            fso.rename(renameBox.getCharacters().toString());
            currLabel.setText(renameBox.getCharacters().toString());
            nameBox.getChildren().remove(0);
            nameBox.getChildren().add(0, currLabel);
        });
        try {
                    Client.exec.submit(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }

        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
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
        this.isEditor = fso.isEditor();
        fileName.setText(fso.getFileName());
        lastModified.setText(fso.getLastModified().toString());
        size.setText(formatSize(fso.getFileSize()));
        //default image is file, so if it's folder change img to folder
        if (fso instanceof File)
            imgFile.setImage(new Image(getClass().getResource(
                    "/org/cs5431/assets/ic_file.png").toExternalForm()));
        else
            imgFile.setImage(new Image(getClass().getResource(
                    "/org/cs5431/assets/ic_folder.png").toExternalForm()));
        imgFile.setPreserveRatio(true);
        imgFile.setFitHeight(20);
    }

    /**
     * Prints the number of bytes of a file in a human readable format
     * @param size Number of bytes to be formatted
     * @return A human readable string of the number of bytes (e.g. "10 MB")
     */
    public String formatSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public HBox getBox()
    {
        return hBox;
    }
}
