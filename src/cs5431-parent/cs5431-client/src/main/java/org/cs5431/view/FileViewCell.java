package org.cs5431.view;

import javafx.scene.control.ListCell;
import org.cs5431.controller.FileController;
import org.cs5431.model.FileSystemObject;

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
