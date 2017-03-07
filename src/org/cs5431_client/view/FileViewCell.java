package org.cs5431_client.view;

import javafx.scene.control.ListCell;
import org.cs5431_client.controller.FileController;
import org.cs5431_client.model.FileSystemObject;

public class FileViewCell extends ListCell<FileSystemObject> {
    FileController fileController;

    public FileViewCell(FileController fileController) {
        this.fileController = fileController;
    }

    @Override
    public void updateItem(FileSystemObject fso, boolean empty)
    {
        super.updateItem(fso,empty);
        if(fso != null)
        {
            FileViewCellController fvcControl =
                    new FileViewCellController(fileController);
            fvcControl.setInfo(fso);
            setGraphic(fvcControl.getBox());
        }
    }
}
