package org.cs5431_client.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;

public class SQL_Connection {

    private static int port = 3306;
    private static String ip = "localhost";

    public SQL_Connection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    private static int createUser() {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";
        String username = "admin";
        String password = "$walaotwod0tseven$";

        System.out.println("Connecting database...");
        int uid = 0;

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("Database connected!");
            PreparedStatement createUser = null;
            PreparedStatement createFolder = null;
            PreparedStatement createLog = null;

            String insertUser =  "INSERT INTO Users (uid, username, pwd, parentFolderid, email, privKey, pubKey)"
                    + " values (?, ?, ?, ?, ?, ?, ?)";
            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, parentFolderid, fsoName, size, " +
                    "lastModified, isFile)"
                    + " values (?, ?, ?, ?, ?, ?)";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                    + "values (?, ?, ?, ?, ?)";

            try {
                connection.setAutoCommit(false);
                createFolder = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                createUser = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
                createLog = connection.prepareStatement(insertLog);

                Timestamp currDate = new Timestamp(System.currentTimeMillis());
                createFolder.setInt (1, 0);
                createFolder.setInt (2, 1);
                createFolder.setString (3, "myFolderName");
                createFolder.setString   (4, Integer.toString(10));
                createFolder.setTimestamp (5, currDate);
                createFolder.setBoolean    (6, true);
                createFolder.executeUpdate();
                System.out.println("created folder");

                ResultSet rs = createFolder.getGeneratedKeys();
                rs.next();
                int folderid = rs.getInt(1);
                createUser.setInt (1, 0);
                createUser.setString (2, "myUsername");
                createUser.setString (3, " myPassword");
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
                createLog.setInt (2, folderid);
                createLog.setInt(3, uid);
                createLog.setTimestamp(4, currDate);
                createLog.setString(5, "CREATE_USER");
                createLog.executeUpdate();
                System.out.println("created log");

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
                connection.setAutoCommit(true);
                return uid;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }

    }

    private static int createFso() {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431";
        String username = "admin";
        String password = "$walaotwod0tseven$";

        System.out.println("Connecting database...");
        int fsoid = 0;

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("Database connected!");
            PreparedStatement createFolder = null;
            PreparedStatement createLog = null;

            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, parentFolderid, fsoName, size, " +
                    "lastModified, isFile)"
                    + " values (?, ?, ?, ?, ?, ?)";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                    + "values (?, ?, ?, ?, ?)";

            try {
                connection.setAutoCommit(false);
                createFolder = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                createLog = connection.prepareStatement(insertLog);

                Timestamp currDate = new Timestamp(System.currentTimeMillis());
                createFolder.setInt (1, 0);
                createFolder.setInt (2, 1);
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
                createLog.setInt(3, 2); //TODO: update UID
                createLog.setTimestamp(4, currDate);
                createLog.setString(5, "CREATE_FOLDER");
                createLog.executeUpdate();
                System.out.println("created log");

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
                connection.setAutoCommit(true);
                return fsoid;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    public static void main(String[] args) {
        //Connection connection = connectToDB();
        System.out.print(createFso());
    }

}