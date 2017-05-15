package SSS_Tests;

import org.cs5431.SSS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

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
        setup();
        //Check each point
        BigInteger[][] testCoordinates = new BigInteger[this.numParts][2];
        for (int x = 1; x <=numParts ;x++){
            testCoordinates[x-1][0]=BigInteger.valueOf(x);
            testCoordinates[x-1][1]=sssObject.applyPoly(x,this.polynomial);
        }
        BigInteger[][] generatedCoordinates = sssObject.generateCoordinates();

        assert(Arrays.deepEquals(testCoordinates,generatedCoordinates) );
    }

    @Test
    void checkCheckSubsets(){
        setup();

        BigInteger[][] badArray = new BigInteger[3][2];
        badArray[0][0] = BigInteger.ONE;
        badArray[0][1] = BigInteger.valueOf(123);
        badArray[1][0] = BigInteger.valueOf(2);
        badArray[1][1] = BigInteger.valueOf(1234);
        badArray[2][0] = BigInteger.ONE;
        badArray[2][1] = BigInteger.valueOf(123);

        HashMap<BigInteger,BigInteger> uniqueCoords = sssObject.checkSubsets(badArray);

        System.out.println(System.lineSeparator()+"Printing our unique Dict");
        for (Map.Entry<BigInteger, BigInteger> entry : uniqueCoords.entrySet()) {
            System.out.println(entry.getKey()+" : "+entry.getValue());
        }
        assertEquals(2,uniqueCoords.size());
    }

    @Test
    void checkGenerateSecrets(){
        setup();
        List<String> HolyShit = sssObject.generateSecrets();

        this.polynomial = sssObject.polynomial;

        BigInteger[][] coordinates = sssObject.generateCoordinates();
        List<String> secrets = new ArrayList<String>();
        for (int i = 0 ; i < coordinates.length;i++){
            String coordinateStr = coordinates[i][0].toString() + ":" + coordinates[i][1].toString();
            secrets.add(coordinateStr);
        }

        Iterator itHS = HolyShit.iterator();
        Iterator itS = secrets.iterator();

        for (int i = 0; i<HolyShit.size();i++){
            System.out.println();
            System.out.println(HolyShit.get(i));
            System.out.println(secrets.get(i));
            assert(secrets.get(i).equals(HolyShit.get(i)));
        }


    }


}