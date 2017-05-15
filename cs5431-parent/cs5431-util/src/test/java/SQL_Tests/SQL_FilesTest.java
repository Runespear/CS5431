package SQL_Tests;

import org.cs5431.SQL_Accounts;
import org.cs5431.SQL_Files;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.cs5431.JSON.receiveJson;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by zilong on 15/5/17.
 */
class SQL_FilesTest {
    public static SQL_Files files;
    public static SQL_Accounts accounts;
    String IP = "127.0.0.1";
    int port = 3306;
    JSONObject jsonUser;

    @BeforeEach
    void setUp() {
        //RUN SERVER VIEW BEFORE ATTEMPTING TO RUN TESTS
        files = new SQL_Files(IP, port, "zilong", "12345678901234567890");
        accounts = new SQL_Accounts(IP, port, "zilong", "12345678901234567890");

        String username = "username";
        String pubKey = "pubKey";
        String privKey = "privKey";
        String privKeySalt = "privKeySalt";
        int uid = 1;
        int folderid = 1;
        String email = "email@email.com";
        jsonUser = new JSONObject();
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

    }

    @Test
    void check_uploadKeys() throws Exception {
        Socket s = new Socket(IP,port);

        JSONObject jsonObject = new JSONObject() ;// = receiveJson(s);
        jsonObject.put("msgType","uploadKeys");
        jsonObject.put("fsoid",1);
        jsonObject.put("uid",322);
        JSONObject k = files.uploadKeys(jsonObject,IP);
        assertEquals(k,null);
    }

    @Test
    void check_createFso() throws Exception {
        Socket s = new Socket(IP,port);

        JSONObject jsonObject = new JSONObject() ;// = receiveJson(s);
        jsonObject.put("msgType","uploadKeys");
        jsonObject.put("fsoid",1);
        jsonObject.put("uid",322);
        JSONObject k = files.uploadKeys(jsonObject,IP);
        assertEquals(k,null);
    }
}