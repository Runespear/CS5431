package Validator_Tests;
import org.cs5431.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by user on 14/5/2017.
 */
class test_validPhone {
    @Test
    void testBrandonDashes(){
        String Brandon = "607-279-7806";
        boolean valid = Validator.validPhone(Brandon);
        assertEquals(valid,true);
    }
    @Test
    void testBrandonNormal(){
        String Brandon = "6072797806";
        boolean valid = Validator.validPhone(Brandon);
        assertEquals(valid,true);
    }
    @Test
    void testBrandonCountry(){
        String Brandon = "+1-607-379-9856";
        boolean valid = Validator.validPhone(Brandon);
        assertEquals(valid,true);
    }
}