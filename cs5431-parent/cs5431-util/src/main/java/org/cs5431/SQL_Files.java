package org.cs5431;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;

import static org.cs5431.Constants.DEBUG_MODE;

public class SQL_Files {

    private int port;
    private String ip;
    private String DB_USER;
    private String DB_PASSWORD;

    public SQL_Files(String ip, int dbPort, String username, String
            password) {
        this.ip = ip;
        this.port = dbPort;
        this.DB_USER = username;
        this.DB_PASSWORD = password;
    }

    /** Adds fso to the db with sk = enc(secret key of fso). Adds owner as editor.
     * Verifies that the user has permission.
     * Creates file log of failure if the user doesnt have permission or if there is a db error (rolls back transaction).
     * @return fsoid of created fso; if no permission, return -1. **/
    public int createFso (JSONObject fso, String sourceIp) throws IOException {

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
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

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }

            boolean hasPermission = verifyEditPermission(parentFolderid, uid);
            PreparedStatement createFso = null;
            PreparedStatement addKey = null;
            PreparedStatement createLog = null;
            PreparedStatement addPermission = null;
            PreparedStatement addFile = null;
            PreparedStatement addPath = null;
            PreparedStatement addParent = null;

            String insertFolder = "INSERT INTO FileSystemObjects (fsoid, fsoName, size, " +
                    "lastModified, isFile, fsoNameIV, ownerid)"
                    + " values (?, ?, ?, ?, ?, ?, ?)";
            String insertKey = "INSERT INTO FsoEncryption (fsoid, uid, encKey, fileIV) values (?, ?, ?, ?)";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String insertEditor = "INSERT INTO Editors (fsoid, uid) values (?, ?)";
            String insertFilePath = "INSERT INTO FileContents (fsoid, path, fileIV) values (?, ?, ?)";
            String insertParent = "INSERT INTO FolderChildren (parentid, childid, uid) values (?, ?, ?)";

            try {
                String actionType;
                if (isFile) {
                    actionType = "UPLOAD_FILE";
                } else {
                    actionType = "CREATE_FOLDER";
                }
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    connection.setAutoCommit(false);
                    createFso = connection.prepareStatement(insertFolder, Statement.RETURN_GENERATED_KEYS);
                    addPermission = connection.prepareStatement(insertEditor);
                    addKey = connection.prepareStatement(insertKey);

                    createFso.setInt(1, 0);
                    createFso.setString(2, fsoName);
                    createFso.setString(3, size);
                    createFso.setTimestamp(4, lastModified);
                    createFso.setBoolean(5, isFile);
                    createFso.setString(6, fsoNameIV);
                    createFso.setInt(7, uid);
                    createFso.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created folder");
                    }

                    ResultSet rs = createFso.getGeneratedKeys();
                    rs.next();
                    int fsoid = rs.getInt(1);

                    addParent = connection.prepareStatement(insertParent);
                    addParent.setInt(1, parentFolderid);
                    addParent.setInt(2, fsoid);
                    addParent.setInt(3, uid);
                   addParent.executeUpdate();

                    addKey.setInt(1, fsoid);
                    addKey.setInt(2, uid);
                    addKey.setString(3, sk);
                    addKey.setString(4, fileIV);
                    addKey.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("added added sk");
                    }

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, actionType);
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, null);
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
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, 0);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, actionType);
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        isFile = fso.getBoolean("isFile");
                        String actionType;
                        if (isFile) {
                            actionType = "UPLOAD_FILE";
                        } else {
                            actionType = "CREATE_FOLDER";
                        }
                        createLog.setInt(1, 0);
                        createLog.setInt(2, 0);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, actionType);
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
                        if (DEBUG_MODE) {
                            System.out.println("created failure log");
                        }
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
                if (addParent != null) {
                    addParent.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** Gets the id, enc(name), size, last modified and isFile that has parentFolderid as a parent.
     * Verifies that the user has permission.
     * Transaction rolls back if db error.
     * @param json with uid and fsoid details.
     * @return An JsonArray of all children. */
    public JSONArray getChildren(JSONObject json, String sourceIp) {

        int uid = json.getInt("uid");
        int parentFolderid = json.getInt("fsoid");

        boolean hasPermission = verifyEditPermission(parentFolderid, uid);
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
            PreparedStatement createLog = null;
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());

            String selectFiles = "SELECT F.fsoid, F.fsoName, F.size, F.lastModified, F.isFile, F.fsoNameIV " +
                    "FROM FileSystemObjects F " +
                    "WHERE EXISTS (SELECT C.childid FROM FolderChildren C WHERE C.parentid = ? AND C.childid = F.fsoid);";
            String selectKey = "SELECT F.encKey FROM FsoEncryption F WHERE F.fsoid = ? AND F.uid = ?";
            String selectIv = "SELECT F.fileIV FROM FileContents WHERE F.fsoid = ? AND F.uid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)";

            try {
                if (hasPermission) {
                    getFiles = connection.prepareStatement(selectFiles);
                    getKey = connection.prepareStatement(selectKey);
                    getIv = connection.prepareStatement(selectIv);
                    connection.setAutoCommit(false);
                    getFiles.setInt(1, parentFolderid);
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
                } else {
                    createLog = connection.prepareStatement(insertLog);
                    createLog.setInt(1, 0);
                    createLog.setInt(2, parentFolderid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "GET_CHILDREN");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog = connection.prepareStatement(insertLog);
                        createLog.setInt(1, 0);
                        createLog.setInt(2, parentFolderid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "GET_CHILDREN");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
                if (createLog != null) {
                    createLog.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DEBUG_MODE) {
            System.out.println("User does not have permission to edit");
        }
        return null;
    }

    /** Verifies that the user has permission before getting the enc file contents
     * and IV used to encrypt the file.
     * Transaction rolls back if db error.
     * @param json with details on uid and fsoid.
     * @return json with downloadAck and path of t*/
    public JSONObject getFile(JSONObject json, String sourceIp) throws Exception {
        int uid = json.getInt("uid");
        int fsoid = json.getInt("fsoid");

        boolean hasPermission = verifyEditPermission(fsoid, uid);
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
            PreparedStatement createLog = null;
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());

            String selectPath = "SELECT F.path, F.fileIV FROM FileContents F WHERE F.fsoid = ?";
            String selectSk = "SELECT F.encKey FROM FsoEncryption F WHERE F.fsoid = ? AND F.uid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try {
                if (hasPermission) {
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
                } else {
                    createLog = connection.prepareStatement(insertLog);
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "DOWNLOAD");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog = connection.prepareStatement(insertLog);
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "DOWNLOAD");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
                if (createLog != null) {
                    createLog.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

            JSONArray editors = new JSONArray();
            JSONArray viewers = new JSONArray();

            try {
                verifyEditors = connection.prepareStatement(selectEditors);
                verifyViewers = connection.prepareStatement(selectViewers);

                verifyEditors.setInt(1, fsoid);
                ResultSet editorsId = verifyEditors.executeQuery();

                while (editorsId.next()) {
                    int editor = editorsId.getInt(1);
                    editors.put(editor);
                }

                verifyViewers.setInt(1, fsoid);
                ResultSet viewersId = verifyViewers.executeQuery();

                while (viewersId.next()) {
                    int viewer = viewersId.getInt(1);
                    viewers.put(viewer);
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

    public boolean verifyViewPermission(int fsoid, int uid) {
        JSONObject permissions = getPermissions(fsoid);
        if (permissions != null) {
            try {
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
    public JSONArray getFileLog(JSONObject jsonObject, String sourceIp) {
        int fsoid = jsonObject.getInt("fsoid");
        int uid = jsonObject.getInt("uid");
        boolean hasPermission = verifyBothPermission(fsoid, uid);
        if (DEBUG_MODE) {
            System.out.println("Can view file logs");
        }
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement getFileLog = null;
        PreparedStatement createLog = null;
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }

            String selectLog = "SELECT L.uid, L.lastModified, L.actionType, L.status, L.newUid, L.fsoid FROM " +
                    "FileLog L WHERE L.fsoid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)";

            JSONArray fileLogArray = new JSONArray();

            try {
                if (hasPermission) {
                    getFileLog = connection.prepareStatement(selectLog);
                    getFileLog.setInt(1, fsoid);
                    ResultSet rs = getFileLog.executeQuery();

                    while (rs.next()) {
                        JSONObject log = new JSONObject();
                        log.put("msgType", "getFileLogAck");
                        log.put("uid", rs.getInt(1));
                        log.put("lastModified", rs.getTimestamp(2));
                        log.put("actionType", rs.getString(3));
                        log.put("status", rs.getString(4));
                        log.put("newUid", rs.getInt(5));
                        log.put("fsoid", rs.getInt(6));
                        fileLogArray.put(log);
                    }
                    return fileLogArray;
                } else {
                    createLog = connection.prepareStatement(insertLog);
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "GET_FILELOG");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return null;
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        createLog = connection.prepareStatement(insertLog);
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "GET_FILELOG");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
                    } catch (SQLException excep) {
                        excep.printStackTrace();
                    }
                }
                return null;
            } finally {
                if (getFileLog != null) {
                    getFileLog.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int renameFso(int fsoid, int uid, String newName, String
            newFSONameIV, String sourceIp) {
        boolean hasPermission = verifyEditPermission(fsoid, uid);
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement renameFso = null;
        PreparedStatement createLog = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            String updateName = "UPDATE FileSystemObjects SET fsoName = ?, fsoNameIV = ? WHERE fsoid =  ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, " +
                    "sourceIp, newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try {
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    if (DEBUG_MODE) {
                        System.out.println("Can rename fso");
                    }
                    renameFso = connection.prepareStatement(updateName);
                    connection.setAutoCommit(false);
                    renameFso.setString(1, newName);
                    renameFso.setString(2, newFSONameIV);
                    renameFso.setInt(3, fsoid);
                    renameFso.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("renamed fso");
                    }

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "RENAME");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, null);
                    createLog.executeUpdate();
                    if (DEBUG_MODE) {
                        System.out.println("created success log");
                    }
                    connection.commit();
                    return fsoid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "RENAME");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "RENAME");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
    public JSONObject getKeys(int fsoid, int uid, int newUid, String sourceIp) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement getSecretKey = null;
        PreparedStatement getPubKey = null;
        PreparedStatement createLog = null;
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            String selectPubKey = "SELECT U.pubKey FROM Users U WHERE U" +
                    ".uid = ?";
            String selectSecretKey = "SELECT F.encKey FROM FsoEncryption F WHERE F.uid = ? AND F.fsoid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, " +
                    "sourceIp, newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            JSONObject output = new JSONObject();

            try {
                if (hasPermission) {
                    getPubKey = connection.prepareStatement(selectPubKey);
                    getSecretKey = connection.prepareStatement(selectSecretKey);
                    connection.setAutoCommit(false);
                    getPubKey.setInt(1, newUid);
                    ResultSet rs = getPubKey.executeQuery();

                    if (rs.next()) {
                        String pubKey = rs.getString(1);
                        output.put("pubKey", pubKey);
                        output.put("msgType", "addViewerKeysAck");
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
                } else {
                    createLog = connection.prepareStatement(insertLog);
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "GET_KEYS");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog = connection.prepareStatement(insertLog);
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "GET_KEYS");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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

    /** Adds newUid as editor of the file. Adds sk of the file that is encrypted with newUid's public key.
     * First verifies that the user has permission. Logs the action.
     * Note that a user must first be a viewer before he/she can be an editor based on the controller.
     * @param json with fsoid, newUid, and uid (user performing the add priv function).
     * @return newUid if successful; else -1 if unsuccessful. */
    public int addEditPriv(JSONObject json, String sourceIp) {

        int uid = json.getInt("uid");
        int fsoid = json.getInt("fsoid");
        int newUid = json.getInt("newUid");
        String encKey = json.getString("encSecretKey");

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        boolean editorExists = verifyEditPermission(fsoid, newUid);
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement addEditor = null;
        PreparedStatement createLog = null;
        PreparedStatement shareFsoKey = null;
        PreparedStatement removeViewer = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            String insertEditor = "INSERT INTO Editors (fsoid, uid) values (?, ?)";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, " +
                    "newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String insertFsoKey = "INSERT INTO FsoEncryption (fsoid, uid, encKey) values (?, ?, ?)";
            String deleteViewer = "DELETE FROM Viewers WHERE uid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (editorExists) {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_EDITOR");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, newUid);
                    createLog.setString(9, "EDITOR ALREADY EXISTS");
                    createLog.executeUpdate();
                    return -1;
                }
                if (hasPermission) {
                    addEditor = connection.prepareStatement(insertEditor);
                    shareFsoKey = connection.prepareStatement(insertFsoKey);
                    removeViewer = connection.prepareStatement(deleteViewer);
                    connection.setAutoCommit(false);
                    addEditor.setInt(1, fsoid);
                    addEditor.setInt(2, newUid);
                    addEditor.executeUpdate();

                    removeViewer.setInt(1, newUid);
                    removeViewer.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_EDITOR");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, newUid);
                    createLog.setString(9, null);
                    createLog.executeUpdate();

                    shareFsoKey.setInt(1, fsoid);
                    shareFsoKey.setInt(2, uid);
                    shareFsoKey.setString(3, encKey);
                    shareFsoKey.executeUpdate();

                    connection.commit();
                    return newUid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_EDITOR");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, newUid);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "ADD_EDITOR");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, newUid);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
                if (removeViewer != null) {
                    removeViewer.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DEBUG_MODE) {
            System.out.println("failed to add priv");
        }
        return -1;
    }

    /** Adds newUid as viewer of the file. Adds sk of the file that is encrypted with newUid's public key.
     * First verifies that the user has permission. Logs the action.
     * If newUid was editor previously, he/she is removed from the editors list, logged, and a new sk is not added.
     * @param json with fsoid, newUid, and uid (user performing the add priv function).
     * @return newUid if successful; else -1 if unsuccessful. */
    public int addViewPriv(JSONObject json, String sourceIp) {

        int uid = json.getInt("uid");
        int fsoid = json.getInt("fsoid");
        int newUid = json.getInt("newUid");
        String encKey = json.getString("encSecretKey");

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        boolean wasEditor = verifyEditPermission(fsoid, newUid);
        boolean viewerExists = verifyViewPermission(fsoid, newUid);
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement addViewer = null;
        PreparedStatement createLog = null;
        PreparedStatement shareFsoKey = null;
        PreparedStatement removeEditor = null;
        PreparedStatement logRmEditor = null;
        PreparedStatement addParent = null;
        PreparedStatement getParentFolder = null;

        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE)
                System.out.println("Database connected!");
            String insertViewer = "INSERT INTO Viewers (fsoid, uid) values (?, ?)";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, " +
                    "newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String insertFsoKey = "INSERT INTO FsoEncryption (fsoid, uid, encKey) values (?, ?, ?)";
            String deleteEditor = "DELETE FROM Editors WHERE uid = ?";
            String rmEditorLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, " +
                    "newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String insertParent = "INSERT INTO FolderChildren (parentid, childid, uid) values (?, ?, ?);";
            String selectParent = "SELECT U.parentFolderid FROM Users U WHERE U.uid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (viewerExists) {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_VIEWER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, newUid);
                    createLog.setString(9, "VIEWER ALREADY EXISTS");
                    createLog.executeUpdate();
                    return -1;
                }

                if (hasPermission) {
                    addViewer = connection.prepareStatement(insertViewer);
                    connection.setAutoCommit(false);
                    addViewer.setInt(1, fsoid);
                    addViewer.setInt(2, newUid);
                    addViewer.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_VIEWER");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, newUid);
                    createLog.setString(9, null);
                    createLog.executeUpdate();

                    if (wasEditor) {
                        removeEditor = connection.prepareStatement(deleteEditor);
                        logRmEditor = connection.prepareStatement(rmEditorLog);
                        removeEditor.setInt(1, newUid);
                        removeEditor.executeUpdate();

                        logRmEditor.setInt(1, 0);
                        logRmEditor.setInt(2, fsoid);
                        logRmEditor.setInt(3, uid);
                        logRmEditor.setTimestamp(4, lastModified);
                        logRmEditor.setString(5, "REMOVE_EDITOR");
                        logRmEditor.setString(6, "SUCCESS");
                        logRmEditor.setString(7, sourceIp);
                        logRmEditor.setInt(8, newUid);
                        logRmEditor.setString(9, null);
                        logRmEditor.executeUpdate();
                    } else {
                        shareFsoKey = connection.prepareStatement(insertFsoKey);
                        shareFsoKey.setInt(1, fsoid);
                        shareFsoKey.setInt(2, newUid);
                        shareFsoKey.setString(3, encKey);
                        shareFsoKey.executeUpdate();

                        getParentFolder = connection.prepareStatement(selectParent);
                        getParentFolder.setInt(1, newUid);
                        ResultSet rs = getParentFolder.executeQuery();

                        if (rs.next()) {
                            int parentFolderid = rs.getInt(1);
                            addParent = connection.prepareStatement(insertParent);
                            addParent.setInt(1, parentFolderid);
                            addParent.setInt(2, fsoid);
                            addParent.setInt(3, newUid);
                            addParent.executeUpdate();
                        }
                    }
                    connection.commit();
                    return newUid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "ADD_VIEWER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, newUid);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "ADD_VIEWER");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, newUid);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
                if (logRmEditor != null) {
                    logRmEditor.close();
                }
                if (removeEditor != null) {
                    removeEditor.close();
                }
                if (addParent != null) {
                    addParent.close();
                }
                if (getParentFolder != null) {
                    getParentFolder.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DEBUG_MODE)
            System.out.println("failed to add priv");
        return -1;
    }

    public int removeViewPriv(int fsoid, int uid, int rmUid, String sourceIp) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement rmViewer = null;
        PreparedStatement createLog = null;
        PreparedStatement removeKey = null;
        PreparedStatement rmFso = null;
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE)
                System.out.println("Database connected!");
            String deleteViewer = "DELETE FROM Viewers WHERE fsoid = ? AND uid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, " +
                    "newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String deleteKey = "DELETE FROM FsoEncryption WHERE fsoid = ? AND uid = ?";
            String deleteFso = "DELETE FROM FolderChildren WHERE childid = ? AND uid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    rmViewer = connection.prepareStatement(deleteViewer);
                    removeKey = connection.prepareStatement(deleteKey);
                    connection.setAutoCommit(false);
                    rmViewer.setInt(1, fsoid);
                    rmViewer.setInt(2, rmUid);
                    rmViewer.executeUpdate();

                    rmFso = connection.prepareStatement(deleteFso);
                    rmFso.setInt(1, fsoid);
                    rmFso.setInt(2, rmUid);
                    rmFso.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "REMOVE_VIEWER");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, rmUid);
                    createLog.setString(9, null);
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
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "REMOVE_VIEWER");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, rmUid);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "REMOVE_VIEWER");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, rmUid);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
                if (rmFso != null) {
                    rmFso.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DEBUG_MODE)
            System.out.println("failed to remove viewer");
        return -1;
    }

    public int removeEditPriv(int fsoid, int uid, int rmUid, String sourceIp) {

        boolean hasPermission = verifyEditPermission(fsoid, uid);

        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        PreparedStatement rmEditor = null;
        PreparedStatement createLog = null;
        PreparedStatement removeKey = null;
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE)
                System.out.println("Database connected!");
            String deleteEditor = "DELETE FROM Editors WHERE fsoid = ? AND uid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, " +
                    "newUid, failureType) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String deleteKey = "DELETE FROM FsoEncryption WHERE fsoid = ? AND uid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    if (DEBUG_MODE)
                        System.out.println("Can rename fso");
                    rmEditor = connection.prepareStatement(deleteEditor);
                    removeKey = connection.prepareStatement(deleteKey);
                    connection.setAutoCommit(false);
                    rmEditor.setInt(1, fsoid);
                    rmEditor.setInt(2, rmUid);
                    rmEditor.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "REMOVE_EDITOR");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, rmUid);
                    createLog.setString(9, null);
                    createLog.executeUpdate();

                    removeKey.setInt(1, fsoid);
                    removeKey.setInt(2, uid);
                    removeKey.executeUpdate();
                    if (DEBUG_MODE)
                        System.out.println("removed key");

                    connection.commit();
                    return rmUid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "REMOVE_EDITOR");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, rmUid);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "REMOVE_VIEWER");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, rmUid);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
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
        if (DEBUG_MODE)
            System.out.println("failed to remove editor");
        return -1;
    }

    /**
     * Gets the encrypted file secret key associated with this fso and this
     * user. First checks to see if the user has edit rights.
     * @param fsoid the id of the fso
     * @param uid the id of the requesting user
     * @return The file secret key associated with this fso and user
     */
    public String getFileSK(int fsoid, int uid, String sourceIp) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        PreparedStatement getSK = null;
        PreparedStatement createLog = null;

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            boolean hasPermission = verifyEditPermission(fsoid, uid);
            String selectSK = "SELECT F.encKey FROM FsoEncryption F WHERE F.uid = ? AND F.fsoid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    getSK = connection.prepareStatement(selectSK);
                    getSK.setInt(1, uid);
                    getSK.setInt(2, fsoid);
                    ResultSet rs = getSK.executeQuery();
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    createLog.setString(5, "GET_SK");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                createLog.setInt(1, 0);
                createLog.setInt(2, fsoid);
                createLog.setInt(3, uid);
                createLog.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                createLog.setString(5, "GET_SK");
                createLog.setString(6, "FAILURE");
                createLog.setString(7, sourceIp);
                createLog.setInt(8, 0);
                createLog.setString(9, "DB ERROR");
                createLog.execute();
                return null;
            } finally {
                if (createLog != null) {
                    createLog.close();
                }
                if (getSK != null) {
                    getSK.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Overwrites the file with First checks to see if the user has edit
     * rights.
     * @param fsoid the id of the file to overwrite
     * @param uid the id of the requesting user
     * @param newFileIV The new fileIV to be written into the database
     * @param encFile The encrypted contents of the file to be written into
     *                the database
     * @return the fsoid if successful, -1 otherwise
     */
    public int overwrite(int fsoid, int uid, String newFileIV, String encFile, String sourceIp) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }

            boolean hasPermission = verifyEditPermission(fsoid, uid);
            PreparedStatement overwriteFso = null;
            PreparedStatement createLog = null;
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());

            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String updateFile = "UPDATE FileContents SET fileIV = ? WHERE fsoid = ?";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    overwriteFso = connection.prepareStatement(updateFile);
                    connection.setAutoCommit(false);

                    overwriteFso.setString(1, newFileIV);
                    overwriteFso.setInt(2, fsoid);
                    overwriteFso.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "OVERWRITE");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, null);
                    createLog.executeUpdate();

                    FileOutputStream fos = new FileOutputStream("./files/" + uid + "/" + fsoid, false);

                    fos.write(Base64.getDecoder().decode(encFile));
                    fos.close();
                    if (DEBUG_MODE) {
                        System.out.println("added file contents");
                    }
                    connection.commit();
                    return fsoid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "OVERWRITE");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.executeUpdate();
                    return -1;
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "OVERWRITE");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
                        if (DEBUG_MODE) {
                            System.out.println("created failure log");
                        }
                    } catch (SQLException excep) {
                        excep.printStackTrace();
                    }
                }
                return -1;
            } finally {
                if (overwriteFso != null) {
                    overwriteFso.close();
                }
                if (createLog != null) {
                    createLog.close();
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean isOnlyViewer(int fsoid) {
        JSONObject permissions = getPermissions(fsoid);
        JSONArray editors = permissions.getJSONArray("editors");
        JSONArray viewers = permissions.getJSONArray("viewers");
        if (editors.length() + viewers.length() == 1) {
            return true;
        }
        return false;
    }

    public boolean isFile(int fsoid) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            PreparedStatement isFile = null;
            String checkFile = "SELECT F.isFile FROM FileSystemObjects F WHERE F.fsoid = ?";

            try {
                isFile = connection.prepareStatement(checkFile);
                isFile.setInt(1, fsoid);
                ResultSet rs = isFile.executeQuery();
                if (rs.next()) {
                    if (1 == rs.getInt(1)) {
                        return true;
                    }
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (isFile != null) {
                    isFile.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Deletes the file. First checks to see if the user has edit rights.
     * @param fsoid the id of the file
     * @param uid the id of the requesting user
     * @return The fsoid if successful, -1 otherwise
     */
    public int deleteForUser(int fsoid, int uid, String sourceIp) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        PreparedStatement deleteFile = null;
        PreparedStatement createLog = null;
        Timestamp lastModified = new Timestamp(System.currentTimeMillis());

        boolean hasPermission = verifyBothPermission(fsoid, uid);

        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            //TODO: remove more from folderchildren if folder? and delete fso if orphan, delete actual folder as well?
            String  removeChild= "DELETE FROM FolderChildren WHERE childid = ? AND uid = ?";
            String insertLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                createLog = connection.prepareStatement(insertLog);
                if (hasPermission) {
                    deleteFile = connection.prepareStatement(removeChild);
                    connection.setAutoCommit(false);
                    deleteFile.setInt(1, fsoid);
                    deleteFile.setInt(2, uid);
                    deleteFile.executeUpdate();

                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "REMOVE_FSO");
                    createLog.setString(6, "SUCCESS");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, null);
                    createLog.execute();

                    connection.commit();
                    return fsoid;
                } else {
                    createLog.setInt(1, 0);
                    createLog.setInt(2, fsoid);
                    createLog.setInt(3, uid);
                    createLog.setTimestamp(4, lastModified);
                    createLog.setString(5, "REMOVE_FSO");
                    createLog.setString(6, "FAILURE");
                    createLog.setString(7, sourceIp);
                    createLog.setInt(8, 0);
                    createLog.setString(9, "NO PERMISSION");
                    createLog.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        createLog.setInt(1, 0);
                        createLog.setInt(2, fsoid);
                        createLog.setInt(3, uid);
                        createLog.setTimestamp(4, lastModified);
                        createLog.setString(5, "REMOVE_FSO");
                        createLog.setString(6, "FAILURE");
                        createLog.setString(7, sourceIp);
                        createLog.setInt(8, 0);
                        createLog.setString(9, "DB ERROR");
                        createLog.executeUpdate();
                        if (DEBUG_MODE) {
                            System.out.println("created failure log");
                        }
                    } catch (SQLException excep) {
                        excep.printStackTrace();
                    }
                }
                return -1;
            } finally {
                if (createLog != null) {
                    createLog.close();
                }
                if (deleteFile != null) {
                    deleteFile.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int deleteForAll(int fsoid, int uid, String sourceIp){
        //TODO: Update SSLServer with the call in the comment
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            boolean isFile = isFile(fsoid);

            PreparedStatement deleteObject = null;
            PreparedStatement logDeleteObject = null;
            PreparedStatement getUid = null;
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());

            String selectUid = "SELECT F.ownerid FROM FileSystemObjects F WHERE F.fsoid = ?";
            String removeFso = "DELETE FROM FileSystemObjects WHERE fsoid = ?";
            String deleteLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                logDeleteObject = connection.prepareStatement(deleteLog);
                int ownerid = -1;
                getUid = connection.prepareStatement(selectUid);
                connection.setAutoCommit(false);

                getUid.setInt(1, fsoid);
                ResultSet rs = getUid.executeQuery();

                if (rs.next()) {
                    ownerid = rs.getInt(1);
                }

                if (DEBUG_MODE) {
                    System.out.println("user is the only viewer left!");
                }
                deleteObject = connection.prepareStatement(removeFso);
                deleteObject.setInt(1, fsoid);
                deleteObject.executeUpdate();

                //delete actual file
                if (isFile) {
                    if (DEBUG_MODE) {
                        System.out.println("trying to delete file!");
                    }
                    Path path = Paths.get("./files/" + ownerid + "/" + fsoid);
                    Files.delete(path);
                    if (DEBUG_MODE) {
                        System.out.println("deleted actual file!");
                    }
                    logDeleteObject.setInt(1, 0);
                    logDeleteObject.setInt(2, fsoid);
                    logDeleteObject.setInt(3, uid);
                    logDeleteObject.setTimestamp(4, lastModified);
                    logDeleteObject.setString(5, "DELETE_FSO_OBJECT");
                    logDeleteObject.setString(6, "SUCCESS");
                    logDeleteObject.setString(7, sourceIp);
                    logDeleteObject.setInt(8, 0);
                    logDeleteObject.setString(9, null);
                    logDeleteObject.execute();
                }
                connection.commit();
            } catch (SQLException e) {
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        logDeleteObject.setInt(1, 0);
                        logDeleteObject.setInt(2, fsoid);
                        logDeleteObject.setInt(3, uid);
                        logDeleteObject.setTimestamp(4, lastModified);
                        logDeleteObject.setString(5, "DELETE_FSO_OBJECT");
                        logDeleteObject.setString(6, "FAILURE");
                        logDeleteObject.setString(7, sourceIp);
                        logDeleteObject.setInt(8, 0);
                        logDeleteObject.setString(9, "DB ERROR");
                        logDeleteObject.execute();
                        if (DEBUG_MODE) {
                            System.out.println("created failure log");
                        }
                    } catch (SQLException excep) {
                        excep.printStackTrace();
                    }
                }
            } catch (IOException e){
                if (DEBUG_MODE) {
                    System.out.println("io exception - cannot delete object!");
                }
                //never seems to reach the following
                if (connection != null) {
                    logDeleteObject = connection.prepareStatement(deleteLog);
                    logDeleteObject.setInt(1, 0);
                    logDeleteObject.setInt(2, fsoid);
                    logDeleteObject.setInt(3, uid);
                    logDeleteObject.setTimestamp(4, lastModified);
                    logDeleteObject.setString(5, "DELETE_FSO_OBJECT");
                    logDeleteObject.setString(6, "FAILURE");
                    logDeleteObject.setString(7, sourceIp);
                    logDeleteObject.setInt(8, 0);
                    logDeleteObject.setString(9, "IO ERROR");
                    logDeleteObject.execute();
                }
            } finally {
                if (deleteObject != null) {
                    deleteObject.close();
                }
                if (logDeleteObject != null) {
                    logDeleteObject.close();
                }
                if (getUid != null) {
                    getUid.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void deleteIfOrphanFile(int fsoid, int uid, String sourceIp) {
        String url = "jdbc:mysql://" + ip + ":" + Integer.toString(port) + "/cs5431?autoReconnect=true&useSSL=false";
        if (DEBUG_MODE) {
            System.out.println("Connecting to database...");
        }
        try (Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            if (DEBUG_MODE) {
                System.out.println("Database connected!");
            }
            boolean isOnlyViewer = isOnlyViewer(fsoid);
            boolean isFile = isFile(fsoid);

            PreparedStatement deleteObject = null;
            PreparedStatement logDeleteObject = null;
            PreparedStatement getUid = null;
            Timestamp lastModified = new Timestamp(System.currentTimeMillis());

            String selectUid = "SELECT F.ownerid FROM FileSystemObjects F WHERE F.fsoid = ?";
            String removeFso = "DELETE FROM FileSystemObjects WHERE fsoid = ?";
            String deleteLog = "INSERT INTO FileLog (fileLogid, fsoid, uid, lastModified, actionType, status, sourceIp, newUid, failureType)"
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                logDeleteObject = connection.prepareStatement(deleteLog);
                if (isOnlyViewer) {
                    int ownerid = -1;
                    getUid = connection.prepareStatement(selectUid);
                    connection.setAutoCommit(false);

                    getUid.setInt(1, fsoid);
                    ResultSet rs = getUid.executeQuery();

                    if (rs.next()) {
                        ownerid = rs.getInt(1);
                    }

                    if (DEBUG_MODE) {
                        System.out.println("user is the only viewer left!");
                    }
                    deleteObject = connection.prepareStatement(removeFso);
                    deleteObject.setInt(1, fsoid);
                    deleteObject.executeUpdate();

                    //delete actual file
                    if (isFile) {
                        if (DEBUG_MODE) {
                            System.out.println("trying to delete file!");
                        }
                        Path path = Paths.get("./files/" + ownerid + "/" + fsoid);
                        Files.delete(path);
                        if (DEBUG_MODE) {
                            System.out.println("deleted actual file!");
                        }
                        logDeleteObject.setInt(1, 0);
                        logDeleteObject.setInt(2, fsoid);
                        logDeleteObject.setInt(3, uid);
                        logDeleteObject.setTimestamp(4, lastModified);
                        logDeleteObject.setString(5, "DELETE_FSO_OBJECT");
                        logDeleteObject.setString(6, "SUCCESS");
                        logDeleteObject.setString(7, sourceIp);
                        logDeleteObject.setInt(8, 0);
                        logDeleteObject.setString(9, null);
                        logDeleteObject.execute();
                    }
                    connection.commit();
                }
            } catch (SQLException e) {
                if (connection != null) {
                    try {
                        System.err.println("Transaction is being rolled back");
                        connection.rollback();
                        logDeleteObject.setInt(1, 0);
                        logDeleteObject.setInt(2, fsoid);
                        logDeleteObject.setInt(3, uid);
                        logDeleteObject.setTimestamp(4, lastModified);
                        logDeleteObject.setString(5, "DELETE_FSO_OBJECT");
                        logDeleteObject.setString(6, "FAILURE");
                        logDeleteObject.setString(7, sourceIp);
                        logDeleteObject.setInt(8, 0);
                        logDeleteObject.setString(9, "DB ERROR");
                        logDeleteObject.execute();
                        if (DEBUG_MODE) {
                            System.out.println("created failure log");
                        }
                    } catch (SQLException excep) {
                        excep.printStackTrace();
                    }
                }
            } catch (IOException e){
                if (DEBUG_MODE) {
                    System.out.println("io exception - cannot delete object!");
                }
                //never seems to reach the following
                if (connection != null) {
                    logDeleteObject = connection.prepareStatement(deleteLog);
                    logDeleteObject.setInt(1, 0);
                    logDeleteObject.setInt(2, fsoid);
                    logDeleteObject.setInt(3, uid);
                    logDeleteObject.setTimestamp(4, lastModified);
                    logDeleteObject.setString(5, "DELETE_FSO_OBJECT");
                    logDeleteObject.setString(6, "FAILURE");
                    logDeleteObject.setString(7, sourceIp);
                    logDeleteObject.setInt(8, 0);
                    logDeleteObject.setString(9, "IO ERROR");
                    logDeleteObject.execute();
                }
            } finally {
                if (deleteObject != null) {
                    deleteObject.close();
                }
                if (logDeleteObject != null) {
                    logDeleteObject.close();
                }
                if (getUid != null) {
                    getUid.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

