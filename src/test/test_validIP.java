package test;

import org.cs5431_client.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 28/3/17.
 */
class test_validIP {
    @Test
    void test_validIP_localhost(){
        // localhost string not allowed?

        boolean localhost = Validator.validIP("localhost");
        assertEquals(false,localhost);
    }

    @Test
    void test_validIP_3dots(){
        boolean dot0 = Validator.validIP("10");
        boolean dot1 = Validator.validIP("10.10");
        boolean dot2 = Validator.validIP("10.10.10");
        boolean dot3 = Validator.validIP("10.10.10.10"); // This is valid
        boolean dot4 = Validator.validIP("10.10.10.10.10");

        assertEquals(false, dot0);
        assertEquals(false, dot1);
        assertEquals(false, dot2);
        assertEquals(true, dot3);
        assertEquals(false, dot4);
    }

    @Test
    void test_validIP_digitsOnly(){
        boolean nondigital = Validator.validIP("ten.ten.1.0");

        assertEquals(false, nondigital);
    }

    @Test
    void test_validIP_range(){
        boolean above255_1 = Validator.validIP("256.255.255.255");
        boolean above255_2 = Validator.validIP("255.256.255.255");
        boolean above255_3 = Validator.validIP("255.255.256.255");
        boolean above255_4 = Validator.validIP("255.255.255.256");


        assertEquals(false,above255_1);
        assertEquals(false,above255_2);
        assertEquals(false,above255_3);
        assertEquals(false,above255_4);
    }

    @Test

    void validIP_legitIP(){
        boolean broadcast = Validator.validIP("255.255.255.255");
        boolean localhost = Validator.validIP("127.0.0.1");
        boolean LinksysAdmin = Validator.validIP("192.168.1.1");
        boolean GoogleDNS_P = Validator.validIP("8.8.8.8");
        boolean GoogleDNS_S = Validator.validIP("8.8.8.4");

        assertEquals(true, broadcast);
        assertEquals(true, localhost);
        assertEquals(true, LinksysAdmin);
        assertEquals(true, GoogleDNS_P);
        assertEquals(true, GoogleDNS_S);
    }
}