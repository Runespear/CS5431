package org.cs5431.view;

import com.sun.xml.internal.bind.api.impl.NameConverter;
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
import org.cs5431.TransmittedFile;
import org.cs5431.Validator;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ResourceBundle;

import static org.cs5431.controller.SSLController.*;


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
            if (serverName == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Please select a server to connect to.");
                alert.showAndWait();
                return;
            }
            File configFile = new File("./user-config/" + serverName +
                    ".config");

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(configFile), StandardCharsets.UTF_8.name()));
            String server = br.readLine();
            String outPort = br.readLine();
            String sslPort = br.readLine();
            br.close();
            if (!Validator.validIP(server)|| !Validator.validPort(outPort) ||
                    !Validator.validPort(sslPort)){
                throw new IOException("Config file tampered with");
            }

            File pubKeyFile = new File("./user-config/" + serverName + ".pub");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream
                    (pubKeyFile));
            PublicKey serverPubKey = (PublicKey) ois.readObject();
            ois.close();

            File cert = new File("./user-config/"+serverName+".cer");
            File jks = new File("./user-config/"+serverName+".jks");
            if (!cert.exists()) {
                Socket s = new Socket(server, Integer.parseInt(outPort));

                String filepath = "./user-config/";
                TransmittedFile received = receive_Cert(s, filepath);
                Boolean is_Cert_Modified = verify_Cert(serverPubKey,received);
                TransmittedFile received2 = receive_JKS(s,filepath);
                Boolean is_JKS_Modified = verify_JKS(serverPubKey, received2);
                if (!cert.exists()) {
                    throw new CertException("Could not create new " +
                            "certificate.");
                }
                if (!jks.exists()) {
                    throw new JKSException("Could not create new " +
                            "truststore.");
                }
                if (!is_Cert_Modified)
                    throw new CertException("Certificate transmitted " +
                            "from server cannot be trusted!");
                if (!is_JKS_Modified)
                    throw new JKSException("Trust store transmitted from " +
                            "server cannot be trusted!");
            }

            goToLogin(e, server, serverName, sslPort);
        } catch (IOException | ClassNotFoundException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("User config error");
            alert.setContentText("User config file could not be read. Please " +
                    "check with your server admin.");
            alert.showAndWait();
            ex.printStackTrace();
        } catch (CertException | JKSException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Server cannot be trusted!");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
            ex.printStackTrace();
        } catch (Exception ex) {
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
    public static class CertException extends Exception {
        CertException(String message) {
            super(message);
        }
    }
    public static class JKSException extends Exception {
        JKSException(String message) {
            super(message);
        }
    }

}