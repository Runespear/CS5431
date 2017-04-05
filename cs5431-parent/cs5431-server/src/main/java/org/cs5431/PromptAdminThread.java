package org.cs5431;

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
        //TODO
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
                //TODO
            }
        }
    }
}
