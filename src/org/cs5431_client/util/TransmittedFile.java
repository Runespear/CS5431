import java.io.Serializable;

/**
 * Created by Brandon on 27/3/2017.
 */
public class TransmittedFile implements Serializable{
    private static final long serialVersionUID = 4229824720221248174L;
    protected byte[] encrypted_Stuff;
    protected byte[] signature;
    protected byte[] mac_Stuff;
    protected byte[] file;
    protected String filename;
}
