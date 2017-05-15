package org.cs5431;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Scanner;

public class PromptAdminThread implements Runnable {
    private SQL_Accounts sql_accounts;
    private SQL_Files sql_files;
    private String sourceIP;

    PromptAdminThread(SQL_Accounts sql_accounts, SQL_Files sql_files) {
        this.sql_accounts = sql_accounts;
        this.sql_files = sql_files;
        try {
            this.sourceIP = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            this.sourceIP = "127.0.0.1";
        }
    }

    public void run() {
        try {
            while (true) {
                prompt();
            }
        } catch (SQLException e) {
            System.out.println("Wrong password, please try again.");
            e.printStackTrace();
        }
    }

    private void prompt() throws SQLException {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        while(true) {
            System.out.println("Enter 'u' to download user logs");
            System.out.println("Enter 'f' to download file logs");
            System.out.println("Enter 'd' to delete a user");
            String command = scanner.nextLine();
            String[] elements = command.trim().split("\\s+");

            switch (elements[0]) {
                case "u":
                    downloadUserLogs(sql_accounts);
                    return;
                case "d":
                    deleteUser(sql_accounts);
                    return;
                case "f":
                    downloadFileLogs(sql_files);
                    return;
                default:
                    System.out.println("Sorry, your command was not " +
                            "understood.");
            }
        }
    }

    private void downloadUserLogs(SQL_Accounts sql_accounts) {
        /*
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file name of the log that will be " +
                "saved");
        String logFileName = scanner.nextLine();
        while(!Validator.validFileName(logFileName)) {
            System.out.println("Please enter a valid file name");
            logFileName = scanner.nextLine();
        }
        */
        String location = sql_accounts.getUserLog();
        if (location != null)
            System.out.println("User logs downloaded into " + location);
        else {
            System.out.println("Failed to download user logs. Please try again.");
        }

    }

    private void downloadFileLogs(SQL_Files sql_files) {
        System.out.println("Type 'a' to download all file logs");
        System.out.println("Type 's' to download the file log of a specific " +
                "file");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        while (true) {
            String input = scanner.nextLine();
            switch(input) {
                case "a":
                    String location = sql_files.getAllFileLogs();
                    if (location != null) {
                        System.out.println("Log has been downloaded into " + location);
                    } else {
                        System.out.println("Failed to download file logs. Please try again.");
                    }
                    return;
                case "s":
                    location = downloadOneFileLog(sql_files);
                    if (location != null) {
                        System.out.println("Log has been downloaded into " + location);
                    } else {
                        System.out.println("Failed to download file logs. Please try again.");
                    }
                    return;
                default:
                    System.out.println("Sorry, that command was not " +
                            "understood.");
                    break;
            }
        }
    }

    private String downloadOneFileLog(SQL_Files sql_files) {
        System.out.println("Type the file id of the file log to download");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        while (true) {
            try {
                Integer input = Integer.parseInt(scanner.nextLine());
                if (input <= 0)
                    System.out.println("File ids are always positive integers" +
                            ". Please try again.");
                else {
                    return sql_files.adminGetFileLog(input);
                }
            } catch (NumberFormatException e) {
                System.out.println("Sorry, that was not a valid number. " +
                        "Please try again.");
            }
        }
    }

    private void deleteUser(SQL_Accounts sql_accounts) throws SQLException {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        while(true) {
            System.out.println("Enter the username of the user to delete:");
            String userToDelete = scanner.nextLine();

            System.out.println("Please confirm the username of the user to " +
                    "delete by entering it again:");
            String confirm = scanner.nextLine();
            if (userToDelete.equals(confirm)) {
                int uid = sql_accounts.getUserId(userToDelete);
                if (uid == -1)
                    System.out.println("There is no user associated with this" +
                            " username.");
                else {
                    if (sql_accounts.adminDeleteUser(uid, sourceIP) == uid) {
                        System.out.println("User successfully deleted.");
                        return;
                    } else {
                        System.out.println("User found but could not be " +
                                "deleted.");
                    }
                }
            }
        }
    }
}
