package org.cs5431;

import org.cs5431.fileserver.Server;

import java.io.*;

public class Main {


    public static void main(String[] args) throws IOException {
	    //TODO: cmd line stuff? call Client?
        if (args.length != 1){
            throw new RuntimeException("Give the port number as argument");
        }
        new Server(Integer.parseInt(args[0])); //launches server with a port for connecting
    }
}
