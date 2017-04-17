package Validator_Tests;

import org.cs5431.Encryption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

//TODO: Use method generateUserKeys to test public keys, private keys and salt for password encryption
class test_Encryption {

    public static PublicKey correctPublicKey;
    public static PrivateKey correctPrivateKey;
    public static PublicKey wrongPublicKeySize;
    public static PublicKey wrongPublicKeyAlgoAndSize;
    public static PrivateKey wrongPrivateKeySize;
    public static PrivateKey wrongPrivateKeyAlgoAndSize;


    public static String getRandomString(int Length, int seed) {
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

    public static void getPrivPubKeyPair() throws Exception{
        SecureRandom random = new SecureRandom();//generating public key
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA" , "BC");
        keygen.initialize(4096,random);
        KeyPair keypair = keygen.generateKeyPair();
        correctPublicKey = keypair.getPublic();
        correctPrivateKey = keypair.getPrivate();


        SecureRandom random2 = new SecureRandom();//generating public key of wrong size
        KeyPairGenerator keygen2 = KeyPairGenerator.getInstance("RSA", "BC");
        keygen2.initialize(1024,random2);
        KeyPair keypair2 = keygen2.generateKeyPair();
        wrongPublicKeyAlgoAndSize = keypair2.getPublic();
        wrongPrivateKeyAlgoAndSize = keypair2.getPrivate();

        SecureRandom random3 = new SecureRandom();//generating public/private key of wrong algo and size
        KeyPairGenerator keygen3 = KeyPairGenerator.getInstance("DSA");
        keygen3.initialize(1024,random3);
        KeyPair keypair3 = keygen3.generateKeyPair();
        wrongPublicKeySize = keypair3.getPublic();
        wrongPrivateKeySize = keypair3.getPrivate();
    }

    @BeforeAll
    public static void setUp() throws Exception{
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        PrintWriter pw = new PrintWriter(new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt"));
        Random random = new Random();
        pw.println(getRandomString(10,random.nextInt()));
        pw.close();
        getPrivPubKeyPair();
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
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] one = Encryption.encryptFile(file,key,iv);
        byte[] two = Encryption.encryptFile(file,key,iv);
        assertNotEquals(one,two);

    }

    @Test
    public void test_encFileWithInvalidKey()throws Exception{ //Checking if encryption fails with invalid key(diff alg, diff size)
        KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256"); //generating key with different algorithm but correct size
        kg.init(128, new SecureRandom());
        SecretKey key = kg.generateKey();

        SecretKey correctkey = Encryption.generateSecretKey();


        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        IvParameterSpec iv = Encryption.generateIV();
        byte[] one = Encryption.encryptFile(file,correctkey,iv);
        byte[] two = Encryption.encryptFile(file,key,iv);
        assertNotEquals(one,two);

        try {
            KeyGenerator kg2 = KeyGenerator.getInstance("AES"); //generating key with different size but correct algo
            kg2.init(256, new SecureRandom());
            SecretKey key2 = kg2.generateKey();

            IvParameterSpec iv2 = Encryption.generateIV();
            byte[] cannot_be_encrypted = Encryption.encryptFile(file, key2, iv2);
            fail("InvalidKeyException should be thrown");

            KeyGenerator kg3 = KeyGenerator.getInstance("DES"); //generating key with different size and different algo
            kg2.init(56, new SecureRandom());
            SecretKey key3 = kg3.generateKey();

            IvParameterSpec iv3 = Encryption.generateIV();
            byte[] cannot_be_encrypted2 = Encryption.encryptFile(file, key3, iv3);
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
    void test_encFileNameWithInvalidKey() throws Exception{ //Checking if encryption fails with invalid key(diff alg, diff size)
        KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256"); //generating key with different algorithm but correct size
        kg.init(128, new SecureRandom());
        SecretKey key = kg.generateKey();

        SecretKey correctkey = Encryption.generateSecretKey();

        String file = "filename";
        IvParameterSpec iv = Encryption.generateIV();
        byte[] one = Encryption.encryptFileName(file,correctkey,iv);
        byte[] two = Encryption.encryptFileName(file,key,iv);
        assertNotEquals(one,two);

        try {
            KeyGenerator kg2 = KeyGenerator.getInstance("AES"); //generating key with different size but correct algo
            kg2.init(256, new SecureRandom());
            SecretKey key2 = kg2.generateKey();

            IvParameterSpec iv2 = Encryption.generateIV();
            byte[] cannot_be_encrypted = Encryption.encryptFileName(file, key2, iv2);
            fail("InvalidKeyException should be thrown");

            KeyGenerator kg3 = KeyGenerator.getInstance("DES"); //generating key with different size and different algorithm
            kg2.init(56, new SecureRandom());
            SecretKey key3 = kg3.generateKey();

            IvParameterSpec iv3 = Encryption.generateIV();
            byte[] cannot_be_encrypted2 = Encryption.encryptFileName(file, key3, iv3);
            fail("InvalidKeyException should be thrown");
        }catch(InvalidKeyException anInvalidKeyException){
        }
    }


    @Test
    void test_encFileSecretKey() throws Exception{ //Test to ensure that same key is encrypted differently
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //generating secret key
        kg.init(128, new SecureRandom());
        SecretKey secretkey = kg.generateKey();

        /*
        SecureRandom random = new SecureRandom();//generating public key
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA" , "BC");
        keygen.initialize(4096,random);
        KeyPair keypair = keygen.generateKeyPair();
        PublicKey pubKey = keypair.getPublic();
        */

        byte[] one = Encryption.encFileSecretKey(secretkey,correctPublicKey);
        byte[] two = Encryption.encFileSecretKey(secretkey,correctPublicKey);
        assertNotEquals(one,two);
    }

    @Test
    void test_encFileSecretKeyWithInvalidKey()throws Exception{ //Checking if encryption fails with invalid key(diff algo, diff size)
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //generating secret key
        kg.init(128, new SecureRandom());
        SecretKey secretkey = kg.generateKey();

        /*
        //generating correct public key
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA" , "BC");
        keygen.initialize(4096, new SecureRandom());
        KeyPair correctkeypair = keygen.generateKeyPair();
        PublicKey correctPubKey = correctkeypair.getPublic();
        */

       //generating public key with different algorithm and different size
        KeyPairGenerator keygen2 = KeyPairGenerator.getInstance("DSA");
        keygen2.initialize(1024,new SecureRandom());
        KeyPair keypair = keygen2.generateKeyPair();
        PublicKey pubKey = keypair.getPublic();

        byte[] one = Encryption.encFileSecretKey(secretkey,correctPublicKey);
        try {
            byte[] two = Encryption.encFileSecretKey(secretkey, pubKey);
            assertNotEquals(one,two);
            fail("This is supposed to throw an exception for wrong key");
        }catch(Exception e){
        }


        //generating public key with correct algorithm but different size
        KeyPairGenerator keygen3 = KeyPairGenerator.getInstance("RSA");
        keygen3.initialize(1024,new SecureRandom());
        KeyPair keypair3 = keygen3.generateKeyPair();
        PublicKey pubKey2 = keypair3.getPublic();

        byte[] third = Encryption.encFileSecretKey(secretkey,pubKey2);
        assertNotEquals(one,third);

    }

    @Test
    void test_decFile()throws Exception{ //Test to ensure that the file is encrypted and decrypted correctly
        //Encrypt file here
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFile = Encryption.encryptFile(file,key,iv);

        String filename = "test.txt";
        File directory = new File("./Encryption_Test_Folder/Decrypted_Stuff/");
        boolean saved = Encryption.decryptFile(encryptedFile,filename,key,iv,directory);
        assertEquals(saved,true);


        String path = "./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt";
        FileInputStream inputStream = new FileInputStream(path);
//        byte[] originalTxt = new byte[inputStream.available()];
//        inputStream.read(originalTxt);
//        inputStream.close();
        BufferedReader myInput = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder originalTxt = new StringBuilder();
        String thisLine;
        while ((thisLine = myInput.readLine()) != null) {
            originalTxt.append(thisLine);
        }

        String path2 = "./Encryption_Test_Folder/Decrypted_Stuff/test.txt";
        FileInputStream inputStream2 = new FileInputStream(path2);
//        byte[] decryptedTxt = new byte[inputStream2.available()];
//        inputStream2.read(decryptedTxt);
//        inputStream2.close();
        BufferedReader myInput2 = new BufferedReader(new InputStreamReader(inputStream2));
        StringBuilder decryptedTxt = new StringBuilder();
        String thisLine2;
        while ((thisLine2 = myInput2.readLine()) != null) {
           decryptedTxt.append(thisLine2);
        }

        File file_to_delete = new File("./Encryption_Test_Folder/Decrypted_Stuff/test.txt");
        Boolean isittrue = file_to_delete.delete();
        System.out.println(isittrue);
        assertEquals(originalTxt.toString().equals(decryptedTxt.toString()), true);
    }

    @Test
    void test_decFileWithWrongKey()throws Exception{ //Test to ensure that the file cannot be decrypted with wrong key
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFile = Encryption.encryptFile(file,key,iv);


        //Generating a different key for decryption
        SecretKey wrongKey = Encryption.generateSecretKey();
        String filename = "test.txt";
        File directory = new File("./Encryption_Test_Folder/Decrypted_Stuff/");
        try {
            boolean saved = Encryption.decryptFile(encryptedFile, filename, wrongKey, iv, directory);
            assertEquals(saved,true);
            fail("Error Message on Wrong key used should be displayed");
        }catch(Exception e){
        }
    }

    @Test
    void test_decFileWithWrongIV()throws Exception{ //Test to ensure that the file cannot be decrypted with wrong IV
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFile = Encryption.encryptFile(file,key,iv);


        //Generating a different IV for decryption
        IvParameterSpec wrongIV = Encryption.generateIV();
        String filename = "test.txt";
        File directory = new File("./Encryption_Test_Folder/Decrypted_Stuff/");
        try {
            boolean saved = Encryption.decryptFile(encryptedFile, filename, key, wrongIV, directory);
            assertEquals(saved,true);
            fail("Error Message on wrong IV used should be displayed");
        }catch(Exception e){
    }
    }

    @Test
    void test_decFileWithWrongIVKEY()throws Exception{ //Test to ensure that the file cannot be decrypted with wrong IV and wrong key
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFile = Encryption.encryptFile(file,key,iv);

        //Generating a different IV for decryption
        IvParameterSpec wrongIV = Encryption.generateIV();

        //Generating a different key for decryption
        SecretKey wrongKey = Encryption.generateSecretKey();

        String filename = "test.txt";
        File directory = new File("./Encryption_Test_Folder/Decrypted_Stuff/");
        try {
            boolean saved = Encryption.decryptFile(encryptedFile, filename, wrongKey, wrongIV, directory);
            assertEquals(saved,true);
            fail("Error Message on wrong IV or wrong key used should be displayed");
        }catch(Exception e){
        }
    }

    @Test
    void test_decFileName() throws Exception{ //Test to ensure that the file name is encrypted and decrypted correctly
        String filename = "test.txt";
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFileName  = Encryption.encryptFileName(filename,key,iv);
        String decryptedFileName = Encryption.decryptFileName(encryptedFileName, key, iv);
        assertEquals(filename.equals(decryptedFileName),true);
    }

    @Test
    void test_decFileNameWithWrongKey()throws Exception{ //Test to ensure that the file name cannot be decrypted with wrong key
        String filename = "test.txt";
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFileName  = Encryption.encryptFileName(filename,key,iv);
        SecretKey wrongkey = Encryption.generateSecretKey();
        try {
            String decryptedFileName = Encryption.decryptFileName(encryptedFileName, wrongkey, iv);
            assertEquals(filename.equals(decryptedFileName),true);
            fail("Wrong key error should be displayed");
        }catch (Exception e){
        }
    }

    @Test
    void test_decFileNameWithWrongIV()throws Exception{ //Test to ensure that the file name cannot be decrypted with wrong IV
        String filename = "test.txt";
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFileName  = Encryption.encryptFileName(filename,key,iv);
        IvParameterSpec wrongiv = Encryption.generateIV();
        try {
            String decryptedFileName = Encryption.decryptFileName(encryptedFileName, key, wrongiv);
            assertEquals(filename.equals(decryptedFileName),true);
            fail("Wrong IV error should be displayed");
        }catch (Exception e){
        }
    }

    @Test
    void test_decFileNameWithWrongIVKEY()throws Exception{ //Test to ensure that the file name cannot be decrypted with wrong IV and key
        String filename = "test.txt";
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        byte[] encryptedFileName  = Encryption.encryptFileName(filename,key,iv);
        IvParameterSpec wrongiv = Encryption.generateIV();
        SecretKey wrongkey = Encryption.generateSecretKey();
        try {
            String decryptedFileName = Encryption.decryptFileName(encryptedFileName, wrongkey, wrongiv);
            assertEquals(filename.equals(decryptedFileName),true);
            fail("Wrong IV and Key error should be displayed");
        }catch (Exception e){
        }
    }

    @Test
    void test_decFileSecretKey() throws Exception{ //Test to ensure that the file secret key is encrypted and decrypted correctly
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //generating secret key
        kg.init(128, new SecureRandom());
        SecretKey secretkey = kg.generateKey();
        byte[] encSK = Encryption.encFileSecretKey(secretkey, correctPublicKey);
        SecretKey decryptedkey = Encryption.decFileSecretKey(encSK,correctPrivateKey);
        assertEquals(decryptedkey, secretkey);
    }

    @Test
    void test_decFileSecretKeyWithWrongKey()throws Exception{ //Test to ensure that the file secret key cannot be decrypted with wrong key(different algo and different size)
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //generating secret key
        kg.init(128, new SecureRandom());
        SecretKey secretkey = kg.generateKey();

        byte[] encSK = Encryption.encFileSecretKey(secretkey, correctPublicKey);
        try {
            SecretKey decryptedkey = Encryption.decFileSecretKey(encSK, wrongPrivateKeyAlgoAndSize);//testing with wrong algo and size
            assertEquals(decryptedkey, wrongPrivateKeyAlgoAndSize);
            fail("Wrong Key error message should be shown");
        }catch(Exception e){
        }
        try{
            SecretKey decryptedkey2 = Encryption.decFileSecretKey(encSK, wrongPrivateKeySize);//testing with wrong size but correct algo
            assertEquals(decryptedkey2,wrongPrivateKeySize);
        }catch (Exception e){
        }
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