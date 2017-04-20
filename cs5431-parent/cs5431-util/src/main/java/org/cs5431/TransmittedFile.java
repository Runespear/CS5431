package org.cs5431;

import java.io.Serializable;

public class TransmittedFile implements Serializable{
    private static final long serialVersionUID = 4229824720221248174L;
    public byte[] signature;
    public byte[] file;
    public String filename;
}
