package Validator_Tests;

import org.cs5431.Validator;
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

        String base = "";
        int j = 0;
        for (int i = 0; i<255;i++){
            j = 97+i%26;
            base+= Character.toString( (char) j );
        }
        //System.out.println(base);
        //System.out.println(base.length());

        boolean Char255 = Validator.validFileName(base);
        boolean Char256 = Validator.validFileName(base+"1");

        assertEquals(true, Char255);
        assertEquals(false, Char256);
    }

    @Test
    void test_validFileName_tooShort(){
        boolean onechar = Validator.validFileName("a");
        boolean empty = Validator.validFileName("");
        //boolean now = Validator.validFileName(null);

        assertEquals(true, onechar);
        assertEquals(false, empty);
        //assertEquals(false , now);
    }
}