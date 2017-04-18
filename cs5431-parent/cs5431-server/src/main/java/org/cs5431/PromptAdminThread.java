package org.cs5431;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Scanner;

public class PromptAdminThread implements Runnable {
    SQL_Accounts sql_accounts;
    private String sourceIP;

    public PromptAdminThread(SQL_Accounts sql_accounts) {
        this.sql_accounts = sql_accounts;
        try {
            this.sourceIP = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            this.sourceIP = "127.0.0.1";
        }
    }

    public void run() {
        try {
            while (true) {
                prompt(sql_accounts);
            }
        } catch (SQLException e) {
            System.out.println("Wrong password, please try again.");
            e.printStackTrace();
        }
    }

    private void prompt(SQL_Accounts sql_accounts) throws SQLException {
        Scanner scanner = new Scanner(System.in);
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
                    downloadFileLogs(sql_accounts);
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
        sql_accounts.getUserLog();
    }

    private void downloadFileLogs(SQL_Accounts sql_accounts) {
        //TODO
    }

    private void deleteUser(SQL_Accounts sql_accounts) throws SQLException {
        Scanner scanner = new Scanner(System.in);
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
