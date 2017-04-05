package org.cs5431.view;

import javafx.scene.control.ListCell;
import org.cs5431.controller.FileController;
import org.cs5431.model.FileSystemObject;
import org.cs5431.model.PrivType;

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
        boolean success = fileController.removePriv(fso, -1, PrivType.EDIT);
        success &= fileController.removePriv(fso, -1, PrivType.VIEW);
        //TODO check for success when hooked up to server
        //if (success) {
            getListView().getItems().remove(getItem());
        //}
    }
}
