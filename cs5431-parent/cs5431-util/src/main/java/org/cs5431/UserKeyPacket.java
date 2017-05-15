package org.cs5431;

import java.security.KeyPair;

public class UserKeyPacket {
    public KeyPair keyPair;
    public String encPrivKey;
    public byte[] salt;
}
