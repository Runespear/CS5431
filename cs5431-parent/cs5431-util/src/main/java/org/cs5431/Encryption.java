package org.cs5431;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Base64;

public class Encryption {
    public static String hash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 10000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters(256);
        return Base64.getEncoder().encodeToString(kp.getKey());
    }

}
