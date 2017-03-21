package org.cs5431_client.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import org.cs5431_client.util.Validator;

import java.io.*;
import java.net.URL;
import java.security.PublicKey;
import java.util.ResourceBundle;

public class ConnectController implements Initializable {
    @FXML
    public ChoiceBox<String> serverDropdown;

    @FXML
    public Button connectButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        File userConfigDir = new File("./user-config");
        if (!userConfigDir.exists()){
            alertConfigFilesNotFound();
        } else {
            File[] configFiles = userConfigDir.listFiles((dir, filename) ->
                    filename.endsWith(".config"));

            if (configFiles == null) {
                alertConfigFilesNotFound();
            } else {
                ObservableList<String> observableList =
                        FXCollections.observableArrayList();
                for (File f : configFiles) {
                    String fileName = f.getName();
                    int pos = fileName.lastIndexOf(".");
                    fileName = fileName.substring(0, pos);
                    observableList.add(fileName);
                }
                serverDropdown.setItems(observableList);
            }
        }

        connectButton.setOnAction(this::connect);
    }

    private void connect(Event e) {
        try {
            String serverName = serverDropdown.getValue();
            File configFile = new File("./user-config/" + serverName +
                    ".config");
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            String nameInFile = br.readLine(); //TODO: validate this?
            String server = br.readLine();
            String port = br.readLine();
            if (!serverName.equals(nameInFile) || !Validator.validIP(server)
                || !Validator.validPort(port)){
                throw new IOException("Config file tampered with");
            }

            File pubKeyFile = new File("./user-config/" + serverDropdown.getValue
                    () + ".pub");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream
                    (pubKeyFile));
            PublicKey serverPubKey = (PublicKey) ois.readObject();
            //TODO actually connect to the server with these details
            //TODO create secure channel here

            goToLogin(e, server, port);
        } catch (IOException | ClassNotFoundException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("User config error");
            alert.setContentText("User config file could not be read. Please " +
                    "check with your server admin.");
            alert.showAndWait();
            ex.printStackTrace();
        }
    }

    /**
     * Tries to open the registration page to allow the user to create an
     * account with a server.
     */
    private void goToLogin(Event e, String server, String port) {
        try {
            Node node = (Node) e.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = stage.getScene();

            final URL r = getClass().getResource("login.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(r);
            Parent root = fxmlLoader.load();
            LoginController lc = fxmlLoader.getController();
            Client.loginNode = root;
            lc.setStage(stage);
            lc.setConnectionDetails(server,port);
            scene.setRoot(root);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void alertConfigFilesNotFound() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("User config folder does not exist");
        alert.setContentText("Please place your config files that your " +
                "server admin has given you into the folder /user-config/");
        alert.showAndWait();
    }
}
