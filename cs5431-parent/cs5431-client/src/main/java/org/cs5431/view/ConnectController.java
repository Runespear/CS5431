package org.cs5431.view;

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
import org.cs5431.SSL_Client_Methods;
import org.cs5431.Validator;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.security.PublicKey;
import java.util.ResourceBundle;

import static org.cs5431.Unsecured_Client.verify_and_receive_Cert;

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
            String server = br.readLine();
            String outPort = br.readLine();
            String sslPort = br.readLine();
            if (!Validator.validIP(server)|| !Validator.validPort(outPort) ||
                    !Validator.validPort(sslPort)){
                throw new IOException("Config file tampered with");
            }

            File pubKeyFile = new File("./user-config/" + serverName + ".pub");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream
                    (pubKeyFile));
            PublicKey serverPubKey = (PublicKey) ois.readObject();
            //TODO actually connect to the server with these details
            //TODO create secure channel here

            File cert = new File("./user-config/"+serverName+".cer");
            if (!cert.exists()) {
                Socket s = new Socket(server, Integer.parseInt(outPort));

                String filepath = "./user-config/";
                verify_and_receive_Cert(serverPubKey, s, filepath);
                if (!cert.exists()) {
                    throw new CertException("Could not create new " +
                            "certificate.");
                }
                SSL_Client_Methods.importCert(serverName);
            }

            goToLogin(e, server, serverName, sslPort);
        } catch (IOException | ClassNotFoundException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("User config error");
            alert.setContentText("User config file could not be read. Please " +
                    "check with your server admin.");
            alert.showAndWait();
            ex.printStackTrace();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Tries to open the registration page to allow the user to create an
     * account with a server.
     */
    private void goToLogin(Event e, String server, String serverName, String
            sslPort) {
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
            lc.setConnectionDetails(server,serverName,sslPort);
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
    public class CertException extends Exception {
        CertException(String message) {
            super(message);
        }
    }

}