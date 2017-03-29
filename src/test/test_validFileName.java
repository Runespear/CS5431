package test;

import org.cs5431_client.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 28/3/17.
 */
class test_validFileName {
    @Test
    void test_validFileName_typical(){
        String typical[] = {"cats","not adult", "assignment", "1","photos2007-7000"};
        for (int i = 0; i < typical.length; i++){
            boolean legit = Validator.validFileName(typical[i]);
            assertEquals(true,legit);
        }
    }

    @Test
    void test_validFileName_invalidCharacters(){
        boolean dot = Validator.validFileName(".");
        boolean doubledots = Validator.validFileName("..");
        boolean slashes = Validator.validFileName("/");
        boolean upOne = Validator.validFileName("../");

        assertEquals(false, dot);
        assertEquals(false,doubledots);
        assertEquals(false, slashes);
        assertEquals(false, upOne);
    }

    @Test
    void test_validFileName_tooLong(){

        boolean ThirtyChar = Validator.validFileName("qwertyuiopasdfghjklzxcvbnmqwer");
        boolean ThirtyOneChar = Validator.validFileName("qwertyuiopasdfghjklzxcvbnmqwert");

        assertEquals(true, ThirtyChar);
        assertEquals(false, ThirtyOneChar);
    }
}