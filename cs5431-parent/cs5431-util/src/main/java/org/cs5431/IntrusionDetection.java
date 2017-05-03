package org.cs5431;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IntrusionDetection {
    private int port;
    private String ip;
    private String DB_USER;
    private String DB_PASSWORD;
    private int FAILURE_PER_UID_PER_DAY = 20;
    private int FAILURE_PER_IP_PER_DAY = 20;
    private int TOTAL_FAILED_LOGIN_PER_DAY = 200;

    IntrusionDetection(String ip, int dbPort, String username, String password) {
        this.ip = ip;
        this.port = dbPort;
        this.DB_USER = username;
        this.DB_PASSWORD = password;

        boolean isAttacked = false;

        List<String> attackerIp = noPermissionAlert();
        if (!attackerIp.isEmpty()) {
            System.out.println("INTRUSION DETECTED. IP(s) that attempted to access the system without permission: " + attackerIp);
            isAttacked = true;
        }

        attackerIp = getFailedAuthPerIp();
        if (!attackerIp.isEmpty()) {
            System.out.println("INTRUSION DETECTED. Multiple failed authentication attempts from the same IP(s): " + attackerIp);
            isAttacked = true;
        }

        attackerIp = getFailedLoginsPerIp();
        if (!attackerIp.isEmpty()) {
            System.out.println("INTRUSION DETECTED. Multiple failed login attempts from the same IP(s): " + attackerIp);
            isAttacked = true;
        }

        List<Integer> attackedUid = getAttemptedUidFailure();
        if (!attackedUid.isEmpty()) {
            System.out.println("INTRUSION DETECTED. Attempts to impersonate the following user(s): " + attackedUid);
            isAttacked = true;
        }

        attackedUid = getFailedAuthPerUid();
        if (!attackedUid.isEmpty()) {
            System.out.println("INTRUSION DETECTED. Multiple failed authentication attempts on the same uid(s): " + attackedUid);
            isAttacked = true;
        }

        attackedUid = getFailedLoginsPerUid();
        if (!attackedUid.isEmpty()) {
            System.out.println("INTRUSION DETECTED. Multiple failed login attempts on the same uid(s): " + attackedUid);
            isAttacked = true;
        }

        if (totalFailedLogins() > TOTAL_FAILED_LOGIN_PER_DAY) {
            System.out.println("INTRUSION DETECTED. An excessive number of failed login attempts.");
            isAttacked = true;
        }

        if (!isAttacked) {
            System.out.println("OK, no intrusion has been detected.");
        }
    }

    //any ip from this list should be banned
    List<String> noPermissionAlert() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getNoPermission = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            String selectUL = "SELECT L.sourceIp FROM FileLog L WHERE L.failureType = \"NO PERMISSION\"" +
                    "AND time_to_sec(timediff(NOW(), L.lastModified )) / 3600 < 24";

            try {
                getNoPermission = connection.prepareStatement(selectUL);
                ResultSet rs = getNoPermission.executeQuery();
                ArrayList<String> attackIp = new ArrayList<>();
                while (rs.next()) {
                    attackIp.add(rs.getString(1));
                }
                return attackIp;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getNoPermission != null) {
                    getNoPermission.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    List<String> getFailedLoginsPerIp() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getFailure = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            //currently 20 fails from the same ip per day
            String selectFailure = "SELECT UL.sourceIp FROM\n" +
                    "(SELECT L.sourceIp, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(NOW(), L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"LOGIN\"" +
                    "GROUP BY L.sourceIp) as UL\n" +
                    "WHERE UL.numFail > ?;";
            try {
                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setInt(1, FAILURE_PER_IP_PER_DAY);
                ResultSet rs = getFailure.executeQuery();
                ArrayList<String> attackIp = new ArrayList<>();
                while (rs.next()) {
                    attackIp.add(rs.getString(1));
                }
                return attackIp;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getFailure != null) {
                    getFailure.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    List<Integer> getFailedLoginsPerUid() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getFailure = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            //currently 20 fails from the same uid per day
            String selectFailure = "SELECT UL.sourceIp FROM\n" +
                    "(SELECT L.uid, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(NOW(), L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"LOGIN\"" +
                    "GROUP BY L.uid) as UL\n" +
                    "WHERE UL.numFail > ?;";
            try {
                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setInt(1, FAILURE_PER_UID_PER_DAY);
                ResultSet rs = getFailure.executeQuery();
                ArrayList<Integer> attackedUid = new ArrayList<>();
                while (rs.next()) {
                    attackedUid.add(rs.getInt(1));
                }
                return attackedUid;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getFailure != null) {
                    getFailure.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    int totalFailedLogins() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getNoPermission = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            String selectUL = "SELECT COUNT(*) FROM UserLog L WHERE L.status = \"FAILURE\" " +
                    "AND L.actionType = \"LOGIN\"";

            try {
                getNoPermission = connection.prepareStatement(selectUL);
                ResultSet rs = getNoPermission.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getNoPermission != null) {
                    getNoPermission.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    List<Integer> getAttemptedUidFailure() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getNoPermission = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            String selectUL = "SELECT L.uid FROM UserLog L WHERE L.actionType = \"ATTEMPTED_UID\" " +
                    "AND time_to_sec(timediff(NOW(), L.lastModified )) / 3600 < 24";

            try {
                getNoPermission = connection.prepareStatement(selectUL);
                ResultSet rs = getNoPermission.executeQuery();
                ArrayList<Integer> attackedUid = new ArrayList<>();
                while (rs.next()) {
                    attackedUid.add(rs.getInt(1));
                }
                return attackedUid;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getNoPermission != null) {
                    getNoPermission.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Authentication comes from change password and delete user
    List<String> getFailedAuthPerIp() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getFailure = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            //currently 20 fails from the same ip per day
            String selectFailure = "SELECT UL.sourceIp FROM\n" +
                    "(SELECT L.sourceIp, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(NOW(), L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"AUTHENTICATION\"" +
                    "GROUP BY L.sourceIp) as UL\n" +
                    "WHERE UL.numFail > ?;";
            try {
                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setInt(1, FAILURE_PER_IP_PER_DAY);
                ResultSet rs = getFailure.executeQuery();
                ArrayList<String> attackIp = new ArrayList<>();
                while (rs.next()) {
                    attackIp.add(rs.getString(1));
                }
                return attackIp;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getFailure != null) {
                    getFailure.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    List<Integer> getFailedAuthPerUid() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getFailure = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            //currently 20 fails from the same uid per day
            String selectFailure = "SELECT UL.sourceIp FROM\n" +
                    "(SELECT L.uid, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(NOW(), L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"AUTHENTICATION\"" +
                    "GROUP BY L.uid) as UL\n" +
                    "WHERE UL.numFail > ?;";
            try {
                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setInt(1, FAILURE_PER_UID_PER_DAY);
                ResultSet rs = getFailure.executeQuery();
                ArrayList<Integer> attackedUid = new ArrayList<>();
                while (rs.next()) {
                    attackedUid.add(rs.getInt(1));
                }
                return attackedUid;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getFailure != null) {
                    getFailure.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
