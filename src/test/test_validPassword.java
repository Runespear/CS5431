package test;

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
}