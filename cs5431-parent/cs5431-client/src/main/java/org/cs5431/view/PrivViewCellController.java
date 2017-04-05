package org.cs5431.view;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.cs5431.controller.FileController;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.PrivType;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.cs5431.view.Client.printNonFatalError;

public class PrivViewCellController implements Initializable {
    @FXML
    private HBox hBox;

    @FXML
    private Label username;

    @FXML
    private CheckBox canEdit;

    @FXML
    private CheckBox canView;

    @FXML
    ImageView imgDelete;

    private FileController fileController;
    private FileSystemObject fso;
    private int userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canEdit.selectedProperty().addListener(
            (arg0, oldPropertyValue, newPropertyValue) -> {
                try {
                    boolean success;
                    if (newPropertyValue) {
                        success = fileController.addPriv(fso, -1, PrivType.EDIT);

                    } else {
                        success = fileController.removePriv(fso, -1, PrivType.EDIT);
                    }
                    //TODO: handle failure of changing of privilege
                /*if (!success) {
                    //TODO change to alert box?
                    System.out.println("FAILED");
                    canEdit.setSelected(oldPropertyValue);
                }*/
                } catch (IOException | ClassNotFoundException e) {
                    //TODO catch error?
                    e.printStackTrace();
                }
            });

        canView.selectedProperty().addListener(
            (arg0, oldPropertyValue, newPropertyValue) -> {
                try {
                    boolean success;
                    if (newPropertyValue) {
                        success = fileController.addPriv(fso, -1, PrivType.VIEW);

                    } else {
                        success = fileController.removePriv(fso, -1, PrivType.VIEW);
                    }
                    //TODO: handle failure of changing of privilege
                /*if (!success) {
                    //TODO change to alert box?
                    System.out.println("FAILED");
                    canView.setSelected(oldPropertyValue);
                }*/
                } catch (IOException | ClassNotFoundException e) {
                    //TODO catch error?
                    e.printStackTrace();
                }
            });
    }

    public PrivViewCellController(FileController fileController) {
        this.fileController = fileController;
        final URL r = getClass().getResource("priv_view_cell.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(r);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            printNonFatalError("Failed to load layout");
            e.printStackTrace();
        }
    }

    public void setInfo(PrivBundle privBundle)
    {
        imgDelete.setPreserveRatio(true);
        //TODO: don't hardcode this number?
        imgDelete.setFitHeight(20);

        username.setText(Integer.toString(privBundle.userId));
        userId = privBundle.userId;
        this.fso = privBundle.fso;
        canEdit.setSelected(privBundle.canEdit);
        canView.setSelected(privBundle.canView);
    }


    public HBox getBox()
    {
        return hBox;
    }
}
