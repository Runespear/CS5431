package org.cs5431;

import java.io.Serializable;

/**
 * Created by Brandon on 27/3/2017.
 */
public class TransmittedFile implements Serializable{
    private static final long serialVersionUID = 4229824720221248174L;
    public byte[] signature;
    public byte[] file;
    public String filename;
}
