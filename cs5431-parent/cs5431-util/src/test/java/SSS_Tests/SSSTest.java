package SSS_Tests;

import org.cs5431.SSS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 14/5/17.
 */
class SSSTest {

    SSS sssObject;
    BigInteger secretNumber;
    SecureRandom generator = new SecureRandom();
    int numParts;
    int numSubsets;
    BigInteger[] polynomial;

    @BeforeClass
    void setup(){
        byte[] value = new byte[256];
        generator.nextBytes(value);
        this.secretNumber = new BigInteger(value);

        System.out.println(secretNumber);

        //Default 6 ,3
        this.numParts = 6;
        this.numSubsets = 3;

        this.sssObject = new SSS(numParts,numSubsets,secretNumber);

    }

    @Test
    void checkPartsSubsets(){
        assertEquals(this.sssObject.numParts,this.numParts);
        assertEquals(this.sssObject.numSubsets,this.numSubsets);
    }

}