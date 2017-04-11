package org.cs5431;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;

import static org.cs5431.Constants.DEBUG_MODE;
import static org.cs5431.Encryption.secondPwdHash;

public class SQL_Connection {

    private int port;
    private String ip;
    private String DB_USER;
    private String DB_PASSWORD;

    public SQL_Connection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public SQL_Connection(String ip, int dbPort, String username, String
            password) {
        this.ip = ip;
        this.port = dbPort;
        this.DB_USER = username;
        this.DB_PASSWORD = password;
    }

    /** Determines if the username already exists in the database.
     * @param username is the username to be checked in the database.
     * @return true if the username does not exist; false otherwise. */
    public boolean isUniqueUsername(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement verifyUniqueness;

            String checkUsername = "SELECT U.uid FROM Users U WHERE U.username = ?";
            verifyUniqueness = connection.prepareStatement(checkUsername);

            try {
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
     * @param user json containing the details of the user to be added
     * @param hashedPwd hashed password of the user that is to be created
     * @param pwdSalt salt of the password that was used
     * @return json containing registrationAck and details of the user added (refer to protocols doc)
     * */
    public JSONObject createUser(JSONObject user, String hashedPwd, String pwdSalt) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
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
            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, parentFolderid, fsoName, size, " +
                    "lastModified, isFile)"
                    + " values (?, ?, ?, ?, ?, ?)";
            String insertLog = "INSERT INTO UserLog (userLogid, uid, lastModified, actionType)"
                    + "values (?, ?, ?, ?)";
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
                createFolder.setInt (2, 0); //TODO: what to set as parent folder id?
                createFolder.setString (3, username);
                createFolder.setString   (4, Integer.toString(0));
                createFolder.setTimestamp (5, currDate);
                createFolder.setBoolean    (6, false);
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
    /** Adds fso to the db with sk = enc(secret key of fso). Adds owner as editor.
     * Verifies that the user has permission.
     * @return fsoid of created fso; if no permission, return -1. **/
    public int createFso (JSONObject fso) throws IOException {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        int fsoid;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            int uid = fso.getInt("uid");
            int parentFolderid = fso.getInt("parentFolderid");
            String fsoName = fso.getString("fsoName");
            String size = fso.getString("size");
            Timestamp lastModified = Timestamp.valueOf(fso.getString("lastModified"));
            boolean isFile = fso.getBoolean("isFile");
            String sk = fso.getString("encSK");
            String fileIV = null;
            if (isFile) {
                fileIV = fso.getString("fileIV");
            }
            String fsoNameIV = fso.getString("fsoNameIV");

            boolean hasPermission = verifyEditPermission(parentFolderid, uid);

            if (hasPermission) {
                PreparedStatement createFso = null;
                PreparedStatement addKey = null;
                PreparedStatement createLog = null;
                PreparedStatement addPermission = null;
                PreparedStatement addFile = null;
                PreparedStatement addPath = null;

                String insertFolder = "INSERT INTO FileSystemObjects (fsoid, parentFolderid, fsoName, size, " +
                        "lastModified, isFile, fsoNameIV)"
                        + " values (?, ?, ?, ?, ?, ?, ?)";
                String insertKey = "INSERT INTO FsoEncryption (fsoid, uid, encKey, fileIV) values (?, ?, ?, ?)";
                String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?, ?)";
                String insertEditor = "INSERT INTO Editors (fsoid, uid) values (?, ?)";
                String insertFilePath = "INSERT INTO FileContents (fsoid, path, fileIV) values (?, ?, ?)";

                try {
                    connection.setAutoCommit(false);
                    createFso = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                    createLog = connection.prepareStatement(insertLog);
                    addPermission = connection.prepareStatement(insertEditor);
                    addKey = connection.prepareStatement(insertKey);

                    createFso.setInt(1, 0);
                    createFso.setInt(2, parentFolderid);
                    createFso.setString(3, fsoName);
                    createFso.setString(4, size);
                    createFso.setTimestamp(5, lastModified);
                    createFso.setBoolean(6, isFile);
                    createFso.setString(7, fsoNameIV);
                    createFso.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created folder");
                    }

                    ResultSet rs = createFso.getGeneratedKeys();
                    rs.next();
                    fsoid = rs.getInt(1);

                    addKey.setInt(1, fsoid);
                    addKey.setInt(2, uid);
                    addKey.setString(3, sk);
                    addKey.setString(4, fileIV);
                    addKey.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("added added sk");
                    }

                    String actionType;
                    if (isFile) {
                        actionType = "UPLOAD_FILE";
                    } else {
                        actionType = "CREATE_FOLDER";
                    }

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, actionType);
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }

                    addPermission.setInt(1, fsoid);
                    addPermission.setInt(2, uid);
                    addPermission.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("added owner as editor");
                    }

                    if (isFile) {
                        String file = fso.getString("file");
                        FileOutputStream fos = new FileOutputStream("./files/" + uid + "/" + fsoid);

                        fos.write(Base64.getDecoder().decode(file));
                        fos.close();

                        addFile = connection.prepareStatement(insertFilePath);
                        addFile.setInt(1, fsoid);
                        addFile.setString(2, "./files/" + uid + "/" + fsoid);
                        addFile.setString(3, fileIV);
                        addFile.executeUpdate();
                        if (DEBUG_MODE) {
                            System.out.println("added file path");
                        }
                    }

                    connection.commit();

                    return fsoid;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (createFso != null) {
                        createFso.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    if (addPermission != null) {
                        addPermission.close();
                    }
                    if (addKey != null) {
                        addKey.close();
                    }
                    if (addFile != null) {
                        addFile.close();
                    }
                    if (addPath != null) {
                        addPath.close();
                    }
                    connection.setAutoCommit(true);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** Compares username and encrypted password with row of User table.
     * @return h(privKey) of the user if the authentication is valid. **/
    public JSONObject authenticate(JSONObject allegedUser, String encPwd) {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        JSONObject user = new JSONObject();

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement verifyUser = null;

            String checkPassword = "SELECT U.uid, U.parentFolderid, U.email, U.privKey, U.pubKey, U.privKeySalt" +
                    " FROM Users U WHERE U.username = ? AND U.pwd = ?";

            String username = allegedUser.getString("username");

            try {
                verifyUser = connection.prepareStatement(checkPassword);
                verifyUser.setString(1, username);
                verifyUser.setString(2, encPwd);
                ResultSet rs = verifyUser.executeQuery();

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
                    return user;
                } else {
                    //TODO: log the number of failed authentications? send email directly?
                    String insertLog = "INSERT INTO UserLog (userLogid, uid, lastModified, actionType)"
                            + "values (?, ?, ?, ?)";
                    String selectUsername = "SELECT U.uid FROM Users U WHERE U.username = ?";
                    PreparedStatement getUid = connection.prepareStatement(selectUsername);
                    PreparedStatement logFailed = connection.prepareStatement(insertLog);

                    getUid.setString(1, username);
                    rs = getUid.executeQuery();

                    if (rs.next()) {
                        int uid = rs.getInt(1);
                        logFailed.setInt(1, 0);
                        logFailed.setInt(2, uid);
                        logFailed.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                        logFailed.setString(4, "FAILED_LOGIN");
                        logFailed.execute();
                    }
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (verifyUser != null) {
                    verifyUser.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DEBUG_MODE) {
            System.out.println("Invalid user");
        }
        return null;
    }

    /*
    public String getPrivKeySalt(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false?autoReconnect=true&useSSL=false";

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
    }*/

    /** Gets pwdSalt of pwd associated with username.
     * @return salt of password associated with username */
    public String getSalt(String username) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement getSalt = null;

            String selectSalt = "SELECT U.pwdSalt FROM Users U WHERE U.username = ?";
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

    //how does an admin delete user?
    /** Deletes the user with uid. To be first authenticated using username and password.
     * Creates a log entry of the deletion of user.
     * @return the uid of the user that is deleted; -1 if unsuccessful deletion. */
    public int deleteUser(int uid, String username, String password) {
        JSONObject allegedUser = new JSONObject();
        allegedUser.put("username", "username");
        //allegedUser.put("pwd", password);
        String salt = getSalt(username);
        String encPwd = secondPwdHash(password, Base64.getDecoder().decode(salt));
        JSONObject user = authenticate(allegedUser, encPwd);

        if (user != null) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            if (DEBUG_MODE) {
                System.out.println("Connecting to database...");
            }
            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }
                PreparedStatement removeUser = null;
                PreparedStatement createLog = null;

                String deleteUser = "DELETE FROM Users WHERE username = ? AND pwd = ?";
                String insertLog = "INSERT INTO UserLog (userLogid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?)";

                try {
                    createLog = connection.prepareStatement(insertLog);
                    removeUser = connection.prepareStatement(deleteUser);
                    connection.setAutoCommit(false);
                    removeUser.setString(1, username);
                    removeUser.setString(2, password);
                    removeUser.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("deleted user");
                    }
                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, uid);
                    createLog.setTimestamp(3, lastModified);
                    createLog.setString(4, "DELETE");
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }

                    connection.commit();
                    return uid;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (removeUser != null) {
                        removeUser.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /** Changes the password of the user and the privKey which is encrypted by the password of the user.
     * Authenticates the user with his/her old password before making any changes.
     * Logs the change of password in the userlog.
     * @param newEncPwd the new encrypted version of the password.
     * @param allegedUser json with the credentials of the user to be modified along
     *                    a new privKey.
     * @return json with changePwdAck and the uid; if user is not authenticated, return null. */
    public JSONObject changePassword(JSONObject allegedUser, String newEncPwd) {
        int uid = allegedUser.getInt("uid");
        String username = allegedUser.getString("username");
        String password = allegedUser.getString("hashedPwd");
        String newPrivKey = allegedUser.getString("newPrivKey");
        String salt = getSalt(username);
        String encPwd = secondPwdHash(password, Base64.getDecoder().decode(salt));
        JSONObject user = authenticate(allegedUser, encPwd);
        if (user != null) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false?autoReconnect=true&useSSL=false";
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
                String insertLog = "INSERT INTO UserLog (userLogid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?)";

                try {
                    createLog = connection.prepareStatement(insertLog);
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
                    createLog.setTimestamp(3, lastModified);
                    createLog.setString(4, "CHANGE_PWD");
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }

                    connection.commit();
                    response.put("msgType", "changePwdAck");
                    response.put("uid", uid);
                    return response;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
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
        }
        return null;
    }

    /** Gets the id, enc(name), size, last modified and isFile that has parentFolderid as a parent.
     * Verifies that the user has permission.
     * @param json with uid and fsoid details.
     * @return An JsonArray of all children. */
    public JSONArray getChildren(JSONObject json) {

        int uid = json.getInt("uid");
        int parentFolderid = json.getInt("fsoid");

        boolean hasPermission = verifyEditPermission(parentFolderid, uid);
        if (hasPermission) {
            JSONArray files = new JSONArray();
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            if (DEBUG_MODE) {
                System.out.println("Connecting to database...");
            }

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }
                PreparedStatement getFiles = null;
                PreparedStatement getKey = null;
                PreparedStatement getIv = null;

                String selectFiles = "SELECT F.fsoid, F.fsoName, F.size, F.lastModified, F.isFile, F.fsoNameIV " +
                        "FROM FileSystemObjects F " +
                        "WHERE F.parentFolderid = ? AND EXISTS" +
                        "(SELECT * FROM Editors E WHERE E.uid=?) OR EXISTS" +
                        "(SELECT * FROM Viewers V WHERE V.uid=?);";
                String selectKey = "SELECT F.encKey FROM FsoEncryption F WHERE F.fsoid = ? AND F.uid = ?";
                String selectIv = "SELECT F.fileIV FROM FileContents WHERE F.fsoid = ? AND F.uid = ?";

                try {
                    getFiles = connection.prepareStatement(selectFiles);
                    getKey = connection.prepareStatement(selectKey);
                    getIv = connection.prepareStatement(selectIv);
                    connection.setAutoCommit(false);
                    getFiles.setInt(1, parentFolderid);
                    getFiles.setInt(2, uid);
                    getFiles.setInt(3, uid);
                    ResultSet rs = getFiles.executeQuery();

                    while (rs.next()) {
                        JSONObject fso = new JSONObject();
                        int fsoid = rs.getInt(1);
                        fso.put("msgType", "getChildrenAck");
                        fso.put("id", fsoid);
                        fso.put("name", rs.getString(2));
                        fso.put("size", rs.getString(3));
                        fso.put("lastModified", rs.getTimestamp(4));
                        fso.put("fsoNameIV", rs.getString(6));

                        if (rs.getBoolean(5)) {
                            fso.put("FSOType", "FILE");
                        } else {
                            fso.put("FSOType", "FOLDER");
                        }
                        getKey.setInt(1, fsoid);
                        getKey.setInt(2, uid);
                        ResultSet encRS = getKey.executeQuery();

                        if (encRS.next()) {
                            fso.put("encKey", encRS.getString(1));
                        }
                        getIv.setInt(1, fsoid);
                        getIv.setInt(2, uid);
                        ResultSet fileIV = getKey.executeQuery();

                        if (fileIV.next()) {
                            fso.put("fileIV", fileIV.getString(1));
                        }

                        files.put(fso);
                    }
                    return files;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return null;
                } finally {
                    if (getFiles != null) {
                        getFiles.close();
                    }
                    if (getKey != null) {
                        getKey.close();
                    }
                    if (getIv != null) {
                        getIv.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE) {
            System.out.println("User does not have permission to edit");
        }
        return null;
    }

    /** Verifies that the user has permission before getting the enc file contents
     * and IV used to encrypt the file.
     * @param json with details on uid and fsoid.
     * @return json with downloadAck and path of t*/
    public JSONObject getFile(JSONObject json) throws Exception {
        int uid = json.getInt("uid");
        int fsoid = json.getInt("fsoid");

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            if (DEBUG_MODE) {
                System.out.println("Connecting to database...");
            }
            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }
                PreparedStatement getPath = null;
                PreparedStatement getKey = null;

                String selectPath = "SELECT F.path, F.fileIV FROM FileContents F WHERE F.fsoid = ?";
                String selectSk = "SELECT F.encKey FROM FsoEncryption F WHERE F.fsoid = ? AND F.uid = ?";

                try {
                    getPath = connection.prepareStatement(selectPath);
                    getKey = connection.prepareStatement(selectSk);
                    connection.setAutoCommit(false);
                    getPath.setInt(1, fsoid);
                    ResultSet rs = getPath.executeQuery();
                    JSONObject fso = new JSONObject();

                    getKey.setInt(1, fsoid);
                    getKey.setInt(2, uid);
                    ResultSet sk = getKey.executeQuery();

                    if (sk.next()) {
                        fso.put("encFileSK", sk.getString(1));
                    }

                    if (rs.next()) {
                        fso.put("msgType","downloadAck");
                        if (DEBUG_MODE) {
                            System.out.println("THE PATH IS: " + rs.getString(1));
                        }
                        File reqFile = new File(rs.getString(1));
                        FileInputStream inputStream = new FileInputStream
                                (reqFile);
                        byte[] filebytes = new byte[inputStream.available()];
                        inputStream.read(filebytes);
                        inputStream.close();
                        fso.put("encFile", Base64.getEncoder().encodeToString
                                (filebytes));

                        fso.put("fsoid", fsoid);
                        if (DEBUG_MODE) {
                            System.out.println("fileic" + rs.getString(2));
                        }
                        fso.put("fileIV", rs.getString(2));
                    }
                    return fso;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return null;
                } finally {
                    if (getPath != null) {
                        getPath.close();
                    }
                    if (getKey != null) {
                        getKey.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE) {
            System.out.println("User does not have permission to the file");
        }
        return null;
    }

    /** Gets all viewers and editors of the fso. Fsoid has to refer to an existing fso.
     * @return A JsonObjects with 2 fields: "editors" and "viewers" with a arraylist value;
     * returns null otherwise  **/
    public JSONObject getPermissions(int fsoid) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement verifyEditors = null;
            PreparedStatement verifyViewers = null;

            String selectEditors = "SELECT E.uid FROM Editors E WHERE E.fsoid = ?";
            String selectViewers = "SELECT V.uid FROM Viewers V WHERE V.fsoid = ?";

            ArrayList<Integer> editors = new ArrayList<>();
            ArrayList<Integer> viewers = new ArrayList<>();

            try {
                verifyEditors = connection.prepareStatement(selectEditors);
                verifyViewers = connection.prepareStatement(selectViewers);

                verifyEditors.setInt(1, fsoid);
                ResultSet editorsId = verifyEditors.executeQuery();

                verifyViewers.setInt(1, fsoid);
                ResultSet viewersId = verifyViewers.executeQuery();

                while (editorsId.next()) {
                    int editor = editorsId.getInt(1);
                    editors.add(editor);
                }
                while (editorsId.next()) {
                    int viewer = viewersId.getInt(1);
                    viewers.add(viewer);
                }
                JSONObject permissions = new JSONObject();
                permissions.put("editors", editors);
                permissions.put("viewers", viewers);
                return permissions;

            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (verifyEditors != null) {
                    verifyEditors.close();
                }
                if (verifyViewers != null) {
                    verifyViewers.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean verifyEditPermission(int fsoid, int uid) {
        JSONObject permissions = getPermissions(fsoid);
        if (permissions != null) {
            try {
                JSONArray editors = permissions.getJSONArray("editors");
                for (int i = 0; i < editors.length(); i++) {
                    int editorid = (int) editors.get(i);
                    if (editorid == uid) {
                        return true;
                    }
                }
            } catch (JSONException e1) {
                System.out.println("Unable to parse JSON object.");
                e1.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean verifyBothPermission(int fsoid, int uid) {
        JSONObject permissions = getPermissions(fsoid);
        if (permissions != null) {
            try {
                JSONArray editors = permissions.getJSONArray("editors");
                for (int i = 0; i < editors.length(); i++) {
                    int editorid = (int) editors.get(i);
                    if (editorid == uid) {
                        return true;
                    }
                }
                JSONArray viewers = permissions.getJSONArray("viewers");
                for (int i = 0; i < viewers.length(); i++) {
                    int viewerid = (int) viewers.get(i);
                    if (viewerid == uid) {
                        return true;
                    }
                }
            } catch (JSONException e1) {
                System.out.println("Unable to parse JSON object.");
                e1.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /** Checks the permissions of the uid before getting all file log entries of this fsoid.
     * @Return A JsonArray of filelog entries; returns null otherwise  **/
    public JSONArray getFileLog(JSONObject jsonObject) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        boolean hasPermission = verifyBothPermission(fsoid, uid);
        if (hasPermission) {
            if (DEBUG_MODE) {
                System.out.println("Can view file logs");
            }
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement getFileLog = null;
            if (DEBUG_MODE) {
                System.out.println("Connecting to database...");
            }

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }

                String selectLog = "SELECT L.uid, L.lastModified, L.actionType FROM FileLog L WHERE L.fsoid = ?";
                JSONArray fileLogArray = new JSONArray();

                try {
                    getFileLog = connection.prepareStatement(selectLog);
                    getFileLog.setInt(1, fsoid);
                    ResultSet rs = getFileLog.executeQuery();

                    while (rs.next()) {
                        JSONObject log = new JSONObject();
                        log.put("msgType", "getFileLogAck");
                        log.put("uid", rs.getInt(1));
                        log.put("lastModified", rs.getTimestamp(2));
                        log.put("actionType", rs.getString(3));
                        fileLogArray.put(log);
                    }
                    return fileLogArray;

                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    if (getFileLog != null) {
                        getFileLog.close();
                    }
                }
        } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    //TODO: userlog
    public JSONArray getUserLog(int fsoid, int uid) {
        boolean hasPermission = verifyBothPermission(fsoid, uid);
        if (hasPermission) {
            if (DEBUG_MODE) {
                System.out.println("Can view file logs");
            }
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement getFileLog = null;
            if (DEBUG_MODE) {
                System.out.println("Connecting to database...");
            }

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }

                String selectLog = "SELECT L.uid, L.lastModified, L.actionType FROM FileLog L WHERE L.fsoid = ?";
                getFileLog = connection.prepareStatement(selectLog);
                JSONArray fileLogArray = new JSONArray();

                try {
                    getFileLog.setInt(1, fsoid);
                    ResultSet rs = getFileLog.executeQuery();

                    while (rs.next()) {
                        JSONObject log = new JSONObject();
                        log.put("uid", rs.getInt(1));
                        log.put("lastModified", rs.getTimestamp(2));
                        log.put("actionType", rs.getString(3));
                        fileLogArray.put(log);
                    }
                    return fileLogArray;

                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    if (getFileLog != null) {
                        getFileLog.close();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    //TODO: to send in new IV when renaming
    public int renameFso(int fsoid, int uid, String newName) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            if (DEBUG_MODE) {
                System.out.println("Can rename fso");
            }
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement renameFso = null;
            PreparedStatement createLog = null;

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }
                String updateName = "UPDATE FileSystemObjects SET fsoName = ? WHERE fsoid =  ?";
                String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?, ?)";

                try {
                    renameFso = connection.prepareStatement(updateName);
                    createLog = connection.prepareStatement(insertLog);
                    connection.setAutoCommit(false);
                    renameFso.setString(1, newName);
                    renameFso.setInt(2, fsoid);
                    renameFso.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("renamed fso");
                    }

                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "RENAME");
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }
                    connection.commit();
                    return fsoid;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (renameFso != null) {
                        renameFso.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE) {
            System.out.println("failed to rename");
        }
        return -1;
    }

    /** Gets the public key of newUid (user to be granted permission) and the sk of the file
     * that is enc with the uid's pubKey. First verifies if uid has permission to add priv.
     * @param fsoid the file that is to be added permissions.
     * @param newUid the user to be granted permission,
     * @param uid the user that is performing the addition of priv.*/
    public JSONObject getKeys(int fsoid, int uid, int newUid) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement getSecretKey = null;
            PreparedStatement getPubKey = null;

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }
                String selectPubKey = "SELECT U.pubKey FROM Users U WHERE U" +
                        ".uid = ?";
                String selectSecretKey = "SELECT F.encKey FROM Users FsoEncryption F WHERE F.uid = ? AND F.fsoid = ?";

                JSONObject output = new JSONObject();

                try {
                    getPubKey = connection.prepareStatement(selectPubKey);
                    getSecretKey = connection.prepareStatement(selectSecretKey);
                    connection.setAutoCommit(false);
                    getPubKey.setInt(1, newUid);
                    ResultSet rs = getPubKey.executeQuery();

                    if (rs.next()) {
                        String pubKey = rs.getString(1);
                        output.put("pubKey", pubKey);
                    }

                    getSecretKey.setInt(1, uid);
                    getSecretKey.setInt(2, fsoid);
                    rs = getSecretKey.executeQuery();

                    if (rs.next()) {
                        String secretKey = rs.getString(1);
                        output.put("secretKey", secretKey);
                    }
                    connection.commit();
                    return output;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return null;
                } finally {
                    if (getSecretKey != null) {
                        getSecretKey.close();
                    }
                    if (getPubKey != null) {
                        getPubKey.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /** Adds newUid as editor of the file. Adds sk of the file that is encrypted with newUid's public key.
     * First verifies that the user has permission. Logs the action.
     * @param json with fsoid, newUid, and uid (user performing the add priv function).
     * @return newUid if successful; else -1 if unsuccessful. */
    public int addEditPriv(JSONObject json) {

        int uid = json.getInt("uid");
        int fsoid = json.getInt("fsoid");
        int newUid = json.getInt("newUid");
        String encKey = json.getString("encSecretKey");

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement addEditor = null;
            PreparedStatement createLog = null;
            PreparedStatement shareFsoKey = null;

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE) {
                    System.out.println("Database connected!");
                }
                String insertEditor = "INSERT INTO Editors (fsoid, uid) " +
                        "values (?, ?)";
                String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?, ?)";
                String insertFsoKey = "INSERT INTO FsoEncryption (fsoid, uid, encKey) values (?, ?, ?)";

                try {
                    createLog = connection.prepareStatement(insertLog);
                    addEditor = connection.prepareStatement(insertEditor);
                    shareFsoKey = connection.prepareStatement(insertFsoKey);
                    connection.setAutoCommit(false);
                    addEditor.setInt(1, fsoid);
                    addEditor.setInt(2, newUid);

                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_PRIV");
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created log");
                    }

                    shareFsoKey.setInt(1, fsoid);
                    shareFsoKey.setInt(2, uid);
                    shareFsoKey.setString(3, encKey);
                    shareFsoKey.executeUpdate();

                    connection.commit();
                    return newUid;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (addEditor != null) {
                        addEditor.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    if (shareFsoKey != null) {
                        shareFsoKey.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE) {
            System.out.println("failed to add priv");
        }
        return -1;
    }

    /** Adds newUid as viewer of the file. Adds sk of the file that is encrypted with newUid's public key.
     * First verifies that the user has permission. Logs the action.
     * @param json with fsoid, newUid, and uid (user performing the add priv function).
     * @return newUid if successful; else -1 if unsuccessful. */
    public int addViewPriv(JSONObject json) {

        int uid = json.getInt("uid");
        int fsoid = json.getInt("fsoid");
        int newUid = json.getInt("newUid");
        String encKey = json.getString("encSecretKey");

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement addViewer = null;
            PreparedStatement createLog = null;
            PreparedStatement shareFsoKey = null;

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE)
System.out.println("Database connected!");
                String insertViewer = "INSERT INTO Viewers (fsoid, uid) values (?, ?)";
                String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?, ?)";
                String insertFsoKey = "INSERT INTO FsoEncryption (fsoid, uid, encKey) values (?, ?, ?)";

                try {
                    createLog = connection.prepareStatement(insertLog);
                    addViewer = connection.prepareStatement(insertViewer);
                    shareFsoKey = connection.prepareStatement(insertFsoKey);
                    connection.setAutoCommit(false);
                    addViewer.setInt(1, fsoid);
                    addViewer.setInt(2, newUid);

                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_PRIV");
                    createLog.executeUpdate();
                    if (DEBUG_MODE)
System.out.println("created log");

                    shareFsoKey.setInt(1, fsoid);
                    shareFsoKey.setInt(2, uid);
                    shareFsoKey.setString(3, encKey);
                    shareFsoKey.executeUpdate();

                    connection.commit();
                    return newUid;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (addViewer != null) {
                        addViewer.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    if (shareFsoKey != null) {
                        shareFsoKey.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE)
System.out.println("failed to add priv");
        return -1;
    }

    //TODO: update
    public int removeViewPriv(int fsoid, int uid, int rmUid) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement rmViewer = null;
            PreparedStatement createLog = null;
            PreparedStatement removeKey = null;

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE)
System.out.println("Database connected!");
                String deleteViewer = "DELETE FROM Viewers WHERE fsoid = ? AND uid = ?";
                String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?, ?)";
                String deleteKey = "DELETE FROM FsoEncryption WHERE fsoid = ? AND uid = ?";


                try {
                    createLog = connection.prepareStatement(insertLog);
                    rmViewer = connection.prepareStatement(deleteViewer);
                    removeKey = connection.prepareStatement(deleteKey);
                    connection.setAutoCommit(false);
                    rmViewer.setInt(1, fsoid);
                    rmViewer.setInt(2, rmUid);
                    rmViewer.executeUpdate();

                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_PRIV");
                    createLog.executeUpdate();
                    if (DEBUG_MODE)
System.out.println("created log");

                    removeKey.setInt(1, fsoid);
                    removeKey.setInt(2, uid);
                    removeKey.executeUpdate();
                    if (DEBUG_MODE)
System.out.println("removed key");

                    connection.commit();
                    return rmUid;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (rmViewer != null) {
                        rmViewer.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    if (removeKey != null) {
                        removeKey.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE)
System.out.println("failed to remove viewer");
        return -1;
    }

    //TODO: update
    public int removeEditPriv(int fsoid, int uid, int rmUid) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        if (hasPermission) {
            if (DEBUG_MODE)
System.out.println("Can rename fso");
            String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
            PreparedStatement rmEditor = null;
            PreparedStatement createLog = null;
            PreparedStatement removeKey = null;

            try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
                if (DEBUG_MODE)
System.out.println("Database connected!");
                String deleteEditor = "DELETE FROM Editors WHERE fsoid = ? AND uid = ?";
                String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType)"
                        + "values (?, ?, ?, ?, ?)";
                String deleteKey = "DELETE FROM FsoEncryption WHERE fsoid = ? AND uid = ?";


                try {
                    createLog = connection.prepareStatement(insertLog);
                    rmEditor = connection.prepareStatement(deleteEditor);
                    removeKey = connection.prepareStatement(deleteKey);
                    connection.setAutoCommit(false);
                    rmEditor.setInt(1, fsoid);
                    rmEditor.setInt(2, rmUid);
                    rmEditor.executeUpdate();

                    Timestamp lastModified = new Timestamp(System.currentTimeMillis());
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_PRIV");
                    createLog.executeUpdate();
                    if (DEBUG_MODE)
System.out.println("created log");

                    removeKey.setInt(1, fsoid);
                    removeKey.setInt(2, uid);
                    removeKey.executeUpdate();
                    if (DEBUG_MODE)
System.out.println("removed key");

                    connection.commit();
                    return rmUid;

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (connection != null) {
                        try {
                            System.err.println("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            excep.printStackTrace();
                        }
                    }
                    return -1;
                } finally {
                    if (rmEditor != null) {
                        rmEditor.close();
                    }
                    if (createLog != null) {
                        createLog.close();
                    }
                    if (removeKey != null) {
                        removeKey.close();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG_MODE)
System.out.println("failed to remove editor");
        return -1;
    }
}

