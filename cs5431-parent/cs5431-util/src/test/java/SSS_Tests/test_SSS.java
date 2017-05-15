package SSS_Tests;

import org.cs5431.SSS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigInteger;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 14/5/17.
 */
class test_SSS {
    SSS sssObject;
    BigInteger secretNumber;
    static SecureRandom generator = new SecureRandom();
    int numParts;
    int numSubsets;
    BigInteger[] polynomial;

    SSS setup(){

        byte[] value = new byte[256];
        generator.nextBytes(value);
        this.secretNumber = new BigInteger(value);

        //System.out.println("Printing out secretNumber");
        //System.out.println(secretNumber);
        //System.out.println("End of secretNumber");
        //Default 6 ,3
        this.numParts = 6;
        this.numSubsets = 3;

        this.sssObject = new SSS(numParts,numSubsets,secretNumber);
        this.polynomial = this.sssObject.generatePolynomial();

        return sssObject;
    }

    @Test
    void checkPartsSubsets(){
        setup();
        assertEquals(this.sssObject.numParts,this.numParts);
        assertEquals(this.sssObject.numSubsets,this.numSubsets);
    }

    @Test
    void checkGeneratePolynomial(){
        setup();
        System.out.println();
        System.out.println("Printing out polynomial coefficients");
        for (int i = 0; i < this.polynomial.length;i++){
            System.out.println(this.polynomial[i]);
        }

    }

    @Test
    void checkApplyPolynomial(){
        setup();
        for (int x = 0; x < 10; x ++){
            BigInteger manual = BigInteger.ZERO;
            BigInteger apply;
            //Manually
            //System.out.println();
            //System.out.println(this.polynomial.length);

            apply = sssObject.applyPoly(x,this.polynomial);

            for (int i = 0; i < this.polynomial.length;i++){
                manual = manual.add(polynomial[i].multiply( (BigInteger.valueOf(x) ).pow(i) ) );
            }
            assertEquals(manual,apply);
        }
    }

    @Test
    void checkGenerateCoordinates(){
        assertEquals(true,true);
    }

}