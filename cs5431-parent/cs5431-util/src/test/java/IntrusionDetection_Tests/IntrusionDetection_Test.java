package IntrusionDetection_Tests;

import org.cs5431.Email;
import org.cs5431.IntrusionDetection;
import org.cs5431.SQL_Accounts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Base64;
import java.util.List;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.secondPwdHash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by user on 17/4/2017.
 */
class IntrusionDetection_Test {
    public static IntrusionDetection account;
    public static SQL_Accounts sql_account;

    private int port = 3306;
    private String ip = "127.0.0.1";
    private String DB_USER = "root";
    private String DB_PASSWORD = "1234567890";

    public boolean create_Log(String failuretype){
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement addLog = null;
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                addLog = connection.prepareStatement(insertLog);
                addLog.setInt(1, 0);
                addLog.setInt(2, 0);
                addLog.setString(3, null);
                addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                addLog.setString(5, "LOGIN");
                addLog.setString(6, "FAILURE");
                addLog.setString(7, ip);
                addLog.setString(8, failuretype);
                addLog.executeUpdate();
                return true;
            } catch (SQLException e1) {
                e1.printStackTrace();
            } finally {
                if (addLog != null) {
                    addLog.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean create_Log_action(String actiontype){
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement addLog = null;
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                addLog = connection.prepareStatement(insertLog);
                addLog.setInt(1, 0);
                addLog.setInt(2, 0);
                addLog.setString(3, null);
                addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                addLog.setString(5, actiontype);
                addLog.setString(6, "FAILURE");
                addLog.setString(7, ip);
                addLog.setString(8, "SOME FAILURE");
                addLog.executeUpdate();
                return true;
            } catch (SQLException e1) {
                e1.printStackTrace();
            } finally {
                if (addLog != null) {
                    addLog.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    @BeforeEach
    void setUp() {
        account = new IntrusionDetection("127.0.0.1", 3306, "root", "1234567890");
        sql_account = new SQL_Accounts("127.0.0.1", 3306, "root", "1234567890");
    }

    @Test
    void test_noPermissionAlert(){
        for (int i = 0; i<10; i++) {
            boolean test = create_Log("NO PERMISSION");
        }
        List<String> testing = account.noPermissionAlert();
        boolean check = true;
        if (testing == null){
            check = false;
        }
        assertEquals(true, check);
        sql_account.dropUserLogs();
    }

    @Test
    void test_getfailedloginsperIP(){
        for (int i = 0; i<20; i++) {
            boolean test = create_Log("FAILED");
        }
        List<String> testing = account.getFailedLoginsPerIp();
        boolean check = true;
        if (testing == null){
            check = false;
        }
        assertEquals(true, check);
        sql_account.dropUserLogs();
    }

    @Test
    void test_getfailedloginsperUID(){
        for (int i = 0; i<20; i++) {
            boolean test = create_Log("FAILED");
        }
        List<Integer> testing = account.getFailedLoginsPerUid();
        boolean check = true;
        //will return a non-empty list since intrusion has been detected
        if (testing == null){
            check = false;
        }
        assertEquals(true, check);
        sql_account.dropUserLogs();
    }

    @Test
    void test_totalfailedlogins(){
        for (int i = 0; i<5; i++) {
            boolean test = create_Log("FAILED");
        }
        int number_failed = account.totalFailedLogins();
        assertEquals(5,number_failed);
        sql_account.dropUserLogs();
    }

    @Test
    void test_getattemptedUIDfailure(){
        for (int i = 0; i<10; i++) {
            boolean test = create_Log_action("ATTEMPTED_UID");
        }
        List<Integer> testing = account.getAttemptedUidFailure();
        boolean check = true;
        //will return a non-empty list since intrusion has been detected
        if (testing == null){
            check = false;
        }
        assertEquals(true, check);
        sql_account.dropUserLogs();
    }

    @Test
    void test_getFailedAuthperIP(){
        for (int i = 0; i<10; i++) {
            boolean test = create_Log_action("AUTHENTICATION");
        }
        List<String> testing = account.getFailedAuthPerIp();
        boolean check = true;
        //will return a non-empty list since intrusion has been detected
        if (testing == null){
            check = false;
        }
        assertEquals(true, check);
        sql_account.dropUserLogs();
    }

    @Test
    void test_getFailedAuthperUID(){
        for (int i = 0; i<10; i++) {
            boolean test = create_Log_action("AUTHENTICATION");
        }
        List<Integer> testing = account.getFailedAuthPerUid();
        boolean check = true;
        //will return a non-empty list since intrusion has been detected
        if (testing == null){
            check = false;
        }
        assertEquals(true, check);
        sql_account.dropUserLogs();
    }

}