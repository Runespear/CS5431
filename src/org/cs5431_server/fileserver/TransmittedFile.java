package org.cs5431_server.fileserver;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Brandon on 27/3/2017.
 */
public class TransmittedFile implements Serializable{
    private static final long serialVersionUID = 4229824720221248174L;
    protected byte[] encrypted_Stuff;
    public byte[] signature;
    protected byte[] mac_Stuff;
    public byte[] file;
    public String filename;
    public String jsonString;
}
