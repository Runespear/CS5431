package org.cs5431.controller;



import java.io.*;

/**
 * Created by Brandon on 14/4/2017.
 */
public class YesToCertThread implements Runnable {

    public Runtime runtime;

    public YesToCertThread(Runtime runtime){
        this.runtime = runtime;
    }
    public void run(){
        try {
            Thread.sleep(5000);

            System.out.println("yes");
            String data = "yes\n";

            ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());
            System.setIn(bais);

        }
        catch(Exception e){
        }
    }

}