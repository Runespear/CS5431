package test.Validator;

import org.cs5431_client.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 28/3/17.
 */
class test_validPassword {
    @Test
    void test_validPassword_null(){
        boolean empty = Validator.validPassword("");
        boolean null1 = Validator.validPassword(null);

        assertEquals(false, empty);
        assertEquals(false, null1);
    }

    @Test
    void test_validPassword_tooShort(){
        int starting = 33;
        String pass = Character.toString( (char) starting );
        boolean tooShort;
        for (int i = 1; i < 16 ; i++){
            //System.out.println(pass);
            tooShort = Validator.validPassword(pass);
            assertEquals(false,tooShort);
            pass += Character.toString((char) (++starting));
        }
    }

    @Test
    void test_validPassword_lengthOK(){
        int starting = 56;
        String pass = "abcdefghijklmnop";
        boolean justRight;
        for (int i = 1; i < (999-14) ; i++){
            //System.out.println(pass);
            justRight = Validator.validPassword(pass);
            assertEquals(true,justRight);
            pass += Character.toString((char) (++starting));
        }
    }

    @Test
    void test_validPassword_length_TOOLONG(){
        int starting = 1;
        String pass = "";
        boolean tooLong;
        for (int i = 1; i<= 1000; i++){
            pass+=Character.toString((char) (++starting));
        }
        tooLong = Validator.validPassword(pass);
        assertEquals(false,tooLong);
    }

}