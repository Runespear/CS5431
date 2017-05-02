package Validator_Tests;

import org.cs5431.Encryption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.BASE64Decoder;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

//TODO: Use method generateUserKeys to test public keys, private keys and salt for password encryption
class test_Encryption {

    public static PublicKey correctPublicKey;
    public static PrivateKey correctPrivateKey;
    public static PublicKey wrongPublicKeySize;
    public static PublicKey wrongPublicKeyAlgoAndSize;
    public static PrivateKey wrongPrivateKeySize;
    public static PrivateKey wrongPrivateKeyAlgoAndSize;

    public static byte[] getRandomBytes(int seed){
        Random random = new Random(seed);
        byte[] b = new byte[random.nextInt(100)+10];
        new Random().nextBytes(b);
        return b;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }


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
        wrongPublicKeySize = keypair2.getPublic();
        wrongPrivateKeySize = keypair2.getPrivate();

        SecureRandom random3 = new SecureRandom();//generating public/private key of wrong algo and size
        KeyPairGenerator keygen3 = KeyPairGenerator.getInstance("DSA");
        keygen3.initialize(1024,random3);
        KeyPair keypair3 = keygen3.generateKeyPair();
        wrongPublicKeyAlgoAndSize = keypair3.getPublic();
        wrongPrivateKeyAlgoAndSize = keypair3.getPrivate();
    }

    @BeforeAll
    public static void setUp() throws Exception{
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        PrintWriter pw = new PrintWriter(new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt"));
        Random random = new Random();
        pw.println(getRandomString(10,random.nextInt()));
        pw.close();
        PrintWriter pw2 = new PrintWriter(new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test2.txt"));
        Random random2 = new Random();
        pw2.println(getRandomString(20,random.nextInt()));
        pw2.close();
        getPrivPubKeyPair();
    }


    @Test
    void test_IVgeneration()throws Exception{ //Just need to ensure that the returned results are not the same

        IvParameterSpec previous = Encryption.generateIV();
        for (int i =0; i<100; i++) {
            IvParameterSpec new_iv = Encryption.generateIV();
            byte[] one = new_iv.getIV();
            byte[] two = previous.getIV();
            assertEquals(Arrays.equals(one,two),false);
            previous = new_iv;
        }
    }

    @Test
    void test_secretkeygeneration() throws Exception{ //Just need to ensure that the returned results are not the same

        SecretKey previous = Encryption.generateSecretKey();
        for (int i =0; i<100; i++) {
            SecretKey new_key = Encryption.generateSecretKey();
            byte[] one = new_key.getEncoded();
            byte[] two = previous.getEncoded();
            assertEquals(Arrays.equals(one,two),false);
            previous = new_key;
        }
    }

    @Test
    void test_encFile() throws Exception{ //Test to ensure that same encrypted file is encrypted differently under different IV and encrypted similarly under the same IV
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        SecretKey key = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        IvParameterSpec iv2 = Encryption.generateIV();
        byte[] one = Encryption.encryptFile(file,key,iv);
        byte[] two = Encryption.encryptFile(file,key,iv);
        //System.out.println(Arrays.toString(one));
        //System.out.println(Arrays.toString(two));
        assertEquals(Arrays.equals(one,two), true);
        byte[] three = Encryption.encryptFile(file, key, iv2);
        assertEquals(Arrays.equals(one, three), false);

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
        assertEquals(Arrays.equals(one,two),false);

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
    void test_encFileName() throws Exception{ //Test to ensure that same encrypted file name is encrypted differently with different iV
        String randomFileName = getRandomString(10, 2000);
        SecretKey correctkey = Encryption.generateSecretKey();
        IvParameterSpec iv = Encryption.generateIV();
        IvParameterSpec iv2 = Encryption.generateIV();
        byte[] one = Encryption.encryptFileName(randomFileName,correctkey,iv);
        byte[] two = Encryption.encryptFileName(randomFileName,correctkey,iv2);
        assertEquals(Arrays.equals(one,two),false);

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
        assertEquals(Arrays.equals(one,two),false);

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
        assertEquals(Arrays.equals(one,two), false);
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
            fail("This is supposed to throw an exception for wrong key");
        }catch(Exception e){
        }

        //generating public key with correct algorithm but different size
        KeyPairGenerator keygen3 = KeyPairGenerator.getInstance("RSA");
        keygen3.initialize(1024,new SecureRandom());
        KeyPair keypair3 = keygen3.generateKeyPair();
        PublicKey pubKey2 = keypair3.getPublic();

        byte[] third = Encryption.encFileSecretKey(secretkey,pubKey2);
        assertEquals(Arrays.equals(one,third),false);
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

        //Testing File content
        String path = "./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt";
        FileInputStream inputStream = new FileInputStream(path);
        BufferedReader myInput = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder originalTxt = new StringBuilder();
        String thisLine;
        while ((thisLine = myInput.readLine()) != null) {
            originalTxt.append(thisLine);
        }
        String path2 = "./Encryption_Test_Folder/Decrypted_Stuff/test.txt";
        FileInputStream inputStream2 = new FileInputStream(path2);
        BufferedReader myInput2 = new BufferedReader(new InputStreamReader(inputStream2));
        StringBuilder decryptedTxt = new StringBuilder();
        String thisLine2;
        while ((thisLine2 = myInput2.readLine()) != null) {
           decryptedTxt.append(thisLine2);
        }
        assertEquals(originalTxt.toString().equals(decryptedTxt.toString()), true);

        //Testing Byte Array of Files
        byte[] originalTxtByte = new byte[inputStream.available()];
        inputStream.read(originalTxtByte);
        inputStream.close();
        byte[] decryptedTxtByte = new byte[inputStream2.available()];
        inputStream2.read(decryptedTxtByte);
        inputStream2.close();
        assertEquals(Arrays.equals(originalTxtByte,decryptedTxtByte), true);

        File file_to_delete = new File("./Encryption_Test_Folder/Decrypted_Stuff/test.txt");
        Boolean isittrue = file_to_delete.delete();
        System.out.println(isittrue);
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
//            assertEquals(filename.equals(decryptedFileName),true);
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
        byte[] one = secretkey.getEncoded();
        byte[] two = decryptedkey.getEncoded();
        assertEquals(Arrays.equals(one,two), true);
    }

    @Test
    void test_decFileSecretKeyWithWrongKey()throws Exception{ //Test to ensure that the file secret key cannot be decrypted with wrong key(different algo and different size)
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //generating secret key
        kg.init(128, new SecureRandom());
        SecretKey secretkey = kg.generateKey();

        byte[] encSK = Encryption.encFileSecretKey(secretkey, correctPublicKey);
        try {
            SecretKey decryptedkey = Encryption.decFileSecretKey(encSK, wrongPrivateKeyAlgoAndSize);//testing with wrong algo and size
            fail("Wrong Key error message should be shown");
        }catch(Exception e){
        }
        try{
            SecretKey decryptedkey2 = Encryption.decFileSecretKey(encSK, wrongPrivateKeySize);//testing with wrong size but correct algo
            fail("Wrong Key error message should be shown");
        }catch (Exception e){
        }
    }

    @Test //TODO: CHECK IF THIS IS CORRECT?
    void test_sameHashedPassword(){//Check whether pwdBasedHash gives same result with same salt and password
        byte[] randomsalt = Encryption.newPwdSalt();
        String password = getRandomString(20, 593);
        byte[] one = Encryption.pwdBasedHash(password, randomsalt);
        byte[] two = Encryption.pwdBasedHash(password, randomsalt);
        assertEquals(Arrays.equals(one,two), true);
    }

    @Test
    void test_diffHashedPassword() {//Check whether pwdBasedHash gives diff result with 3 diff cases: 1. same salt, diff password, 2. diff salt, same password, 3. diff salt, diff password
        //same salt, diff password
        byte[] randomsalt = Encryption.newPwdSalt();
        String password = getRandomString(20, 593);
        String password2 = "password";
        byte[] one = Encryption.pwdBasedHash(password, randomsalt);
        byte[] two = Encryption.pwdBasedHash(password2, randomsalt);
        assertEquals(Arrays.equals(one, two), false);

        //diff salt, same password
        byte[] randomsalt2 = Encryption.newPwdSalt();
        byte[] three = Encryption.pwdBasedHash(password, randomsalt);
        byte[] four = Encryption.pwdBasedHash(password, randomsalt2);
        assertEquals(Arrays.equals(three, four), false);

        //diff salt, diff password
        byte[] five = Encryption.pwdBasedHash(password2, randomsalt2);
        assertEquals(Arrays.equals(one, five), false);
    }

    @Test
    void test_generatedSalt(){//Salts should be unique
        byte[] previous = Encryption.newPwdSalt();//unable to thoroughly test for uniqueness through unit tests, but this will do for now
        for (int i =0; i<100; i++) {
            byte[] new_salt = Encryption.newPwdSalt();
            assertEquals(Arrays.equals(new_salt,previous), false);
            previous = new_salt;
        }
    }

    @Test
    void test_samePasswordBasedKey(){//Check whether pwdBasedKey gives same result with same salt and password
        byte[] randomsalt = Encryption.newPwdSalt();
        String password = getRandomString(20, 688);
        byte [][] one = Encryption.pwdBasedKey(password, randomsalt);
        byte [][] two = Encryption.pwdBasedKey(password, randomsalt);
        assertEquals(Arrays.equals(one[0],two[0]), true);
    }

    @Test
    void test_diffPasswordBasedKey(){//Check whether pwdBasedKey gives diff result with 3 diff cases: 1. same salt, diff password, 2. diff salt, same password, 3. diff salt, diff password
        //testing with same salt and different password
        String password = getRandomString(20, 688);
        String password2 = getRandomString(18, 18);
        byte[] randomsalt = Encryption.newPwdSalt();
        byte[][] one = Encryption.pwdBasedKey(password,randomsalt);
        byte[][] two = Encryption.pwdBasedKey(password2,randomsalt);
        assertEquals(Arrays.equals(one,two),false);

        //testing with different salt and same password
        byte[] randomsalt2 = Encryption.newPwdSalt();
        byte[][] three = Encryption.pwdBasedKey(password, randomsalt2);
        assertEquals(Arrays.equals(one,three), false );

        //testing with different salt and different password
        assertEquals(Arrays.equals(two,three), false);
    }

    @Test
    void test_sameSecondHashedPassword(){//Check whether secondPwdHash gives same result with same salt and password
        String password = getRandomString(30, 391);
        byte[] randomsalt = Encryption.newPwdSalt();
        String one = Encryption.secondPwdHash(password,randomsalt);
        String two = Encryption.secondPwdHash(password, randomsalt);
        assertEquals(one.equals(two),true);
    }

    @Test
    void test_diffSecondHashedPassword(){//Check whether secondPwdHash gives diff result with 3 diff cases: 1. same salt, diff password, 2. diff salt, same password, 3. diff salt, diff password
        //Testing with same salt and different password
        String password = getRandomString(30, 391);
        String password2 = getRandomString(15, 184);
        byte[] randomsalt = Encryption.newPwdSalt();
        String one = Encryption.secondPwdHash(password,randomsalt);
        String two = Encryption.secondPwdHash(password2, randomsalt);
        assertEquals(one.equals(two), false);

        //testing with different salt and same password
        byte[] randomsalt2 = Encryption.newPwdSalt();
        String three = Encryption.secondPwdHash(password, randomsalt2);
        assertEquals(one.equals(three),false);

        //testing with different salt and different password
        assertEquals(two.equals(three), false);
    }

    @Test
    void test_SHA256Length(){//Checking if the length is correct
        String message = getRandomString(100, 3920);
        byte[] one = Encryption.SHA256(message);
        //System.out.println(one.length);
        assertEquals(one.length==32, true);
    }
    @Test
    void test_sameSHA256(){//Checking if the results for the SHA256 of same messages are the same
        String message = getRandomString(150, 2408);
        byte[] one = Encryption.SHA256(message);
        byte[] two = Encryption.SHA256(message);
        assertEquals(Arrays.equals(one,two), true);
    }

    @Test
    void test_diffSHA256(){//Checking if the results for the SHA256 of different messages are different
        String message = getRandomString(150, 2408);
        String message2 = getRandomString(100, 3920);
        byte[] one = Encryption.SHA256(message);
        byte[] two = Encryption.SHA256(message2);
        assertEquals(Arrays.equals(one,two),false);
    }

    @Test
    void test_signSameCert()throws Exception{//Check if signed cert is the same for the same cert
        byte[] cert = getRandomBytes(1000);
        byte[] one = Encryption.signCert(cert,correctPrivateKey);
        byte[] two = Encryption.signCert(cert,correctPrivateKey);
        assertEquals(Arrays.equals(one,two), true);
    }

    @Test
    void test_signDiffCert()throws Exception{//Check if signed cert is different for different cert
        byte[] cert = getRandomBytes(1000);
        byte[] cert2 = getRandomBytes(2000);
        byte[] one = Encryption.signCert(cert,correctPrivateKey);
        byte[] two = Encryption.signCert(cert2,correctPrivateKey);
        assertEquals(Arrays.equals(one,two), false);
    }

    @Test
    void test_signCertWithDiffKey()throws Exception{//Check if signed cert is different when signed with different key algo and/or size
        byte[] cert = getRandomBytes(1001);
        byte[] one = Encryption.signCert(cert,correctPrivateKey);
        byte[] two = Encryption.signCert(cert,wrongPrivateKeySize); //wrong key size
        assertEquals(Arrays.equals(one,two), false);
        try {
            byte[] three = Encryption.signCert(cert, wrongPrivateKeyAlgoAndSize); //wrong algo and size
            fail("Signing is supposed to fail with incorrect key algo and size");
        }catch(Exception e){
        }
    }

    @Test
    void test_signSameJKS()throws Exception{//Check if signed JKS is the same for the same JKS
        byte[] JKS = getRandomBytes(1005);
        byte[] one = Encryption.signJKS(JKS,correctPrivateKey);
        byte[] two = Encryption.signJKS(JKS,correctPrivateKey);
        assertEquals(Arrays.equals(one,two), true);
    }

    @Test
    void test_signDiffJKS()throws Exception{//Check if signed JKS is different for different JKS
        byte[] JKS = getRandomBytes(1001);
        byte[] JKS2 = getRandomBytes(2056);
        byte[] one = Encryption.signJKS(JKS,correctPrivateKey);
        byte[] two = Encryption.signJKS(JKS2,correctPrivateKey);
        assertEquals(Arrays.equals(one,two), false);
    }

    @Test
    void test_signJKSWithDiffKey()throws Exception{//Check if signed JKS is different when signed with different key algo and/or size
        byte[] JKS = getRandomBytes(91008);
        byte[] one = Encryption.signJKS(JKS,correctPrivateKey);
        byte[] two = Encryption.signJKS(JKS,wrongPrivateKeySize); //wrong key size
        assertEquals(Arrays.equals(one,two), false);
        try {
            byte[] three = Encryption.signJKS(JKS, wrongPrivateKeyAlgoAndSize); //wrong algo and size
            fail("Signing is supposed to fail with incorrect key algo and size");
        }catch(Exception e){
        }
    }

    @Test
    void test_generateUserKeys()throws Exception{//Simply test whether keys generated are able to encrypt and decrypt
        String password = getRandomString(18, 19280);
        String keys[] = Encryption.generateUserKeys(password);
        String filename = "filename";
        SecretKey secretkey = Encryption.generateSecretKey();

        //convert base64 public key string to byte stream
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] bytes = decoder.decodeBuffer(keys[0]);
        // Convert the public key bytes into a PublicKey object
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFact = KeyFactory.getInstance("RSA");
        PublicKey pubkey = keyFact.generatePublic(x509KeySpec);

        // Convert the private key bytes into a PrivateKey object
        PrivateKey privkey = Encryption.getPrivKeyFromJSON(keys[1],keys[2],password);

        //Checking whether the user generated keys can encrypt and decrypt correctly
        byte[] encrypted_Stuff = Encryption.encFileSecretKey(secretkey, pubkey);
        SecretKey decrypted_secret_key = Encryption.decFileSecretKey(encrypted_Stuff, privkey);
        byte[] one = secretkey.getEncoded();
        byte[] two = decrypted_secret_key.getEncoded();
        assertEquals(Arrays.equals(one,two),true);

        //Testing salt with pwdBasedHash function
        byte[] pwdhash1 = Encryption.pwdBasedHash(password, Base64.getDecoder().decode(keys[2]));
        byte[] pwdhash2 = Encryption.pwdBasedHash(password, Base64.getDecoder().decode(keys[2]));
        assertEquals(Arrays.equals(pwdhash1,pwdhash2),true);
    }

    @Test
    void test_encryptPrivateKeysamePassSaltKey() throws Exception{//test to check whether encryption of same password under the same salt gives the same result
        String password = getRandomString(20,383);
        byte[] privKeyByte = correctPrivateKey.getEncoded();
        byte[] salt = Encryption.newPwdSalt();
        String one = Encryption.encryptPrivateKey(password,privKeyByte,salt);
        String two = Encryption.encryptPrivateKey(password,privKeyByte,salt);
        assertEquals(one.equals(two),true);
    }

    @Test
    void test_encryptPrivateKeyPermuteDifferentResult()throws Exception{//test to check whether encryption gives different result
        String password = getRandomString(20,383);
        String password2 = getRandomString(21, 9308);
        byte[] PrivKeyByte = correctPrivateKey.getEncoded();
        byte[] salt = Encryption.newPwdSalt();
        byte[] salt2 = Encryption.newPwdSalt();

        //1. Diff Pass, Same Salt
        String one = Encryption.encryptPrivateKey(password, PrivKeyByte, salt);
        String two = Encryption.encryptPrivateKey(password2, PrivKeyByte, salt);
        assertEquals(one.equals(two), false);

        //2. Diff Pass, Diff Salt
        String three = Encryption.encryptPrivateKey(password, PrivKeyByte, salt2);
        assertEquals(two.equals(three), false);

        //3. Same Pass, Diff salt
        assertEquals(one.equals(three), false);
    }


    @Test
    void test_getPubKeyFromJson()throws Exception{//Encode a public key to a string and retrieve it using getPubKeyFromJSON, test whether the two keys are same
        String pubKey = Base64.getEncoder().encodeToString(correctPublicKey.getEncoded());
        PublicKey Jsonpubkey = Encryption.getPubKeyFromJSON(pubKey);
        assertEquals(Arrays.equals(correctPublicKey.getEncoded(),Jsonpubkey.getEncoded()),true);
    }

    @Test //decryptPwdBasedKey is integrated tested here
    void test_getPrivKeyFromJson()throws Exception{//Encode a private key to a string with encryptPrivateKey and retrieve it using getPrivKeyFromJSON with salt and password
        // Test whether the two keys are same
        String password = getRandomString(20,3574);
        byte[] privKeyByte = correctPrivateKey.getEncoded();
        byte[] salt = Encryption.newPwdSalt();
        String saltstring = Base64.getEncoder().encodeToString(salt);
        String privkeystring = Encryption.encryptPrivateKey(password,privKeyByte,salt);

        //correct password, correct salt given
        PrivateKey Jsonprivkey = Encryption.getPrivKeyFromJSON(privkeystring,saltstring, password);
        assertEquals(Arrays.equals(correctPrivateKey.getEncoded(),Jsonprivkey.getEncoded()),true);

        //correct password, wrong salt
        byte[] salt2 = Encryption.newPwdSalt();
        String saltstring2 = Base64.getEncoder().encodeToString(salt2);
        try {
            PrivateKey Jsonprivkey2 = Encryption.getPrivKeyFromJSON(privkeystring, saltstring2, password);
            fail("Not supposed to be decoded");
        }catch (Exception e){
        }

        //wrong password, correct salt
        String password2 = getRandomString(30,34);
        try{
        PrivateKey Jsonprivkey3 = Encryption.getPrivKeyFromJSON(privkeystring, saltstring, password2);
        fail("Not supposed to be decoded");
        }catch (Exception e){
        }

        //wrong password, wrong salt
        try{
        PrivateKey Jsonprivkey4 = Encryption.getPrivKeyFromJSON(privkeystring, saltstring2, password2);
            fail("Not supposed to be decoded");
        }catch (Exception e){
        }
    }

     /*@Test
    void test_generateAndEncFile() throws Exception{//Test whether outputs are the same as the actual methods that produce them
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        File file2 = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test2.txt");
        String filename = "filename";
        String filename2 = "filename2";

        // same file, same name, same public key, all outputs should be different
        String[] outputs = Encryption.generateAndEncFile(file, filename, correctPublicKey);
        String[] outputs2 = Encryption.generateAndEncFile(file, filename, correctPublicKey);
        assertEquals(outputs[0].equals(outputs2[0]), false);
        assertEquals(outputs[1].equals(outputs2[1]), false);
        assertEquals(outputs[2].equals(outputs2[2]), false);
        assertEquals(outputs[3].equals(outputs2[3]), false);
        assertEquals(outputs[4].equals(outputs2[4]), false);

        //different file, same name, same public key, all outputs should be different
        String[] outputs3 = Encryption.generateAndEncFile(file2, filename, correctPublicKey);
        assertEquals(outputs[0].equals(outputs3[0]), false);
        assertEquals(outputs[1].equals(outputs3[1]), false);
        assertEquals(outputs[2].equals(outputs3[2]), false);
        assertEquals(outputs[3].equals(outputs3[3]), false);
        assertEquals(outputs[4].equals(outputs3[4]), false);

        //same file, same name, diff public key, all outputs should be different
        String[] outputs4 = Encryption.generateAndEncFile(file, filename, wrongPublicKeySize);
        assertEquals(outputs[0].equals(outputs4[0]), false);
        assertEquals(outputs[1].equals(outputs4[1]), false);
        assertEquals(outputs[2].equals(outputs4[2]), false);
        assertEquals(outputs[3].equals(outputs4[3]), false);
        assertEquals(outputs[4].equals(outputs4[4]), false);

        //different file, same name, different public key, all outputs should be different
        String[] outputs5 = Encryption.generateAndEncFile(file2, filename, wrongPublicKeySize);
        assertEquals(outputs[0].equals(outputs5[0]), false);
        assertEquals(outputs[1].equals(outputs5[1]), false);
        assertEquals(outputs[2].equals(outputs5[2]), false);
        assertEquals(outputs[3].equals(outputs5[3]), false);
        assertEquals(outputs[4].equals(outputs5[4]), false);
    }

    @Test
    void test_generateAndEncFileName() throws Exception{//Test whether outputs are the same as the actual methods that produce them
        String filename = getRandomString(5, 201);
        String filename2 = getRandomString(12, 305);
        String[] one = Encryption.generateAndEncFileName(filename, correctPublicKey);
        String[] two = Encryption.generateAndEncFileName(filename, correctPublicKey);
        String[] three = Encryption.generateAndEncFileName(filename, wrongPublicKeySize);
        String[] four = Encryption.generateAndEncFileName(filename2, correctPublicKey);
        String[] five = Encryption.generateAndEncFileName(filename2, wrongPublicKeySize);

        //same name, same key, diff result obtained
        assertEquals(one[0].equals(two[0]), false);
        assertEquals(one[1].equals(two[1]), false);
        assertEquals(one[2].equals(two[2]), false);

        //same name, different key, diff result obtained
        assertEquals(one[0].equals(three[0]), false);
        assertEquals(one[1].equals(three[1]), false);
        assertEquals(one[2].equals(three[2]), false);

        //diff name, diff key, diff result obtained
        assertEquals(one[0].equals(five[0]), false);
        assertEquals(one[1].equals(five[1]), false);
        assertEquals(one[2].equals(five[2]), false);

        //diff name, same key, diff result obtained
        assertEquals(one[0].equals(four[0]), false);
        assertEquals(one[1].equals(four[1]), false);
        assertEquals(one[2].equals(four[2]), false);
    }*/

    @Test
    void test_reEncryptFile() throws Exception{//Test whether the outputs are different for different permutations of key and file
        File file = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test.txt");
        File file2 = new File("./Encryption_Test_Folder/Stuff_To_Encrypt/test2.txt");
        SecretKey sk = Encryption.generateSecretKey();
        SecretKey sk2 = Encryption.generateSecretKey();

        //same file, same key, must obtain different outputs
        String[] one = Encryption.reEncryptFile(file,sk);
        String[] two = Encryption.reEncryptFile(file,sk);
        assertEquals(one[0].equals(two[0]), false);
        assertEquals(one[1].equals(two[1]), false);

        //same file, diff key, must obtain different outputs
        String[] three = Encryption.reEncryptFile(file,sk2);
        assertEquals(one[0].equals(three[0]), false);
        assertEquals(one[1].equals(three[1]), false);

        //diff file, same key, must obtain different outputs
        String[] four = Encryption.reEncryptFile(file2,sk);
        assertEquals(one[0].equals(four[0]), false);
        assertEquals(one[1].equals(four[1]), false);

        //diff file, diff key, must obtain different outputs
        String[] five = Encryption.reEncryptFile(file2,sk2);
        assertEquals(one[0].equals(five[0]), false);
        assertEquals(one[1].equals(five[1]), false);
    }

    @Test
    void test_reEncryptFileName()throws Exception{//Test whether outputs are different for different permutation and combination
        String file = getRandomString(22, 9302);
        String file2 = getRandomString(12,392);
        SecretKey sk = Encryption.generateSecretKey();
        SecretKey sk2 = Encryption.generateSecretKey();

        //same file, same key, must obtain different outputs
        String[] one = Encryption.reEncryptFileName(file,sk);
        String[] two = Encryption.reEncryptFileName(file,sk);
        assertEquals(one[0].equals(two[0]), false);
        assertEquals(one[1].equals(two[1]), false);

        //same file, diff key, must obtain different outputs
        String[] three = Encryption.reEncryptFileName(file,sk2);
        assertEquals(one[0].equals(three[0]), false);
        assertEquals(one[1].equals(three[1]), false);

        //diff file, same key, must obtain different outputs
        String[] four = Encryption.reEncryptFileName(file2,sk);
        assertEquals(one[0].equals(four[0]), false);
        assertEquals(one[1].equals(four[1]), false);

        //diff file, diff key, must obtain different outputs
        String[] five = Encryption.reEncryptFileName(file2,sk2);
        assertEquals(one[0].equals(five[0]), false);
        assertEquals(one[1].equals(five[1]), false);
    }

    @Test
    void test_generatePasswordHash(){//Just need to ensure that returned results are not the same
        String password = getRandomString(93,1902);
        String[] previous = Encryption.generatePasswordHash(password);
        for (int i =0; i<100; i++) {
            String[] new_results = Encryption.generatePasswordHash(password);
            assertEquals(previous[0].equals(new_results[0]),false);
            assertEquals(previous[1].equals(new_results[1]),false);
            previous = new_results;
        }
    }
}