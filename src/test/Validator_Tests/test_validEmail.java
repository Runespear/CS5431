package test.Validator;

import org.cs5431_client.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 28/3/17.
 */
class test_validEmail {
    @Test
    void test_validEmail_legit(){
        boolean GabeN = Validator.validEmail("gaben@valvesoftware.com");
        boolean Brandon = Validator.validEmail("bcp39@cornell.edu");
        boolean Clarkson = Validator.validEmail("clarkson@cs.cornell.edu");
        boolean punctuations = Validator.validEmail("!{}aasd#$#%!@cornell.edu");

        assertEquals(true, GabeN);
        assertEquals(true, Brandon);
        assertEquals(true, Clarkson);
        assertEquals(true, punctuations);
    }

    @Test
    void test_validEmail_notLegit(){
        boolean noAt = Validator.validEmail("bcp39atcornell.edu");
        boolean noDot = Validator.validEmail("asdasd@gmailcom");
        boolean AtInName = Validator.validEmail("John@Doe@gmail.com");

        assertEquals(false,noAt);
        assertEquals(false,noDot);
        assertEquals(false, AtInName);
    }
}