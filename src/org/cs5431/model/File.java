package org.cs5431.model;

public class File extends FileSystemObject {
    //TODO: THIS SHOULD NOT BE A STRING
    private String fileContents;

    //TODO: populate from database instead?
    public File(String fileContents) {
        this.fileContents = fileContents;
    }


}

