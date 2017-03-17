package org.cs5431_server.setup;

import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Scanner;

/**
 * Sets up the server. The admin of a server should run this first and only
 * once.
 */
public class ServerSetup {

    public static void main(String[] args) {
        //TODO validation for IP address and port
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the name of the server:");
        String name = scanner.nextLine();
        System.out.println("Enter the IP address of the server:");
        String ip = scanner.nextLine();
        System.out.println("Enter the port address of the server:");
        String port = scanner.nextLine();

        KeyGenerationParameters parameters = new KeyGenerationParameters(new
                SecureRandom(), 2048); //TODO change key size?
        RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
        kpg.init(parameters);
        AsymmetricCipherKeyPair keyPair = kpg.generateKeyPair();

        //TODO write name, ip, port, public key to name.config file in
        // /user-config
        //keyPair.getPublic()

        //TODO write private key in /server-config
        //keyPair.getPrivate()
        File serverDir = new File("./server-config/");
        if (!serverDir.exists()){
            if (!serverDir.mkdir())
                System.err.println("Could not make server config folder");
        }
        File privKeyFile = new File("./server-config/"+name+".priv");
        //TODO store private key in this file

        //TODO create databases

        System.out.println("Distribute the "+name+".config file found in the " +
                "/user-config folder to your users.");
    }
}
