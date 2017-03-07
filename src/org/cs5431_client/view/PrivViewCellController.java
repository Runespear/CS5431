package org.cs5431_client.view;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.PrivType;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.cs5431_client.view.Client.printNonFatalError;

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
                boolean success;
                if (newPropertyValue) {
                    success = fileController.addPriv(fso, PrivType.EDIT);
                } else {
                    success = fileController.removePriv(fso, PrivType.EDIT);
                }
                //TODO: handle failure of changing of privilege
                /*if (!success) {
                    //TODO change to alert box?
                    System.out.println("FAILED");
                    canEdit.setSelected(oldPropertyValue);
                }*/
            });

        canView.selectedProperty().addListener(
            (arg0, oldPropertyValue, newPropertyValue) -> {
                boolean success;
                if (newPropertyValue) {
                    success = fileController.addPriv(fso, PrivType.VIEW);
                } else {
                    success = fileController.removePriv(fso, PrivType.VIEW);
                }
                //TODO: handle failure of changing of privilege
                /*if (!success) {
                    //TODO change to alert box?
                    System.out.println("FAILED");
                    canView.setSelected(oldPropertyValue);
                }*/
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
