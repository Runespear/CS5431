package org.cs5431;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import java.util.Properties;

/**
 * Created by Brandon on 17/4/2017.
 */

//TODO: IF THERE IS A PROBLEM WITH SENDING MAIL AND ENCOUNTER AN AUTHENTICATIONFAILEDEXCEPTION,
//TODO: GO TO https://myaccount.google.com/lesssecureapps AND MAKE IT LESS SECURE

public class Email {
    //Adapted from https://www.javatpoint.com/example-of-sending-email-using-java-mail-api-through-gmail-server
    public static void send(String from,String password,String to,String subject,String msg){
        //Get properties object
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        //get Session
        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from,password);}
        });

        //compose message
        try {
            MimeMessage message = new MimeMessage(session);
            message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
            message.setSubject(subject);
            message.setText(msg);
            //send message
            Transport.send(message);
            System.out.println("message sent successfully");
        } catch (MessagingException e) {throw new RuntimeException(e);}
    }

    public static void main(String[] args){
        Email.send("psfs5431@gmail.com","theroadtoA+","zl245@cornell.edu","GG","YOUR ACCOUNT HAS BEEN HACKED BY ZILONG!! HE CAN BE FOUND IN THURSTON RIGHT NOW");
    }
}
