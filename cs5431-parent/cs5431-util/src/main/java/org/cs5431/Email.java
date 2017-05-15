package org.cs5431;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Brandon on 17/4/2017.
 */

//TODO: IF THERE IS A PROBLEM WITH SENDING MAIL AND ENCOUNTER AN AUTHENTICATIONFAILEDEXCEPTION,
//TODO: GO TO https://myaccount.google.com/lesssecureapps AND MAKE IT LESS SECURE

public class Email {

    private String adminEmail;
    private String adminPwd;

    public Email(String adminEmail, String adminPwd) {
        this.adminEmail = adminEmail;
        this.adminPwd = adminPwd;
    }
    //Adapted from https://www.javatpoint.com/example-of-sending-email-using-java-mail-api-through-gmail-server
    void send(String to,String subject,String msg){
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
                        return new PasswordAuthentication(adminEmail,adminPwd);}
        });

        //compose message
        try {
            MimeMessage message = new MimeMessage(session);
            message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
            message.setSubject(subject);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            switch(subject) {
                case "Failed Login Attempt": msg = "An attempt to log into your Pretty Secure File Sharing account failed" +
                        " on " + sdf.format(new Date()) + ". Do contact us at psfs5431@gmail.com if you require further assistance.";
            }

            message.setText(msg);
            //send message
            Transport.send(message);
            System.out.println("message sent successfully");
        } catch (MessagingException e) {throw new RuntimeException(e);}
    }

    //public static void main(String[] args){
        //Email.send("psfs5431@gmail.com","theroadtoA+","zl245@cornell.edu","GG","YOUR ACCOUNT HAS BEEN HACKED BY ZILONG!! HE CAN BE FOUND IN THURSTON RIGHT NOW");
   // }
}
