package SQL_Tests;

import org.cs5431.Email;
import org.cs5431.JSON;
import org.cs5431.SQL_Accounts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Encryption.secondPwdHash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by user on 17/4/2017.
 */
class SQL_AccountsTest {
    public static SQL_Accounts account;
/*
    @BeforeAll
    void setUpAll() {
        //TODO HI RUIXIN
        return;
    }
*/
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
        account.dropUserLogs();

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
        account.dropUserLogs();
    }

    @Test
    void test_twoFactorLogin(){
        //returns a null object when the uid is not valid
        JSONObject empty = account.twoFactorLogin(5000, "192.168.0.55");
        boolean test = false;
        if (empty == null){
            test = true;
        }
        assertEquals(test, true);
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
        account.dropUserLogs();
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
        boolean test = false;
        if (object == null){
            test = true;
        }
        assertEquals(test, true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
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
        account.dropUserLogs();
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
        account.dropUserLogs();
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
        account.dropUserLogs();
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
        boolean test = false;
        if (test_object == null){
            test = true;
        }
        assertEquals(true, test);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_getUserLog() throws IOException{
        String test =account.getUserLog();
        //user logs successfully extracted
        assertNotNull(test);
        try {//only works for windows
            Path p1 = Paths.get("C:/ProgramData/MySQL/MySQL Server 5.7/Uploads/userlogs.csv");
            Files.deleteIfExists(p1);
        }
        catch (Error e){
        }
    }

    @Test
    void test_changeEmail(){
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
        boolean test = account.changeEmail(account.getUserId(username), email, "newemail@email.com", "192.168.0.1");
        //Successful email change
        assertEquals(true, test);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_checkCredentials(){
        boolean test = account.checkCredentials();
        //Credentials returned should be true
        assertEquals(true, test);
    }

    @Test
    void test_getUsername(){
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
        String test = account.getUsername(account.getUserId("username"));
        assertEquals(true, test.equals("username"));
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_userEmailExists(){
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
        boolean testing = true;
        JSONObject test = account.userEmailExists("username");
        if (test == null){
            testing = false;
        }
        assertEquals(true, testing);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_attemptedUIDfailLog(){
        boolean test = account.attemptedUidFailLog(5, 34,"192.168.0.1");
        assertEquals(true, test);
        account.dropUserLogs();
    }

    @Test
    void test_createRecoveryGroup(){
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

        JSONArray groupUid = new JSONArray();
        JSONArray secrets = new JSONArray();
        jsonUser.put("groupUid", groupUid);
        jsonUser.put("secrets", secrets);
        jsonUser.put("neededUsers", 6);
        boolean test = account.createRecoveryGroup(jsonUser,"192.168.0.0");
        assertEquals(true, test);
        account.dropUserLogs();
    }

    @Test
    void test_removeSecrets(){
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
        boolean test = account.removeSecrets(account.getUserId("username"), "192.168.0.1");
        assertEquals(true, test);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_toggle2FA(){
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
        boolean test = account.toggle2fa(account.getUserId("username"), 1, "192.168.0.1");
        //Testing that toggling works
        assertEquals(test,true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_getPasRecInfo(){
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
        JSONObject test_object = account.getPasRecInfo(account.getUserId("username"));
        boolean test = false;
        if (test_object == null){
            test = true;
        }
        assertEquals(false, test);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_hasRecovery(){
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
        boolean test = account.hasRecovery(account.getUserId("username"));
        assertEquals(true,test);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_getSecrets(){
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
        JSONObject test_object = account.getSecrets(account.getUserId("username"));
        boolean test = false;
        if (test_object == null){
            test = true;
        }
        assertEquals(false,test);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void test_recoverPwd(){
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
        JSONObject test_object = account.recoverPwd(account.getUserId("username"),"192.168.0.1");
        boolean test = true;
        if (test_object == null){
            test = false;
        }
        //password recovery is activated
        assertEquals(test,true);
        account.adminDeleteUser(account.getUserId("username"), "192.168.0.1");
        account.dropUserLogs();
    }

    @Test
    void testgetpubkeys(){
        JSONArray array = new JSONArray();
        List<String> list= account.getPubKeys(array);
        //the list should not be empty
        boolean test = false;
        if (list == null){
            test = true;
        }
        assertEquals(false, test);
    }

    @Test
    void testChangePhoneNumber(){
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
        jsonUser.put("oldPhone", "oldPhone");
        jsonUser.put("newPhone", "newPhone");

        int test_int = account.changePhoneNo(jsonUser,"192.168.0.3");
        boolean test = true;
        if (test_int == -1){
            test = false;
        }
        //json user does not exist, so will return -1
        assertEquals(false,test);
    }

    @AfterEach
    void tearDown() {
    }


}