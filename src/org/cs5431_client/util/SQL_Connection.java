package org.cs5431_client.util;

import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;
import java.util.ArrayList;


public class SQL_Connection {

    private static int port = 3306;
    private static String ip = "localhost";
    private static String DB_USER = "admin";
    private static String DB_PASSWORD = "$walaotwod0tseven$";

    public SQL_Connection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    private static  boolean isUniqueUsername(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";

        System.out.println("Connecting to database...");

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connected!");
            PreparedStatement verifyUniqueness = null;

            String checkUsername = "SELECT U.uid FROM Users U WHERE U.username = ?";
            verifyUniqueness = connection.prepareStatement(checkUsername);

            try {
                verifyUniqueness.setString(1, username);
                ResultSet rs = verifyUniqueness.executeQuery();
                if (rs.next()) {
                    System.out.println("The username is not unique");
                    return false;
                } else {
                    System.out.println("The username is unique");
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (verifyUniqueness != null) {
                    verifyUniqueness.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static int createUser() {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";

        System.out.println("Connecting to database...");
        int uid = 0;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connected!");
            PreparedStatement createUser = null;
            PreparedStatement createFolder = null;
            PreparedStatement createLog = null;
            PreparedStatement addPermission = null;

            String insertUser =  "INSERT INTO Users (uid, username, pwd, parentFolderid, email, privKey, pubKey)"
                    + " values (?, ?, ?, ?, ?, ?, ?)";
            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, parentFolderid, fsoName, size, " +
                    "lastModified, isFile)"
                    + " values (?, ?, ?, ?, ?, ?)";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, lastModified, actionType)"
                    + "values (?, ?, ?, ?)";
            String insertEditor = "INSERT INTO Editors (fsoid, uid) values (?, ?)";

            try {
                connection.setAutoCommit(false);
                createFolder = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                createUser = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
                createLog = connection.prepareStatement(insertLog);
                addPermission = connection.prepareStatement(insertEditor);

                Timestamp currDate = new Timestamp(System.currentTimeMillis());
                createFolder.setInt (1, 0);
                createFolder.setInt (2, 1);
                createFolder.setString (3, "myFolderName");
                createFolder.setString   (4, Integer.toString(10));
                createFolder.setTimestamp (5, currDate);
                createFolder.setBoolean    (6, false);
                createFolder.executeUpdate();
                System.out.println("created folder");

                ResultSet rs = createFolder.getGeneratedKeys();
                rs.next();
                int folderid = rs.getInt(1);
                createUser.setInt (1, 0);
                createUser.setString (2, "myUsername");
                createUser.setString (3, "myPassword");
                createUser.setInt   (4, folderid);
                createUser.setString (5, "myEmail");
                createUser.setString    (6, "myPrivKey");
                createUser.setString    (7, "myPubKey");
                createUser.executeUpdate();
                System.out.println("created user");

                rs = createUser.getGeneratedKeys();
                rs.next();
                uid = rs.getInt(1);
                createLog.setInt(1, 0);
                createLog.setInt(2, uid);
                createLog.setTimestamp(3, currDate);
                createLog.setString(4, "CREATE_USER");
                createLog.executeUpdate();
                System.out.println("created log");

                addPermission.setInt(1, folderid);
                addPermission.setInt(2, uid);
                addPermission.executeUpdate();
                System.out.println("added owner as editor");

                connection.commit();

            } catch (SQLException e ) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                    } catch(SQLException excep) {
                        excep.printStackTrace();
                    }
                }
            } finally {
                if (createFolder != null) {
                    createFolder.close();
                }
                if (createUser != null) {
                    createUser.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                if (addPermission != null) {
                    addPermission.close();
                }
                connection.setAutoCommit(true);
                return uid;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    private static int createFso(JSONObject fso) {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";

        System.out.println("Connecting to database...");
        int fsoid = 0;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connected!");
            PreparedStatement createFolder = null;
            PreparedStatement createLog = null;
            PreparedStatement addPermission = null;

            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, parentFolderid, fsoName, size, " +
                    "lastModified, isFile)"
                    + " values (?, ?, ?, ?, ?, ?)";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                    + "values (?, ?, ?, ?, ?)";
            String insertEditor = "INSERT INTO Editors (fsoid, uid) values (?, ?)";

            try {
                connection.setAutoCommit(false);
                createFolder = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                createLog = connection.prepareStatement(insertLog);
                addPermission = connection.prepareStatement(insertEditor);

                Timestamp currDate = new Timestamp(System.currentTimeMillis());
                createFolder.setInt (1, 0);
                createFolder.setInt (2, 2);
                createFolder.setString (3, "myFolderName");
                createFolder.setString   (4, Integer.toString(10));
                createFolder.setTimestamp (5, currDate);
                createFolder.setBoolean    (6, true);
                createFolder.executeUpdate();
                System.out.println("created folder");

                ResultSet rs = createFolder.getGeneratedKeys();
                rs.next();
                fsoid = rs.getInt(1);
                String actionType;
                //TODO: if isFolder then set action type as CREATE_FOLDER; otherwise, UPLOAD_FILE

                createLog.setInt(1, 0);
                createLog.setInt (2, fsoid);
                createLog.setInt(3, 2); //TODO: update uid
                createLog.setTimestamp(4, currDate);
                createLog.setString(5, "CREATE_FOLDER");
                createLog.executeUpdate();
                System.out.println("created log");

                addPermission.setInt(1, fsoid);
                addPermission.setInt(2, 2); //TODO: update UID
                addPermission.executeUpdate();
                System.out.println("added owner as editor");

                connection.commit();

            } catch (SQLException e ) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                    } catch(SQLException excep) {
                        excep.printStackTrace();
                    }
                }
            } finally {
                if (createFolder != null) {
                    createFolder.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                if (addPermission != null) {
                    addPermission.close();
                }
                connection.setAutoCommit(true);
                return fsoid;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    /** Compares username and encrypted password with row of User table.
     * @Return h(privKey) of the user if the authentication is valid. **/
    private static String authenticate(String username, String encPwd) {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";

        System.out.println("Connecting to database...");

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connected!");
            PreparedStatement verifyUser = null;

            String checkPassword = "SELECT U.privKey FROM Users U WHERE U.username = ? AND U.pwd = ?";
            verifyUser = connection.prepareStatement(checkPassword);
            //TODO: salting?

            try {
                verifyUser.setString(1, username);
                verifyUser.setString(2, encPwd);
                ResultSet rs = verifyUser.executeQuery();

                if (rs.next()) {
                    //user valid
                    System.out.println("Valid user");
                    String privKey = rs.getString(1);
                    return privKey;
                }
            } finally {
                if (verifyUser != null) {
                    verifyUser.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //TODO: log the number of failed authentications?
        System.out.println("Invalid user");
        return null;
    }

    private static ArrayList<JSONObject> getChildren(int parentFolderid, int uid) {

        ArrayList<JSONObject> files = new ArrayList<>();
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";

        System.out.println("Connecting to database...");

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connected!");
            PreparedStatement getFiles = null;

            String selectFiles = "SELECT F.fsoid, F.fsoName, F.size, F.lastModified, F.isFile " +
                    "FROM FileSystemObjects F " +
                    "WHERE F.parentFolderid = ? AND EXISTS" +
                    "(SELECT * FROM Editors E WHERE E.uid=?) OR EXISTS" +
                    "(SELECT * FROM Viewers V WHERE V.uid=?);";
            getFiles = connection.prepareStatement(selectFiles);
            //TODO: salting?

            try {
                getFiles.setInt(1, parentFolderid);
                getFiles.setInt(2, uid);
                getFiles.setInt(3, uid);
                ResultSet rs = getFiles.executeQuery();
                while (rs.next()) {
                    JSONObject fso = new JSONObject();
                    fso.put("id", rs.getInt(1));
                    fso.put("name", rs.getString(2));
                    fso.put("size", rs.getString(3));
                    fso.put("lastModified", rs.getTimestamp(4));

                    if (rs.getBoolean(5)) {
                        fso.put("FSOType", "FILE");
                    } else {
                        fso.put("FSOType", "FOLDER");
                    }
                    files.add(fso);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (getFiles != null) {
                    getFiles.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return files;
    }

    public static void main(String[] args) {
        //Connection connection = connectToDB();
        System.out.print(getChildren(2, 2));
    }

}
