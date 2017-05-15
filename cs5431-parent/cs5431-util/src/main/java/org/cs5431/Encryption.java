package org.cs5431;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static org.cs5431.Constants.CAN_KEYS_BE_DESTROYED;
import static org.cs5431.Constants.DEBUG_MODE;

public class Encryption {
    public static byte[] encryptFile(java.io.File file, SecretKey secretKey,
                               IvParameterSpec ivSpec) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException,
            IOException, IllegalBlockSizeException, BadPaddingException {
        FileInputStream inputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[inputStream.available()];
        inputStream.read(fileBytes);
        inputStream.close();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(fileBytes);
    }

    public static byte[] encryptFileName(String fileName, SecretKey secretKey,
                                   IvParameterSpec ivSpec) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(fileName.getBytes());
    }

    public static byte[] encFileSecretKey (SecretKey secretKey, PublicKey
            userPubKey) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        Cipher cipher = Cipher.getInstance
                ("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, userPubKey);
        return cipher.doFinal(secretKey.getEncoded());
    }

    public static boolean decryptFile(byte[] encFile, String fileName,
                                SecretKey secretKey, IvParameterSpec ivSpec,
                                java.io.File directory)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] fileDec = cipher.doFinal(encFile);
        java.io.File fileToWrite = new java.io.File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(fileToWrite);
        fos.write(fileDec);
        fos.close();
        return true;
    }

    public static String decryptFileName(byte[] encFileName, SecretKey fileSK,
                                   IvParameterSpec ivSpec) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, fileSK, ivSpec);
        byte fileName[] = cipher.doFinal(encFileName);
        return new String(fileName);
    }

    public static SecretKey decFileSecretKey(byte[] encSK, PrivateKey
            userPrivKey) throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException{
        Cipher cipher = Cipher.getInstance
                ("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, userPrivKey);
        byte SKbytes[] = cipher.doFinal(encSK);
        return new SecretKeySpec(SKbytes, 0, SKbytes.length, "AES");
    }

    public static byte[] pwdBasedHash(String pwd, byte[] salt) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                pwd.toCharArray()), salt, 3000);
        //TODO: 256 is currently the key length. Is this correct?
        KeyParameter kp = (KeyParameter) generator.generateDerivedParameters
                (128);
        return kp.getKey();
    }

    public static byte[] newPwdSalt() {
        Random random = new SecureRandom();
        //TODO: 32 is currently the salt length. Is this correct?
        byte salt[] = new byte[32];
        random.nextBytes(salt);
        return salt;
    }

    //[0] is the hashed password, [1] is the salt
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
            NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //SHA-256 hashing algorithm with RSA, getting the signature object
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign(key); //initializing the private key
        dsa.update(message);
        return dsa.sign();
    }

    public static byte[] signJKS(byte[] message, PrivateKey key) throws
            NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //SHA-256 hashing algorithm with RSA, getting the signature object
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign(key); //initializing the private key
        dsa.update(message);
        return dsa.sign();
    }

    //[0] is Base64 encoded public key, [1] is Base64 encoded password
    // encrypted private key, [2] is salt used for password based encryption
    public static String[] generateUserKeys(String password) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        String values[] = new String[3];
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new
                BouncyCastleProvider());
        kpg.initialize(4096, new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        values[0] = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        //encrypt secret key using password based key
        //symmetric, uses AES
        byte salt[] = newPwdSalt();
        values[1] = encryptPrivateKey(password, keyPair.getPrivate()
                .getEncoded(), salt);
        values[2] = Base64.getEncoder().encodeToString(salt);
        return values;
    }

    public static String encryptPrivateKey(String password, byte[]
            privKeyByte, byte[] privKeySalt) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        byte keyAndSalt[][] = pwdBasedKey(password, privKeySalt);
        byte key[] = keyAndSalt[0];
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte encryptedKey[] = cipher.doFinal(privKeyByte);
        if (CAN_KEYS_BE_DESTROYED) {
            try {
                secretKey.destroy();
            } catch (DestroyFailedException e) {
                e.printStackTrace();
            }
        }
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    public static PublicKey getPubKeyFromJSON(String encodedPubKey) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decodedPub = Base64.getDecoder().decode(encodedPubKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new X509EncodedKeySpec(decodedPub));
    }

    public static PrivateKey getPrivKeyFromJSON(String encodedPrivKey, String
            privKeySalt, String password)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeySpecException, InvalidKeyException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        byte[] decodedPriv = decryptPwdBasedKey(encodedPrivKey,
                password, privKeySalt);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(decodedPriv));
    }

    private static byte[] decryptPwdBasedKey(String enc, String pwd, String
            salt) throws NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        byte key[] = pwdBasedHash(pwd, Base64.getDecoder().decode(salt));
        if (DEBUG_MODE) {
            System.out.println("From server: key generated from pwd and salt:");
            System.out.println(Base64.getEncoder().encodeToString(key));
        }
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        byte privKey[] =  cipher.doFinal(Base64.getDecoder().decode(enc));
        if (CAN_KEYS_BE_DESTROYED) {
            try {
                secretKey.destroy();
            } catch (DestroyFailedException e) {
                e.printStackTrace();
            }
        }
        return privKey;
    }

    public static EncFilePacket generateAndEncFile(File file, String name, PublicKey
            editorsKeys[], PublicKey viewersKeys[]) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, IOException {
        EncFilePacket efp = new EncFilePacket();
        SecretKey fileSK = generateSecretKey();
        IvParameterSpec fileIVSpec = generateIV();
        IvParameterSpec fsoNameIVSpec = generateIV();

        byte[] encFile = encryptFile(file, fileSK, fileIVSpec);
        efp.encFile = Base64.getEncoder().encodeToString(encFile);
        efp.encFileName = Base64.getEncoder().encodeToString(encryptFileName(name,fileSK, fsoNameIVSpec));
        List<String> editorsSK = new ArrayList<>();
        for (PublicKey editorsKey : editorsKeys)
            editorsSK.add(Base64.getEncoder().encodeToString(encFileSecretKey(fileSK, editorsKey)));
        efp.editorsFileSK = editorsSK;
        List<String> viewersSK = new ArrayList<>();
        for (PublicKey viewersKey : viewersKeys)
            viewersSK.add(Base64.getEncoder().encodeToString(encFileSecretKey(fileSK, viewersKey)));
        efp.viewersFileSK = viewersSK;
        efp.fileIV = Base64.getEncoder().encodeToString(fileIVSpec.getIV());
        efp.fsoNameIV = Base64.getEncoder().encodeToString(fsoNameIVSpec.getIV());
        if (CAN_KEYS_BE_DESTROYED) {
            try {
                fileSK.destroy();
            } catch (DestroyFailedException e) {
                e.printStackTrace();
            }
        }
        return efp;
    }

    public static EncFilePacket generateAndEncFileName(String folderName, PublicKey
            editorsKeys[], PublicKey viewersKeys[]) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        EncFilePacket efp = new EncFilePacket();

        SecretKey fileSK = generateSecretKey();
        IvParameterSpec ivSpec = generateIV();
        efp.fsoNameIV = Base64.getEncoder().encodeToString(ivSpec.getIV());
        efp.encFileName = Base64.getEncoder().encodeToString
                (encryptFileName(folderName,fileSK, ivSpec));
        List<String> editorsSK = new ArrayList<>();
        for (PublicKey editorsKey : editorsKeys)
            editorsSK.add(Base64.getEncoder().encodeToString(encFileSecretKey(fileSK, editorsKey)));
        efp.editorsFileSK = editorsSK;
        List<String> viewersSK = new ArrayList<>();
        for (PublicKey viewersKey : viewersKeys)
            viewersSK.add(Base64.getEncoder().encodeToString(encFileSecretKey(fileSK, viewersKey)));
        efp.viewersFileSK = viewersSK;

        if (CAN_KEYS_BE_DESTROYED) {
            try {
                fileSK.destroy();
            } catch (DestroyFailedException e) {
                e.printStackTrace();
            }
        }
        return efp;
    }

    public static SecretKey generateSecretKey() throws
            NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128, new SecureRandom());
        return kg.generateKey();
    }

    public static IvParameterSpec generateIV() {
        SecureRandom random = new SecureRandom();
        byte iv[] = new byte[16];
        random.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    //[0] is the new IV, [1] is the encrypted file
    public static String[] reEncryptFile(File file, SecretKey sk) throws
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException,
            IOException, IllegalBlockSizeException, BadPaddingException {
        //TODO BRANDON! I MADE THIS NEW METHOD!
        String ret[] = new String[2];
        IvParameterSpec iv = generateIV();
        byte[] encryptedFile = encryptFile(file, sk, iv);
        ret[0] = Base64.getEncoder().encodeToString(iv.getIV());
        ret[1] = Base64.getEncoder().encodeToString(encryptedFile);
        return ret;
    }

    //[0] is the new IV, [1] is the encrypted file name
    public static String[] reEncryptFileName(String fileName, SecretKey sk)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException,
            IOException, IllegalBlockSizeException, BadPaddingException {
        //TODO BRANDON! I MADE THIS NEW METHOD!
        String ret[] = new String[2];
        IvParameterSpec iv = generateIV();
        byte[] encFileName = encryptFileName(fileName, sk, iv);
        ret[0] = Base64.getEncoder().encodeToString(iv.getIV());
        ret[1] = Base64.getEncoder().encodeToString(encFileName);
        return ret;
    }

    public static String[] generatePasswordHash(String pwd) {
        byte salt[] = newPwdSalt();
        String hashedPW = secondPwdHash(pwd, salt);
        String returnedValues[] = new String[2];
        returnedValues[0] = hashedPW;
        returnedValues[1] = Base64.getEncoder().encodeToString(salt);
        return returnedValues;
    }

    public static List<String> encryptSecrets(List<PublicKey> publicKeys, List<String> plainSecrets) {
        //TODO: encrypt
        return null;
    }

    public static BigInteger decryptSecret(PrivateKey privateKey, String code) {
        //TODO decrypt what's sent in the email
        return null;
    }
}
