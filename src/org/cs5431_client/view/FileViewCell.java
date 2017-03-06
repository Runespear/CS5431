package org.cs5431_client.view;

import javafx.scene.control.ListCell;
import org.cs5431_client.model.FileSystemObject;

public class FileViewCell extends ListCell<FileSystemObject> {

    @Override
    public void updateItem(FileSystemObject fso, boolean empty)
    {
        super.updateItem(fso,empty);
        if(fso != null)
        {
            FileViewCellController fvcControl = new FileViewCellController();
            fvcControl.setInfo(fso);
            setGraphic(fvcControl.getBox());
        }
    }
}
