package org.cs5431;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class IntrusionDetection {
    private int port;
    private String ip;
    private String DB_USER;
    private String DB_PASSWORD;
    private int FAILURE_PER_UID_PER_DAY = 20;
    private int FAILURE_PER_IP_PER_DAY = 20;
    private int TOTAL_FAILED_LOGIN_PER_DAY = 200;

    public IntrusionDetection(String ip, int dbPort, String username, String password) {
        this.ip = ip;
        this.port = dbPort;
        this.DB_USER = username;
        this.DB_PASSWORD = password;

        String log = "OK, no intrusion has been detected.";

        List<String> attackerIp = noPermissionAlert();
        if (!attackerIp.isEmpty()) {
            //System.out.println("INTRUSION DETECTED. IP(s) that attempted to access the system without permission: " + attackerIp);
            log = "INTRUSION DETECTED. IP(s) that attempted to access the system without permission: " + attackerIp;
        }

        attackerIp = getFailedAuthPerIp();
        if (!attackerIp.isEmpty()) {
            //System.out.println("INTRUSION DETECTED. Multiple failed authentication attempts from the same IP(s): " + attackerIp);
            log = "INTRUSION DETECTED. Multiple failed authentication attempts from the same IP(s): " + attackerIp;
        }

        attackerIp = getFailedLoginsPerIp();
        if (!attackerIp.isEmpty()) {
            //System.out.println("INTRUSION DETECTED. Multiple failed login attempts from the same IP(s): " + attackerIp);
            log = "INTRUSION DETECTED. Multiple failed login attempts from the same IP(s): " + attackerIp;
        }

        List<Integer> attackedUid = getAttemptedUidFailure();
        if (!attackedUid.isEmpty()) {
            //System.out.println("INTRUSION DETECTED. Attempts to impersonate the following user(s): " + attackedUid);
            log = "INTRUSION DETECTED. Attempts to impersonate the following user(s): " + attackedUid;
        }

        attackedUid = getFailedAuthPerUid();
        if (!attackedUid.isEmpty()) {
            //System.out.println("INTRUSION DETECTED. Multiple failed authentication attempts on the same uid(s): " + attackedUid);
            log = "INTRUSION DETECTED. Multiple failed authentication attempts on the same uid(s): " + attackedUid;
        }

        attackedUid = getFailedLoginsPerUid();
        if (!attackedUid.isEmpty()) {
            //System.out.println("INTRUSION DETECTED. Multiple failed login attempts on the same uid(s): " + attackedUid);
            log = "INTRUSION DETECTED. Multiple failed login attempts on the same uid(s): " + attackedUid;
        }

        if (totalFailedLogins() > TOTAL_FAILED_LOGIN_PER_DAY) {
            //System.out.println("INTRUSION DETECTED. An excessive number of failed login attempts.");
            log = "INTRUSION DETECTED. An excessive number of failed login attempts.";
        }

        File intrusionLog = new File("./intrusion-log.txt");
        if (!intrusionLog.exists()) {
            try {
                boolean createdNewFile = intrusionLog.createNewFile();
                if (createdNewFile) {
                    FileWriter fw;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        fw = new FileWriter(intrusionLog, true);
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write(sdf.format(new java.util.Date()) + " " + log);
                        bw.newLine();
                        bw.flush();
                        fw.flush();
                        bw.close();
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //any ip from this list should be banned
    public List<String> noPermissionAlert() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            PreparedStatement getNoPermission = null;
            PreparedStatement getLast = null;

            String selectUL = "SELECT L.sourceIp FROM FileLog L WHERE L.failureType = \"NO PERMISSION\"" +
                    "AND time_to_sec(timediff(?, L.lastModified )) / 3600 < 24";

            String selectLastTime = "SELECT MAX(lastModified) FROM UserLog;";

            try {
                Timestamp latestTime = new Timestamp(System.currentTimeMillis());
                getLast = connection.prepareStatement(selectLastTime);
                ResultSet rs = getLast.executeQuery();
                if (rs.next()) {
                    latestTime = rs.getTimestamp(1);
                }
                getNoPermission = connection.prepareStatement(selectUL);
                getNoPermission.setTimestamp(1, latestTime);
                rs = getNoPermission.executeQuery();
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
                if (getLast != null) {
                    getLast.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getFailedLoginsPerIp() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            PreparedStatement getFailure = null;
            PreparedStatement getLast = null;
            //currently 20 fails from the same ip per day
            String selectFailure = "SELECT UL.sourceIp FROM\n" +
                    "(SELECT L.sourceIp, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(?, L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"LOGIN\"" +
                    "GROUP BY L.sourceIp) as UL\n" +
                    "WHERE UL.numFail > ?;";
            String selectLastTime = "SELECT MAX(lastModified) FROM UserLog;";

            try {
                Timestamp latestTime = new Timestamp(System.currentTimeMillis());
                getLast = connection.prepareStatement(selectLastTime);
                ResultSet rs = getLast.executeQuery();
                if (rs.next()) {
                    latestTime = rs.getTimestamp(1);
                }

                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setTimestamp(1, latestTime);
                getFailure.setInt(2, FAILURE_PER_IP_PER_DAY);
                rs = getFailure.executeQuery();
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
                if (getLast != null) {
                    getLast.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Integer> getFailedLoginsPerUid() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            PreparedStatement getFailure = null;
            PreparedStatement getLast = null;
            //currently 20 fails from the same uid per day
            String selectFailure = "SELECT UL.uid FROM\n" +
                    "(SELECT L.uid, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(?, L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"LOGIN\"" +
                    "GROUP BY L.uid) as UL\n" +
                    "WHERE UL.numFail > ?;";
            String selectLastTime = "SELECT MAX(lastModified) FROM UserLog;";

            try {
                Timestamp latestTime = new Timestamp(System.currentTimeMillis());
                getLast = connection.prepareStatement(selectLastTime);
                ResultSet rs = getLast.executeQuery();
                if (rs.next()) {
                    latestTime = rs.getTimestamp(1);
                }

                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setTimestamp(1, latestTime);
                getFailure.setInt(2, FAILURE_PER_UID_PER_DAY);
                rs = getFailure.executeQuery();
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
                if (getLast != null) {
                    getLast.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int totalFailedLogins() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            PreparedStatement getNoPermission = null;
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

    public List<Integer> getAttemptedUidFailure() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getNoPermission = null;
        PreparedStatement getLast = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {

            String selectUL = "SELECT L.uid FROM UserLog L WHERE L.actionType = \"ATTEMPTED_UID\" " +
                    "AND time_to_sec(timediff(?, L.lastModified )) / 3600 < 24";
            String selectLastTime = "SELECT MAX(lastModified) FROM UserLog;";

            try {
                Timestamp latestTime = new Timestamp(System.currentTimeMillis());
                getLast = connection.prepareStatement(selectLastTime);
                ResultSet rs = getLast.executeQuery();
                if (rs.next()) {
                    latestTime = rs.getTimestamp(1);
                }

                getNoPermission = connection.prepareStatement(selectUL);
                getNoPermission.setTimestamp(1, latestTime);
                rs = getNoPermission.executeQuery();
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
                if (getLast != null) {
                    getLast.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Authentication comes from change password and delete user
    public List<String> getFailedAuthPerIp() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            PreparedStatement getFailure = null;
            PreparedStatement getLast = null;
            //currently 20 fails from the same ip per day
            String selectFailure = "SELECT UL.sourceIp FROM\n" +
                    "(SELECT L.sourceIp, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(?, L.lastModified)) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"AUTHENTICATION\"" +
                    "GROUP BY L.sourceIp) as UL\n" +
                    "WHERE UL.numFail > ?;";
            String selectLastTime = "SELECT MAX(lastModified) FROM UserLog;";

            try {
                Timestamp latestTime = new Timestamp(System.currentTimeMillis());
                getLast = connection.prepareStatement(selectLastTime);
                ResultSet rs = getLast.executeQuery();
                if (rs.next()) {
                    latestTime = rs.getTimestamp(1);
                }

                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setTimestamp(1, latestTime);
                getFailure.setInt(2, FAILURE_PER_IP_PER_DAY);
                rs = getFailure.executeQuery();
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
                if (getLast != null) {
                    getLast.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Integer> getFailedAuthPerUid() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            PreparedStatement getFailure = null;
            PreparedStatement getLast = null;
            //currently 20 fails from the same uid per day
            String selectLastTime = "SELECT MAX(lastModified) FROM UserLog;";
            String selectFailure = "SELECT UL.uid FROM\n" +
                    "(SELECT L.uid, COUNT(*) as numFail FROM UserLog L " +
                    "WHERE time_to_sec(timediff(?, L.lastModified )) / 3600 < 24 AND L.status = \"FAILURE\" " +
                    "AND L.actionType = \"AUTHENTICATION\"" +
                    "GROUP BY L.uid) as UL\n" +
                    "WHERE UL.numFail > ?;";
            try {
                Timestamp latestTime = new Timestamp(System.currentTimeMillis());
                getLast = connection.prepareStatement(selectLastTime);
                ResultSet rs = getLast.executeQuery();
                if (rs.next()) {
                    latestTime = rs.getTimestamp(1);
                }

                getFailure = connection.prepareStatement(selectFailure);
                getFailure.setTimestamp(1, latestTime);
                getFailure.setInt(2, FAILURE_PER_UID_PER_DAY);
                rs = getFailure.executeQuery();
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
                if (getLast != null) {
                    getLast.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
