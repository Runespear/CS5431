package SSL_CmdLine_Test;

import org.junit.jupiter.api.Test;
import org.cs5431.SSLServer;

import java.net.Socket;
import java.util.Random;

import org.cs5431.Encryption;

import static org.cs5431.Encryption.generatePasswordHash;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Created by Brandon on 11/4/2017.
 */
public class test_validHash {

    protected String getRandomString(int Length, int seed) {
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
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
    @Test
    void testPasswordHash(){
        //Test whether the same password gives the same hash and salt(salt and hash should be different)

        String one = getRandomString(16,1000);

        for (int i =0; i<100; i++) {
            String first_hash[] = generatePasswordHash(one);
            String second_hash[] = generatePasswordHash(one);

            assertNotEquals(first_hash[0],second_hash[0]); //Testing hashed password
            assertNotEquals(first_hash[1], second_hash[1]);//Testing salt
        }

    }
}
