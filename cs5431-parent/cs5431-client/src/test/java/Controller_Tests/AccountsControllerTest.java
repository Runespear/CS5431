package Controller_Tests;

import org.cs5431.controller.AccountsController;
import org.cs5431.controller.UserController;
import org.cs5431.model.User;
import org.cs5431.view.PwdRecoveryBundle;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.cs5431.controller.SSLController.connect_SSLServerSocket;
import static org.junit.gen5.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Before running this test, please set up a server called test_server with ssl port 8080
 */
class AccountsControllerTest {
    private static AccountsController ac;
    private static User testUser;
    private static Socket s;

    @BeforeAll
    static void setUp() throws Exception {
        ac = new AccountsController();
        s = connect_SSLServerSocket("127.0.0.1",8888, "./user-config/test_server.jks");
        ac.setSocket(s);
        testUser = ac.createUser("testUser1", "passwordpassword", "bcp39@cornell.edu", "999",
                0, false, null, 0, null);
    }

    @AfterAll
    static void tearDown() throws Exception {
        UserController userController = new UserController(testUser, s);
        userController.deleteUser(testUser.getUsername(), "passwordpassword");
    }

    @Test
    void createUser() throws Exception {
        User user = ac.createUser("testUser2", "passwordpassword", "bcp39@cornell.edu", "999",
                0, false, null, 0, null);
        User expectedUser = new User(user.getId(), "testUser2", "bcp39@cornell.edu", user.getUserParentFolder(),
                user.getPrivKey(), user.getPubKey(), 0, null);
        assertEquals(expectedUser, user);
        UserController userController = new UserController(user, s);
        userController.deleteUser(user.getUsername(), "passwordpassword");
    }

    @Test
    void login() throws Exception {
        JSONObject response = ac.login("testUser1", "passwordpassword");
        assertEquals("loginAck", response.getString("msgType"));
    }

    @Test
    void do2fa() {
        assertThrows( AccountsController.LoginFailException.class,
                () -> ac.do2fa("wrong otp", testUser.getId()));
    }

    @Test
    void parseLogin() throws Exception {
        JSONObject response = ac.login("testUser1", "passwordpassword");
        User user = ac.parseLogin("testUser1", "passwordpassword", response, 0);
        assertEquals(testUser, user);
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
        assertThrows(AccountsController.UserRetrieveException.class,
                () -> ac.recoverPassword("testUser1"));
    }

    @Test
    void sendRecoveryEmail() {
        assertThrows(AccountsController.UserRetrieveException.class,
                () -> ac.sendRecoveryEmail(testUser.getId(), "testUser1"));
    }

}