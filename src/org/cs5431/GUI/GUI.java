package org.cs5431.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class GUI extends Application {

    static Parent guiNode;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            final URL r = getClass().getResource("login.fxml");

            if (r == null) {
                System.err.println("Could not find the layout!");
                tryStopping();
            } else {
                guiNode = FXMLLoader.load(r);
                final Scene scene = new Scene(guiNode);
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
    private void printNonFatalError(String message) {
        //TODO: reconsider if printing to cerr is correct behaviour
        System.err.println(message);
    }
}
