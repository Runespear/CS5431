package org.cs5431;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;
import java.util.Base64;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.secondPwdHash;

public class SQL_Accounts {

    private int port;
    private String ip;
    private String DB_USER;
    private String DB_PASSWORD;

    SQL_Accounts(String ip, int dbPort, String username, String
            password) {
        this.ip = ip;
        this.port = dbPort;
        this.DB_USER = username;
        this.DB_PASSWORD = password;
    }

    /** Determines if the username already exists in the database.
     * @param username is the username to be checked in the database.
     * @return true if the username does not exist; false otherwise. */
    boolean isUniqueUsername(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement verifyUniqueness = null;

            String checkUsername = "SELECT U.uid FROM Users U WHERE U.username = ?";

            try {
                verifyUniqueness = connection.prepareStatement(checkUsername);
                verifyUniqueness.setString(1, username);
                ResultSet rs = verifyUniqueness.executeQuery();
                if (rs.next()) {
                    if (DEBUG_MODE) {
                        System.out.println("The username is not unique");
                    }
                    return false;
                } else {
                    if (DEBUG_MODE) {
                        System.out.println("The username is unique");
                    }
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
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

    /** Adds the user into the db. Creates a parent folder for the user where he/she is an editor.
     * Adds a user log to log the registration of the user.
     * Creates user log of failure if there is an error in the registration and rolls back the transaction.
     * @param user json containing the details of the user to be added
     * @param hashedPwd hashed password of the user that is to be created
     * @param pwdSalt salt of the password that was used
     * @return json containing registrationAck and details of the user added (refer to protocols doc)
     * */
    JSONObject createUser(JSONObject user, String hashedPwd, String pwdSalt, String sourceIp) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        int uid;
        JSONObject jsonUser;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement createUser = null;
            PreparedStatement createFolder = null;
            PreparedStatement createLog = null;
            PreparedStatement addPermission = null;

            String insertUser =  "INSERT INTO Users (uid, username, pwd, parentFolderid, email, privKey, " +
                    "pubKey, pwdSalt, privKeySalt) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, fsoName, size, " +
                    "lastModified, isFile)"
                    + " values (?, ?, ?, ?, ?)";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?)";
            String insertEditor = "INSERT INTO Editors (fsoid, uid) values (?, ?)";

            String username = user.getString("username");
            String pubKey = user.getString("pubKey");
            String privKey = user.getString("privKey");
            String privKeySalt = user.getString("privKeySalt");

            String email = null;
            if (user.has("email")) {
                email = (String) user.get("email");
            }

            try {
                connection.setAutoCommit(false);
                createFolder = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                createUser = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
                createLog = connection.prepareStatement(insertLog);
                addPermission = connection.prepareStatement(insertEditor);

                Timestamp currDate = new Timestamp(System.currentTimeMillis());
                createFolder.setInt (1, 0);
                createFolder.setString (2, username);
                createFolder.setString   (3, Integer.toString(0));
                createFolder.setTimestamp (4, currDate);
                createFolder.setBoolean    (5, false);

                createFolder.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("created folder");
                }

                ResultSet rs = createFolder.getGeneratedKeys();
                rs.next();
                int folderid = rs.getInt(1);
                createUser.setInt (1, 0);
                createUser.setString (2, username);
                createUser.setString (3, hashedPwd);
                createUser.setInt   (4, folderid);
                createUser.setString (5, email);
                createUser.setString    (6, privKey);
                createUser.setString    (7, pubKey);
                createUser.setString    (8, pwdSalt);
                createUser.setString    (9, privKeySalt);
                createUser.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("created user");
                }
                rs = createUser.getGeneratedKeys();
                rs.next();
                uid = rs.getInt(1);
                createLog.setInt(1, 0);
                createLog.setInt(2, uid);
                createLog.setTimestamp(3, currDate);
                createLog.setString(4, "CREATE_USER");
                createLog.setString(5, "SUCCESS");
                createLog.setString(6, sourceIp);
                createLog.setString(7, null);
                createLog.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("created log");
                }

                addPermission.setInt(1, folderid);
                addPermission.setInt(2, uid);
                addPermission.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("added owner as editor");
                }

                connection.commit();

                File userDir = new File("./files/"+uid+"/");
                if (!userDir.exists()) {
                    if (!userDir.mkdir())
                        return null;
                }

                jsonUser = new JSONObject();
                jsonUser.put("msgType", "registrationAck");
                jsonUser.put("username", username);
                jsonUser.put("uid", uid);
                jsonUser.put("parentFolderid", folderid);
                jsonUser.put("email", email);
                jsonUser.put("privKey", privKey);
                jsonUser.put("privKeySalt", privKeySalt);
                jsonUser.put("pubKey", pubKey);
                return jsonUser;

            } catch (SQLException e ) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        Timestamp currDate = new Timestamp(System.currentTimeMillis());
                        createLog.setInt(1, 0);
                        createLog.setInt(2, 0);
                        createLog.setTimestamp(3, currDate);
                        createLog.setString(4, "CREATE_USER");
                        createLog.setString(5, "FAILURE");
                        createLog.setString(6, sourceIp);
                        createLog.setString(7, "DB ERROR");
                        createLog.executeUpdate();
                        if (DEBUG_MODE) {
                            System.out.println("created failure log");
                        }
                    } catch(SQLException excep) {
                        excep.printStackTrace();
                    }
                }
                return null;
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
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean logSessionLimit(String sourceIp) {
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
                addLog.setString(7, sourceIp);
                addLog.setString(8, "TOO MANY FAILED LOGINS");
                addLog.executeUpdate();
                return true;
            } catch (SQLException e1) {
                e1.printStackTrace();
                return false;
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

    /** Compares username and encrypted password with row of User table.
     * Creates a failure login log if the user's password and username does not match. (username is valid).
     * Creates a success login log upon success.
     * @return h(privKey) of the user if the authentication is valid. **/
    JSONObject authenticate(JSONObject allegedUser, String encPwd, String sourceIp, String action, Email adminEmail) {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        JSONObject user = new JSONObject();

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement verifyUser = null;
            PreparedStatement addLog = null;
            PreparedStatement limitIp = null;
            PreparedStatement limitUsername = null;
            PreparedStatement getEmail = null;

            String checkPassword = "SELECT U.uid, U.parentFolderid, U.email, U.privKey, U.pubKey, U.privKeySalt" +
                    " FROM Users U WHERE U.username = ? AND U.pwd = ?";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";
            String countIp = "SELECT COUNT(*) FROM UserLog U \n" +
                    "WHERE U.lastModified > DATE_SUB(now(), INTERVAL 5 MINUTE) \n" +
                    "AND U.actionType = \"LOGIN\" AND U.status = \"FAILURE\" \n" +
                    "AND U.sourceIp = ?;";
            String countUsername = "SELECT COUNT(*) FROM UserLog U \n" +
                    "WHERE U.lastModified > DATE_SUB(now(), INTERVAL 5 MINUTE) \n" +
                    "AND U.actionType = \"LOGIN\" AND U.status = \"FAILURE\" \n" +
                    "AND U.simulatedUsername = ?;";
            String selectEmail = "SELECT U.email FROM Users U WHERE U.username = ?";

            String username = allegedUser.getString("username");

            try {
                int numIp = 0;
                int numUsername = 0;
                addLog = connection.prepareStatement(insertLog);
                verifyUser = connection.prepareStatement(checkPassword);
                limitIp = connection.prepareStatement(countIp);
                limitUsername = connection.prepareStatement(countUsername);

                connection.setAutoCommit(false);
                limitIp.setString(1, sourceIp);
                ResultSet rs = limitIp.executeQuery();

                if (rs.next()) {
                    numIp = rs.getInt(1);
                }

                if (numIp >= 5) {
                    addLog.setInt(1, 0);
                    addLog.setInt(2, 0);
                    addLog.setString(3, username);
                    addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    addLog.setString(5, action);
                    addLog.setString(6, "FAILURE");
                    addLog.setString(7, sourceIp);
                    addLog.setString(8, "IP TOO MANY FAILED LOGINS");
                    addLog.executeUpdate();
                    connection.commit();
                    return null; //TODO: what to return
                }

                limitUsername.setString(1, username);
                rs = limitUsername.executeQuery();

                if (rs.next()) {
                    numUsername = rs.getInt(1);
                }

                if (numUsername >= 5) {
                    addLog.setInt(1, 0);
                    addLog.setInt(2, 0);
                    addLog.setString(3, username);
                    addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    addLog.setString(5, action);
                    addLog.setString(6, "FAILURE");
                    addLog.setString(7, sourceIp);
                    addLog.setString(8, "USERNAME TOO MANY FAILED LOGINS");
                    addLog.executeUpdate();
                    connection.commit();
                    return null; //TODO: what to return
                }

                verifyUser.setString(1, username);
                verifyUser.setString(2, encPwd);
                rs = verifyUser.executeQuery();

                if (rs.next()) {
                    //user valid
                    if (DEBUG_MODE) {
                        System.out.println("Valid user");
                    }
                    int uid = rs.getInt(1);
                    int parentFolderid = rs.getInt(2);
                    String email = rs.getString(3);
                    String privKey = rs.getString(4);
                    String pubKey = rs.getString(5);
                    String privKeySalt = rs.getString(6);
                    user.put("msgType", "loginAck");
                    user.put("uid", uid);
                    user.put("parentFolderid", parentFolderid);
                    user.put("email", email);
                    user.put("privKey", privKey);
                    user.put("pubKey", pubKey);
                    user.put("privKeySalt", privKeySalt);

                    addLog.setInt(1, 0);
                    addLog.setInt(2, uid);
                    addLog.setString(3, username);
                    addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    addLog.setString(5, action);
                    addLog.setString(6, "SUCCESS");
                    addLog.setString(7, sourceIp);
                    addLog.setString(8, null);
                    addLog.execute();

                    connection.commit();
                    return user;
                } else {
                    addLog.setInt(1, 0);
                    addLog.setInt(2, 0);
                    addLog.setString(3, username);
                    addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    addLog.setString(5, action);
                    addLog.setString(6, "FAILURE");
                    addLog.setString(7, sourceIp);
                    addLog.setString(8, "INVALID PASSWORD");
                    if (DEBUG_MODE) {
                        System.out.println("invalid login");
                    }
                    addLog.execute();

                    getEmail = connection.prepareStatement(selectEmail);
                    getEmail.setString(0, username);
                    ResultSet userEmail = getEmail.executeQuery();
                    if (userEmail.next()) {
                        adminEmail.send(userEmail.getString(0),"Failed Login Attempt", "");
                    }
                    connection.commit();
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (verifyUser != null) {
                    verifyUser.close();
                }
                if (addLog != null) {
                    addLog.close();
                }
                if (limitIp != null) {
                    limitIp.close();
                }
                if (limitUsername != null) {
                    limitUsername.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DEBUG_MODE) {
            System.out.println("Invalid user");
        }
        return null;
    }

    String getPrivKeySalt(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false?autoReconnect=true&useSSL=false";

        System.out.println("Connecting to database...");

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connected!");
            PreparedStatement getSalt = null;

            String selectSalt = "SELECT U.privKeySalt FROM Users U WHERE U.username = ?";
            String salt = null;

            try {
                getSalt = connection.prepareStatement(selectSalt);
                getSalt.setString(1, username);
                ResultSet rs = getSalt.executeQuery();
                if (rs.next()) {
                    salt = rs.getString(1);
                }
                return salt;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (getSalt != null) {
                    getSalt.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Gets pwdSalt of pwd associated with username.
     * Creates a failed login log if the username does not exist.
     * @return salt of password associated with username */
    String getSalt(String username, String sourceIp, String action) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement getSalt = null;
            PreparedStatement addLog = null;

            String selectSalt = "SELECT U.pwdSalt FROM Users U WHERE U.username = ?";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";
            String salt = null;

            try {
                addLog = connection.prepareStatement(insertLog);
                getSalt = connection.prepareStatement(selectSalt);
                getSalt.setString(1, username);
                ResultSet rs = getSalt.executeQuery();
                if (rs.next()) {
                    salt = rs.getString(1);
                } else {
                    addLog.setInt(1, 0);
                    addLog.setInt(2, 0);
                    addLog.setString(3, username);
                    addLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    addLog.setString(5, action);
                    addLog.setString(6, "FAILURE");
                    addLog.setString(7, sourceIp);
                    addLog.setString(8, "INVALID USERNAME");
                    addLog.execute();
                }
                return salt;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (getSalt != null) {
                    getSalt.close();
                }
                if (addLog != null) {
                    addLog.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Deletes the user with uid, without knowing the password of the user.
     * This can only be done by the admin.
     * @return the uid of the user that is deleted; -1 if unsuccessful deletion.
     */
    int adminDeleteUser(int uid, String sourceIp) {
        int parentFolderid = getParentFolderid(uid);
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            PreparedStatement removeUser = null;
            PreparedStatement createLog = null;
            PreparedStatement removeFolder = null;

            String deleteUser = "DELETE FROM Users WHERE uid = ?";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";
            String deleteFolder = "DELETE FROM FileSystemObjects WHERE fsoid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                removeUser = connection.prepareStatement(deleteUser);
                removeFolder = connection.prepareStatement(deleteFolder);
                connection.setAutoCommit(false);

                removeUser.setInt(1, uid);
                int affectedRows = removeUser.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("deleted user");
                }
                if (affectedRows == 1) {
                    removeFolder.setInt(1, parentFolderid);
                    removeFolder.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, null);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADMIN_DELETE_USER");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, null);
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }
                    connection.commit();
                    return uid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, 0);
                    createLog.setString(3, null);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADMIN_DELETE_USER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "INVALID USER");
                    createLog.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                try {
                    System.err.println("Transaction is being rolled back");
                    connection.rollback();
                    createLog.setInt(1, 0);
                    createLog.setInt(2, 0);
                    createLog.setString(3, null);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADMIN_DELETE_USER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "DB ERROR");
                    createLog.executeUpdate();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
                return -1;
            } finally {
                if (removeUser != null) {
                    removeUser.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                if (removeFolder != null) {
                    removeFolder.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //how does an admin delete user?
    /** Deletes the user with uid. To be first authenticated using username and password.
     * Creates a log entry of the deletion of user.
     * @return the uid of the user that is deleted; -1 if unsuccessful deletion. */
    int deleteUser(int uid, String username, String password, String sourceIp) {
        JSONObject allegedUser = new JSONObject();
        allegedUser.put("username", username);
        String salt = getSalt(username, sourceIp, "DELETE_USER");
        String encPwd = secondPwdHash(password, Base64.getDecoder().decode(salt));
        JSONObject user = authenticate(allegedUser, encPwd, sourceIp, "AUTHENTICATE", null);
        int parentFolderid = -1;
        if (user != null) {
            parentFolderid = getParentFolderid(uid);
        }
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            PreparedStatement removeUser = null;
            PreparedStatement createLog = null;
            PreparedStatement removeFolder = null;

            String deleteUser = "DELETE FROM Users WHERE uid = ? AND username = ? AND pwd = ?";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";
            String deleteFolder = "DELETE FROM FileSystemObjects WHERE fsoid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (user != null) {
                    removeUser = connection.prepareStatement(deleteUser);
                    removeFolder = connection.prepareStatement(deleteFolder);
                    connection.setAutoCommit(false);
                    removeUser.setInt(1, uid);
                    removeUser.setString(2, username);
                    removeUser.setString(3, password);
                    removeUser.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("deleted user");
                    }

                    removeFolder.setInt(1, parentFolderid);
                    removeFolder.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, username);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "DELETE_USER");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, null);
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }
                    connection.commit();
                    return uid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, username);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "DELETE_USER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "INVALID PASSWORD");
                    createLog.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                try {
                    System.err.println("Transaction is being rolled back");
                    connection.rollback();
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, username);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "DELETE_USER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "DB ERROR");
                    createLog.executeUpdate();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
                return -1;
            } finally {
                if (removeUser != null) {
                    removeUser.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                if (removeFolder != null) {
                    removeFolder.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int


    getParentFolderid (int uid) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }

            PreparedStatement getParentFolder = null;

            String selectParent = "SELECT U.parentFolderid FROM Users U WHERE U.uid = ?";
            try {
                getParentFolder = connection.prepareStatement(selectParent);
                getParentFolder.setInt(1, uid);
                ResultSet rs = getParentFolder.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            } finally {
                if (getParentFolder != null) {
                    getParentFolder.close();
                }
                if (getParentFolder != null) {
                    getParentFolder.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    /** Changes the password of the user and the privKey which is encrypted by the password of the user.
     * Authenticates the user with his/her old password before making any changes.
     * Logs the change of password in the userlog.
     * Creates failure file log invalid password or db error (rollsback accordingly).
     * @param newEncPwd the new encrypted version of the password.
     * @param allegedUser json with the credentials of the user to be modified along
     *                    a new privKey.
     * @return json with changePwdAck and the uid; if user is not authenticated, return null. */
    JSONObject changePassword(JSONObject allegedUser, String newEncPwd, String sourceIp) {
        int uid = allegedUser.getInt("uid");
        String username = allegedUser.getString("username");
        String password = allegedUser.getString("hashedPwd");
        String newPrivKey = allegedUser.getString("newPrivKey");
        String salt = getSalt(username, sourceIp, "CHANGE_PWD");
        String encPwd = secondPwdHash(password, Base64.getDecoder().decode(salt));
        JSONObject user = authenticate(allegedUser, encPwd, sourceIp, "AUTHENTICATE", null);

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement changePwd = null;
            PreparedStatement createLog = null;
            JSONObject response = new JSONObject();
            String updatePwd = "UPDATE Users SET pwd = ?, privKey = ? WHERE uid = ? AND username = ?";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";
            try {
                createLog = connection.prepareStatement(insertLog);
                if (user != null) {
                    changePwd = connection.prepareStatement(updatePwd);
                    connection.setAutoCommit(false);
                    changePwd.setString(1, newEncPwd);
                    changePwd.setString(2, newPrivKey);
                    changePwd.setInt(3, uid);
                    changePwd.setString(4, username);
                    changePwd.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("changed password");
                    }

                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, username);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "CHANGE_PWD");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, null);
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }

                    connection.commit();
                    response.put("msgType", "changePwdAck");
                    response.put("uid", uid);
                    return response;
                } else {
                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, username);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "CHANGE_PWD");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "INVALID PASSWORD");
                    createLog.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                try {
                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, username);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "CHANGE_PWD");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "DB ERROR");
                    createLog.executeUpdate();
                    System.err.println("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
                return null;
            } finally {
                if (changePwd != null) {
                    changePwd.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Saves the userlog as csv file into /tmp/userlogs.csv
     * Logs the change of password in the userlog.
     * Creates failure file log invalid password or db error (rollsback accordingly). */
    //TODO: how to verify admin?
    boolean getUserLog() {
        if (DEBUG_MODE) {
            System.out.println("Can view file logs");
        }
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        PreparedStatement getFileLog = null;
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }

            String selectLog = "SELECT 'userLogid', 'uid', 'simulatedUsername', 'lastModified', 'actionType', " +
                    "'status', 'sourceIp', 'failureType' UNION ALL " +
                    "SELECT * FROM UserLog INTO OUTFILE \"/tmp/userlogs.csv\" FIELDS TERMINATED BY ','\n" +
                    "ENCLOSED BY '\"'\n" +
                    "LINES TERMINATED BY '\\n'; ";

            try {
                getFileLog = connection.prepareStatement(selectLog);
                getFileLog.executeQuery();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (getFileLog != null) {
                    getFileLog.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Changes the email associated with a certain user.
     * @param uid The user id of the user
     * @param oldEmail The alleged old email address of the user
     * @param newEmail The email address to change to
     * @return true if the change of email is successful, false otherwise.
     */
    boolean changeEmail(int uid, String oldEmail, String newEmail, String sourceIp) {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement changeEmail = null;
            PreparedStatement createLog = null;

            String updateEmail = "UPDATE Users SET email = ? WHERE uid = ? AND email = ?";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, simulatedUsername, lastModified, actionType, status, sourceIp, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                createLog = connection.prepareStatement(insertLog);
                changeEmail = connection.prepareStatement(updateEmail);
                connection.setAutoCommit(false);
                changeEmail.setString(1, newEmail);
                changeEmail.setInt(2, uid);
                changeEmail.setString(3, oldEmail);
                changeEmail.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("changed email");
                }

                createLog.setInt(1, 0);
                createLog.setInt(2, uid);
                createLog.setString(3, null);
                createLog.setTimestamp(4, lastModified);
                createLog.setString(5, "CHANGE_EMAIL");
                createLog.setString(6, "SUCCESS");
                createLog.setString(7, sourceIp);
                createLog.setString(8, null);
                createLog.executeUpdate();
                if (DEBUG_MODE) {
                    System.out.println("created log");
                }

                connection.commit();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                try {
                    System.err.println("Transaction is being rolled back");
                    connection.rollback();
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setString(3, null);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "CHANGE_EMAIL");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setString(8, "DB ERROR");
                    createLog.executeUpdate();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
                return false;
            } finally {
                if (changeEmail != null) {
                    changeEmail.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks that the username and password associated with this instance of
     * SQL_Accounts can be used to connect to the database
     * @return true if the username and password combination can be used to
     * connect to the database, false otherwise
     */
    boolean checkCredentials() {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (connection != null) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (DEBUG_MODE) {
                System.out.println("Unable to connect to the db");
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the user name that is associated with this user id
     * @param userId The userid of the user
     * @return The username of the user
     */
    String getUsername(int userId) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement getUsername = null;
            String selectUsername = "SELECT U.username FROM Users U WHERE U.uid = ?";

            try {
                getUsername = connection.prepareStatement(selectUsername);
                connection.setAutoCommit(false);
                getUsername.setInt(1, userId);
                ResultSet rs = getUsername.executeQuery();

                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (getUsername != null) {
                    getUsername.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the user id that is associated with this user name
     * @param username The userid of the user
     * @return The userid of the user
     */
    int getUserId(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement getUid = null;
            String selectUid = "SELECT U.uid FROM Users U WHERE U.username = ?";

            try {
                getUid = connection.prepareStatement(selectUid);
                connection.setAutoCommit(false);
                getUid.setString(1, username);
                ResultSet rs = getUid.executeQuery();

                if (rs.next()) {
                    int uid = rs.getInt(1);
                    return uid;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            } finally {
                if (getUid != null) {
                    getUid.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    int userEmailExists(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/PSFS5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement verifyUniqueness = null;

            String checkUsername = "SELECT U.uid FROM Users U WHERE U.username = ? AND U.email != \"\"";

            try {
                verifyUniqueness = connection.prepareStatement(checkUsername);
                verifyUniqueness.setString(1, username);
                ResultSet rs = verifyUniqueness.executeQuery();
                return (rs.next()) ? rs.getInt(0) : -1;
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            } finally {
                if (verifyUniqueness != null) {
                    verifyUniqueness.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

