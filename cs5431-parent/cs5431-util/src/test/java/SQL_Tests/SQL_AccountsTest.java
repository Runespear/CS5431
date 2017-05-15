package SQL_Tests;

import org.cs5431.Email;
import org.cs5431.JSON;
import org.cs5431.SQL_Accounts;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.cs5431.Encryption.secondPwdHash;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by user on 17/4/2017.
 */
class SQL_AccountsTest {
    public static SQL_Accounts account;

    @BeforeEach
    void setUp() {
        account = new SQL_Accounts("127.0.0.1", 3306, "root", "1234567890");
    }

    @Test
    void test_uniqueUsername(){
        //username does not exist
        String name = "user";
        boolean test = account.isUniqueUsername(name);
        assertEquals(test, true);
    }

    @Test
    void test_createUser(){

        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");


        account.createUser(jsonUser, "password", "salt", "192.168.0.0");
        boolean test = account.isUniqueUsername("username");
        assertEquals(test,false);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.0");

    }

    @Test
    void test_logsessionlimit(){
        //test whether the log works
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");


        account.createUser(jsonUser, "password", "salt", "192.168.0.1");
        boolean test = account.logSessionLimit("192.168.0.1");
        assertEquals(test,true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
    }

    @Test
    void test_twoFactorLogin(){
        //returns a null object when the uid is not valid
        JSONObject empty = account.twoFactorLogin(5000, "192.168.0.55");
        assertEquals(empty, null);
    }

    @Test
    void test_create2faFailureLog(){
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");
        account.createUser(jsonUser, "password", "salt", "192.168.0.1");
        boolean test = account.create2faFailureLog(account.getUserId("username"),"192.168.0.1");
        assertEquals(test,true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
    }

    @Test
    void test_authenticate(){
        //authenticate invalid username
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");
        Email email_acc = new Email("test","test");
        JSONObject object = account.authenticate(jsonUser,"password","192.168.0.0","check",email_acc);
        assertEquals(object, null);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
    }

    @Test
    void test_getPrivKeySalt(){
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");
        account.createUser(jsonUser, "password", "salt", "192.168.0.1");
        String test = account.getPrivKeySalt("username");
        assertEquals(test.equals("privKeySalt"),true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
    }

    @Test
    void test_getSalt(){
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");
        account.createUser(jsonUser, "password", "salt", "192.168.0.1");
        String test = account.getSalt("username","192.168.0.1","test");
        assertEquals(test.equals("salt"),true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
    }

    @Test
    void test_deleteUser(){
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");
        account.createUser(jsonUser, secondPwdHash("password", Base64.getDecoder().decode("salt")), "salt", "192.168.0.1");
        int uid_test = account.getUserId("username");
        int test = account.deleteUser(account.getUserId("username"),"username", "password","192.168.0.1");
        //delete should be successful
        assertEquals(test,uid_test);
    }

    @Test
    void test_getparentfolderid(){
        //invalid parent id
        int uid = account.getParentFolderid(100);
        assertEquals(uid, -1);
    }

    @Test
    void test_changepassword(){
        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("msgType", "registrationAck");
        jsonUser.put("username", username);
        jsonUser.put("uid", uid);
        jsonUser.put("parentFolderid", folderid);
        jsonUser.put("email", email);
        jsonUser.put("privKey", privKey);
        jsonUser.put("privKeySalt", privKeySalt);
        jsonUser.put("pubKey", pubKey);
        jsonUser.put("has2fa", 0);
        jsonUser.put("hasPwdRec", true);
        jsonUser.put("phoneNo", "6073799856");
        jsonUser.put("hashedPwd",  "password");
        jsonUser.put("newPrivKey", "newPrivKey");

        account.createUser(jsonUser, "password", "salt", "192.168.0.1");
        String newpassword = secondPwdHash("newpassword", Base64.getDecoder().decode("salt"));
        JSONObject test_object = account.changePassword(jsonUser, newpassword, "192.168.0.1");
        //incorrect authentication
        assertEquals(test_object,null);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
    }

    @AfterEach
    void tearDown() {
    }

}