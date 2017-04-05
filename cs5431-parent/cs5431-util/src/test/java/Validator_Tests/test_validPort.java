package Validator_Tests;

import org.cs5431.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 28/3/17.
 */
class test_validPort{

    @Test
    void test_validPort_privilegedPorts(){
        for (int i = 1; i<1024; i++){
            boolean privilegedPort = Validator.validPort( Integer.toString(i) );
            assertEquals(false,privilegedPort);
        }
    }

    @Test
    void test_validPort_registeredPorts(){
        for (int i = 1024; i <=49151 ; i ++){
            boolean registeredPort = Validator.validPort( Integer.toString(i) );
            assertEquals(true, registeredPort);
        }
    }

    @Test
    void test_validPort_EphemerealPorts(){
        for (int i = 49152; i <= 65535; i++){
            boolean EphemerealPorts = Validator.validPort( Integer.toString(i) );
            assertEquals(true, EphemerealPorts);
        }
    }

    @Test
    void test_validPort_below1(){
        for (int i = 0; i >= -1000; i--){
            boolean below1 = Validator.validPort( Integer.toString(i) );
            assertEquals(false, below1);
        }
    }
    @Test
    void test_validPort_above65535(){
        for (int i = 65536; i <= 100000; i++){
            boolean above65536 = Validator.validPort( Integer.toString(i) );
            assertEquals(false, above65536);
        }
    }
}