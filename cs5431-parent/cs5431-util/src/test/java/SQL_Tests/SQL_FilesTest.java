package SQL_Tests;

import org.cs5431.SQL_Accounts;
import org.cs5431.SQL_Files;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;

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

        JSONObject fso = new JSONObject() ;// = receiveJson(s);
        fso.put("msgType","createFso");
        fso.put("parentFolderid",1);
        fso.put("uid",322);
        fso.put("fsoName","Onii-chan.txt");
        fso.put("size","123");
        fso.put("lastModified","1993-12-12 12:12:33");
        fso.put("isFile",true);

        //INPUT ARRAYS
        JSONArray editorsArr = new JSONArray();
        editorsArr.put("johnny");

        JSONArray viewersArr = new JSONArray();
        viewersArr.put("mary");

        JSONArray editorsKeysArr = new JSONArray();
        editorsKeysArr.put("3154");

        JSONArray viewersKeysArr = new JSONArray();
        viewersKeysArr.put("18-03-1993");


        fso.put("editors",editorsArr);
        fso.put("viewers",viewersArr);
        fso.put("editorsKeys",editorsKeysArr);
        fso.put("viewersKeys",viewersKeysArr);
        fso.put("fileIV","123");
        fso.put("fsoNameIV","asdasd");

        int k = files.createFso(fso,IP);

        int uid = fso.getInt("uid");
        int parentFolderid = fso.getInt("parentFolderid");
        String fsoName = fso.getString("fsoName");
        String size = fso.getString("size");
        Timestamp lastModified = Timestamp.valueOf(fso.getString("lastModified"));
        boolean isFile = fso.getBoolean("isFile");
        JSONArray editors = fso.getJSONArray("editors");
        JSONArray viewers = fso.getJSONArray("viewers");
        JSONArray editorsKeys = fso.getJSONArray("editorsKeys");
        JSONArray viewersKeys = fso.getJSONArray("viewersKeys");

        assertEquals(k,-1);
    }

    @Test
    void check_addParentPermissions(){
        
    }

}