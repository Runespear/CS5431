package Controller_Tests;

import org.cs5431.controller.AccountsController;
import org.cs5431.model.User;
import org.cs5431.view.PwdRecoveryBundle;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.cs5431.controller.SSLController.connect_SSLServerSocket;
import static org.junit.gen5.api.Assertions.assertEquals;

class AccountsControllerTest {
    private static AccountsController ac;
    private static User testUser;

    @BeforeEach
    void setUp() throws Exception {
        ac = new AccountsController();
        Socket s = connect_SSLServerSocket("test_server",8080, "./user-config/test_server.jks");
        ac.setSocket(s);
        testUser = ac.createUser("testUser1", "passwordpassword", "bcp39@cornell.edu", "999",
                0, false, null, 0, null);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void createUser() throws Exception {
        User user = ac.createUser("testUser2", "passwordpassword", "bcp39@cornell.edu", "999",
                0, false, null, 0, null);
        User expectedUser = new User(user.getId(), "testUser2", "bcp39@cornell.edu", user.getUserParentFolder(),
                user.getPrivKey(), user.getPubKey(), 0);
        assertEquals(expectedUser, user);
    }

    @Test
    void login() throws Exception {
        JSONObject response = ac.login("testUser1", "passwordpassword");
        assertEquals("loginAck", response.getString("msgType"));
    }

    @Test
    void do2fa() {
    }

    @Test
    void parseLogin() {
    }

    @Test
    void getUsername() throws Exception {
        String username = ac.getUsername(testUser.getId());
        assertEquals(testUser.getUsername(), username);
    }

    @Test
    void getUserId() throws Exception {
        int uid = ac.getUserId(testUser.getUsername());
        assertEquals(testUser.getId(), uid);
    }

    @Test
    void getUserForPwdRecovery() throws Exception {
        PwdRecoveryBundle expected = new PwdRecoveryBundle(testUser.getId(), testUser.getUsername(),
                testUser.getPubKey());
        PwdRecoveryBundle actual = ac.getUserForPwdRecovery(testUser.getUsername());
        assertEquals(expected, actual);
    }

    @Test
    void recoverPassword() {
    }

    @Test
    void sendRecoveryEmail() {
    }

}