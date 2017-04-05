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
        BufferedReader r = new BufferedReader(
                new InputStreamReader(sslSocket.getInputStream()));
        String str;
        str = r.readLine();
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
        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(s.getOutputStream()));
        String str = o.toString();
        if (DEBUG_MODE) {
            System.out.println(str);
        }
        w.write(str + '\n');
        w.flush();
    }

    public static JSONArray receiveJsonArray(Socket sslSocket) throws
            IOException, ClassNotFoundException {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(sslSocket.getInputStream()));
        String str;
        str = r.readLine();
        if (DEBUG_MODE) {
            System.out.println(str);
            System.out.flush();
        }

        return new JSONArray(str);
    }
}
