package org.cs5431;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IntrusionDetection {
    private int port;
    private String ip;
    private String DB_USER;
    private String DB_PASSWORD;

    IntrusionDetection(String ip, int dbPort, String username, String password) {
        this.ip = ip;
        this.port = dbPort;
        this.DB_USER = username;
        this.DB_PASSWORD = password;
    }

    //any ip from this list should be banned
    List<String> noPermissionAlert() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getNoPermission = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            String selectUL = "SELECT L.sourceIp FROM FileLog L WHERE L.failureType = \"NO PERMISSION\"";

            try {
                getNoPermission = connection.prepareStatement(selectUL);
                ResultSet rs = getNoPermission.executeQuery();
                ArrayList<String> attackIp = new ArrayList<>();
                if (rs.next()) {
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
                    "WHERE UL.numFail > 20;";
            try {
                getFailure = connection.prepareStatement(selectFailure);
                ResultSet rs = getFailure.executeQuery();
                ArrayList<String> attackIp = new ArrayList<>();
                if (rs.next()) {
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
                    "WHERE UL.numFail > 20;";
            try {
                getFailure = connection.prepareStatement(selectFailure);
                ResultSet rs = getFailure.executeQuery();
                ArrayList<Integer> attackedUid = new ArrayList<>();
                if (rs.next()) {
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
}
