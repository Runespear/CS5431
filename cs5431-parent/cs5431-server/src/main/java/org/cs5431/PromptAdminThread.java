package org.cs5431;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class PromptAdminThread implements Runnable {
    SQL_Connection sqlConnection;

    public PromptAdminThread(SQL_Connection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public void run() {
        try {
            while (true) {
                prompt(sqlConnection);
            }
        } catch (SQLException e) {
            System.out.println("Wrong password, please try again.");
            e.printStackTrace();
        }
    }

    private void prompt(SQL_Connection sql_connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter 'u' to download user logs");
            System.out.println("Enter 'd' to delete a user");
            String command = scanner.nextLine();
            String[] elements = command.trim().split("\\s+");

            switch (elements[0]) {
                case "u":
                    downloadUserLogs(sql_connection);
                    return;
                case "d":
                    deleteUser(sql_connection);
                    return;
                default:
                    System.out.println("Sorry, your command was not " +
                            "understood.");
            }
        }
    }

    private static void downloadUserLogs(SQL_Connection sql_connection) {
        Scanner scanner = new Scanner(System.in);
        String logFileName = "/";
        while(Validator.validFileName(logFileName)) {
            System.out.println("Enter the file name of the log that will be " +
                    "saved");
            logFileName = scanner.nextLine();
        }
        JSONArray array = sql_connection.getUserLog(-1,-1);
        try {
            File logFile = new File(logFileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            for (int i = 0; i < array.length(); ++i) {
                JSONObject logEntry = array.getJSONObject(i);
                //TODO write to file
            }
        } catch (IOException e) {
            System.out.println("Error when writing logs to file");
            e.printStackTrace();
        }
    }

    private static void deleteUser(SQL_Connection sql_connection) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter the username of the user to delete:");
            String userToDelete = scanner.nextLine();

            System.out.println("Please confirm the username of the user to " +
                    "delete by entering it again:");
            String confirm = scanner.nextLine();
            if (userToDelete.equals(confirm)) {
                int uid = sql_connection.getUserId(userToDelete);
                if (uid == -1)
                    System.out.println("There is no user associated with this" +
                            " username.");
                else {
                    if (sql_connection.adminDeleteUser(uid) == uid) {
                        System.out.println("User successfully deleted.");
                    } else {
                        System.out.println("User found but could not be " +
                                "deleted.");
                    }
                }
            }
        }
    }
}
