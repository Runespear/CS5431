package org.cs5431.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client extends Application {

    static Parent connectNode;
    static Parent loginNode;
    static Parent fileViewNode;
    static Parent registrationNode;
    static Parent editDetailsNode;

    public static final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t ;
    });

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            final URL r = getClass().getResource("connect.fxml");

            if (r == null) {
                System.err.println("Could not find the layout!");
                tryStopping();
            } else {
                connectNode = FXMLLoader.load(r);
                final Scene scene = new Scene(connectNode);
                stage.setScene(scene);
                stage.setTitle("CS 5431: Pretty Secure File Sharing");
                stage.show();
            }
        } catch (IOException e) {
            printNonFatalError("Failed to load layout");
            e.printStackTrace();
            tryStopping();
        }
    }

    private void tryStopping() {
        try {
            stop();
        } catch (Exception e) {
            printNonFatalError("Couldn't stop...");
        }
    }

    /**
     * Prints a non fatal error in a manner of our choice.
     * Currently prints to System.err
     * @param message The message to be printed
     */
    static void printNonFatalError(String message) {
        System.err.println(message);
    }
}
