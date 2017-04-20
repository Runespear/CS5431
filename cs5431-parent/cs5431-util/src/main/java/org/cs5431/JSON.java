package org.cs5431;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

import static org.cs5431.Constants.DEBUG_MODE;

public class JSON {
    public static void sendJson(JSONObject json, Socket sslSocket) throws
            IOException {
        sendObject(json, sslSocket);
    }

    public static JSONObject receiveJson(Socket sslSocket) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(sslSocket.getInputStream());
        TransmittedJSON received = (TransmittedJSON) objectInputStream.readObject();
        String str = received.jsonString;
        if (DEBUG_MODE) {
            System.out.println(str);
            System.out.flush();
        }
        return new JSONObject(str);
    }

    static void sendJsonArray(JSONArray json, Socket s) throws
            IOException {
        sendObject(json, s);
    }

    private static void sendObject(Object o, Socket s) throws IOException {
        if (DEBUG_MODE) {
            System.out.println("sending json");
        }
        String str = o.toString();
        if (DEBUG_MODE) {
            System.out.println(str);
        }
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(s.getOutputStream());
        TransmittedJSON transmittedJSON = new TransmittedJSON();
        transmittedJSON.jsonString = str;
        objectOutputStream.writeObject(transmittedJSON);
    }

    public static JSONArray receiveJsonArray(Socket sslSocket) throws
            IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(sslSocket.getInputStream());
        TransmittedJSON received = (TransmittedJSON) objectInputStream.readObject();
        String str = received.jsonString;
        if (DEBUG_MODE) {
            System.out.println(str);
            System.out.flush();
        }

        return new JSONArray(str);
    }
}
