package org.cs5431_client.view;

import javafx.scene.control.ListCell;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.model.FileSystemObject;
import org.cs5431_client.model.PrivType;

public class PrivViewCell extends ListCell<PrivBundle> {
    FileController fileController;
    FileSystemObject fso;

    public PrivViewCell(FileController fileController, FileSystemObject fso) {
        this.fileController = fileController;
        this.fso = fso;
    }

    @Override
    public void updateItem(PrivBundle privBundle, boolean empty) {
        super.updateItem(privBundle, empty);
        if (privBundle != null) {
            PrivViewCellController pvcControl =
                    new PrivViewCellController(fileController);
            pvcControl.setInfo(privBundle);
            pvcControl.imgDelete.setOnMouseClicked(e -> removeUser());
            setGraphic(pvcControl.getBox());
        }
    }

    private void removeUser() {
        //TODO: how to check success? what if deleted user doesn't have edit
        // priv?
        boolean success = fileController.removePriv(fso, PrivType.EDIT);
        success &= fileController.removePriv(fso, PrivType.VIEW);
        if (success) {
            getListView().getItems().remove(getItem());
        }
    }
}
