package Validator_Tests;

import org.cs5431.Encryption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

//TODO: Use method generateUserKeys to test public keys, private keys and salt for password encryption
class test_Encryption {

    protected String getRandomString(int Length, int seed) {
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890,./;'[]-=~!@#$%^&*()_+";
        StringBuilder rs = new StringBuilder();
        Random random = new Random(seed);
        while (rs.length() < Length) {
            int index = (int) (random.nextFloat() * CHARS.length());
            rs.append(CHARS.charAt(index));
        }
        String randStr = rs.toString();
        //String[] strArray = new String[] {randStr};
        return randStr;

    }

    @BeforeAll
    public static void setUp() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }




    @Test
    void test_IVgeneration(){ //Just need to ensure that the returned results are not the same

        IvParameterSpec previous = Encryption.generateIV();
        for (int i =0; i<100; i++) {
            IvParameterSpec new_iv = Encryption.generateIV();
            assertNotEquals(previous,new_iv);
            previous = new_iv;
        }

    }

    @Test
    void test_secretkeygeneration() throws Exception{ //Just need to ensure that the returned results are not the same

        SecretKey previous = Encryption.generateSecretKey();
        for (int i =0; i<100; i++) {
            SecretKey new_key = Encryption.generateSecretKey();
            assertNotEquals(previous,new_key);
            previous = new_key;
        }
    }

    @Test
    void test_encFile() throws Exception{ //Test to ensure that same encrypted file is encrypted differently
        File file = new File("./test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] one = Encryption.encryptFile(file,key,iv);
        byte[] two = Encryption.encryptFile(file,key,iv);
        assertNotEquals(one,two);
        file.delete();
    }

    @Test
    public void test_encFileWithInvalidKey()throws Exception{ //Checking if encryption fails with invalid key(diff alg, diff size)
        KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256"); //generating key with different algorithm
        kg.init(128, new SecureRandom());
        SecretKey key = kg.generateKey();

        SecretKey correctkey = Encryption.generateSecretKey();

        PrintWriter writer = new PrintWriter("test.txt", "UTF-8");
        File file = new File("./test.txt");
        IvParameterSpec iv = Encryption.generateIV();
        byte[] one = Encryption.encryptFile(file,correctkey,iv);
        byte[] two = Encryption.encryptFile(file,key,iv);
        assertNotEquals(one,two);

        try {
            KeyGenerator kg2 = KeyGenerator.getInstance("AES"); //generating key with different size
            kg2.init(256, new SecureRandom());
            SecretKey key2 = kg2.generateKey();

            IvParameterSpec iv2 = Encryption.generateIV();
            byte[] cannot_be_encrypted = Encryption.encryptFile(file, key2, iv2);
            fail("InvalidKeyException should be thrown");
        }catch(InvalidKeyException anInvalidKeyException){
        }
    }

    @Test
    void test_encFileName() throws Exception{ //Test to ensure that same encrypted file name is encrypted differently
        String randomFileName = getRandomString(10, 2000);
        SecretKey correctkey = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] one = Encryption.encryptFileName(randomFileName,correctkey,iv);
        byte[] two = Encryption.encryptFileName(randomFileName,correctkey,iv);
        assertNotEquals(one,two);

    }

    @Test
    void test_encFileNameWithInvalidKey(){ //Checking if encryption fails with invalid key

    }


    @Test
    void test_encFileSecretKey(){ //Test to ensure that same key is encrypted differently

    }

    @Test
    void test_encFileSecretKeyWithInvalidKey(){ //Checking if encryption fails with invalid key

    }

    @Test
    void test_decFile(){ //Test to ensure that the file is encrypted and decrypted correctly

    }

    @Test
    void test_decFileWithWrongKey(){ //Test to ensure that the file cannot be decrypted with wrong key

    }

    @Test
    void test_decFileName(){ //Test to ensure that the file name is encrypted and decrypted correctly

    }

    @Test
    void test_decFileNameWithWrongKey(){ //Test to ensure that the file name cannot be decrypted with wrong key

    }

    @Test
    void test_decFileSecretKey(){ //Test to ensure that the file secret key is encrypted and decrypted correctly

    }

    @Test
    void test_decFileSecretKeyWithWrongKey(){ //Test to ensure that the file secret key cannot be decrypted with wrong key

    }

    @Test
    void test_sameHashedPassword(){//Check whether pwdBasedHash gives same result with same salt and password

    }

    @Test
    void test_diffHashedPassword(){//Check whether pwdBasedHash gives diff result with 3 diff cases: 1. same salt, diff password, 2. diff salt, same password, 3. diff salt, diff password

    }

    @Test
    void test_generatedSalt(){//Salts should be unique
        byte[] previous = Encryption.newPwdSalt();
        for (int i =0; i<100; i++) {
            byte[] new_salt = Encryption.newPwdSalt();
            assertNotEquals(previous,new_salt);
            previous = new_salt;
        }
    }

    @Test
    void test_samePasswordBasedKey(){//Check whether pwdBasedKey gives same result with same salt and password

    }

    @Test
    void test_diffPasswordBasedKey(){//Check whether pwdBasedKey gives diff result with 3 diff cases: 1. same salt, diff password, 2. diff salt, same password, 3. diff salt, diff password

    }

    @Test
    void test_sameSecondHashedPassword(){//Check whether secondPwdHash gives same result with same salt and password

    }

    @Test
    void test_diffSecondHashedPassword(){//Check whether secondPwdHash gives diff result with 3 diff cases: 1. same salt, diff password, 2. diff salt, same password, 3. diff salt, diff password

    }

    @Test
    void test_SHA256Length(){//Checking if the length is correct

    }

    @Test
    void test_diffSHA256(){//Checking if the results for different SHA256 are different

    }

    //TODO: NO METHOD TO CHECK SIGNED CERT?

    @Test
    void test_encPrivateKey(){//Test to ensure that same encrypted key is encrypted differently under same password and salt with same/different key

    }

    @Test
    void test_encPrivateKey2(){//Test to ensure that same encrypted key is encrypted differently under different password and same salt with same/different key

    }

    @Test
    void test_encPrivateKey3(){//Test to ensure that same encrypted key is encrypted differently under same password and different salt with same/different key

    }

    @Test
    void test_encPrivateKey4(){//Test to ensure that same encrypted key is encrypted differently under different password and different salt with same/different key

    }

    @Test
    void test_encPrivateKeyWithInvalidKey(){//Test to ensure invalid key cannot be used to encrypt

    }

    @Test
    void test_decPwdBasedKey(){// does it decrypt correctly with correct password and salt?

    }

    @Test
    void test_decPwdBasedKey2(){// does it decrypt with wrong password and correct salt?

    }

    @Test
    void test_decPwdBasedKey3(){// does it decrypt with correct password and wrong salt?

    }

    @Test
    void test_decPwdBasedKey4(){// does it decrypt with wrong password and wrong salt?

    }

    //TODO: TEST generateAndEncFile and generateAndEncFileName??
}