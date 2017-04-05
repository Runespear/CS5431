package org.cs5431;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;

import static org.cs5431.Constants.DEBUG_MODE;

public class Encryption {
    private static byte[] pwdBasedHash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 3000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters
                (128);
        return kp.getKey();
    }

    public static byte[][] pwdBasedKey(String pwd, byte[] salt) {
        byte[] hashedPW = pwdBasedHash(pwd, salt);
        if (DEBUG_MODE) {
            System.out.println("On Client side: key generated from pwd and salt:");
            System.out.println(Base64.getEncoder().encodeToString(hashedPW));
        }
        byte returnedValues[][] = new byte[2][128];
        returnedValues[0] = hashedPW;
        returnedValues[1] = salt;
        return returnedValues;
    }

    public static String secondPwdHash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 10000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters(256);
        return Base64.getEncoder().encodeToString(kp.getKey());
    }

    public static byte[] SHA256(String msg) {
        SHA256Digest sha256 = new SHA256Digest();
        byte msgByte[] = msg.getBytes();
        sha256.update(msgByte, 0, msgByte.length);
        Arrays.fill(msgByte, (byte)0 );    //an attempt to zero out pwd
        byte[] hashedPwd = new byte[sha256.getDigestSize()];
        sha256.doFinal(hashedPwd, 0);
        return hashedPwd;
    }

    public static byte[] signCert(byte[] message, PrivateKey key) throws
            Exception{
        //TODO don't throw exception
        //SHA-256 hashing algorithm with RSA, getting the signature object
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign(key); //initializing the private key
        dsa.update(message);
        byte[] signature  = dsa.sign();
        return signature;
    }

}
