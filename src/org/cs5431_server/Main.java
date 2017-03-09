package org.cs5431_server;

import org.cs5431_server.fileserver.Server;

import java.io.*;

public class Main {


    public static void main(String[] args) throws IOException {
	    //TODO: cmd line stuff? call Client?
        if (args.length != 1){
            throw new RuntimeException("Give the port number as argument");
        }
        System.out.println(args[0]);
        new Server(Integer.parseInt(args[0])); //launches server with a port for connecting
    }
}
