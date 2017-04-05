package Validator_Tests;

import org.cs5431.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 28/3/17.
 */
class test_validUsername {
    @Test
    void test_validUsername_spaces() {
        boolean namewithspace = Validator.validUsername("Brandon Peh");
        assertEquals(false, namewithspace );

        boolean spaces = Validator.validUsername("          ");
        assertEquals(false, spaces);
    }

    @Test
    void test_validUsername_tooshort(){
        boolean tooshort1 = Validator.validUsername("a");
        assertEquals(false, tooshort1 );

        boolean tooshort2 = Validator.validUsername("as");
        assertEquals(false, tooshort2 );

        boolean tooshort3 = Validator.validUsername("asd");
        assertEquals(false, tooshort3 );

        boolean tooshort4 = Validator.validUsername("asde");
        assertEquals(false, tooshort4 );

        boolean exactly5 = Validator.validUsername("asdef");
        assertEquals(true, exactly5);

    }

    @Test
    void test_validUsername_toolong(){
        boolean longname = Validator.validUsername("Janus_Maximus_of_Tigurius_Princeps_of_Rubicon");
        boolean exactly30 = Validator.validUsername("qwertyuiopasdfghjklzxcvbnmqwer");
        boolean exactly31 = Validator.validUsername("qwertyuiopasdfghjklzxcvbnmqwert");

        assertEquals(false, longname);
        assertEquals(true, exactly30);
        assertEquals(false, exactly31);
    }

    @Test
    void test_validUsername_empty(){
        boolean empty = Validator.validUsername("");
        assertEquals(false, empty );
    }

    @Test
    void test_validUsername_nonalphanumeric(){

        boolean punctuations = Validator.validUsername("!Wtf!");
        boolean at = Validator.validUsername("Hans@Mother-Solo");

        assertEquals(false, punctuations);
        assertEquals(false, at);
    }

    @Test
    void test_validUsername_typicalNames(){
        String typicalNames[] = {"Brandon", "Zilong", "Ruixin", "Louise", "Michael", "Eleanor"};

        for (int i = 0; i < typicalNames.length; i++){
            boolean result = Validator.validUsername(typicalNames[i]);
            assertEquals(true,result);
        }

    }

    @Test
    void test_validUsername_substitutedNames(){
        String funnyNames[] = {"l33thaxor","hanss0l0","j00lian"};
        for (int i = 0; i < funnyNames.length; i++){
            boolean result = Validator.validUsername(funnyNames[i]);
            assertEquals(true,result);
        }
    }
}