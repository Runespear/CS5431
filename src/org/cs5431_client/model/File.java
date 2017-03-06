package org.cs5431_client.model;

public class File extends FileSystemObject {
    //TODO: THIS SHOULD NOT BE A STRING
    private String fileContents;

    //TODO: populate from database instead?
    public File(String name, Folder parentFolder, long size, String fileContents) {
        super(name, parentFolder);
        this.size = size;
        this.fileContents = fileContents;
        //remove this type?
        this.type = FSOType.FILE;
        //TODO: date modified set by DB timestamp
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
    }

    public String getFileContents() {return fileContents; }


}

