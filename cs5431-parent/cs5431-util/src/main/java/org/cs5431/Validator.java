package org.cs5431;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class Validator {
    public static boolean validUsername(String username) {
        // Allows Upper and lower case alphabets
        // Allows numerals 0 to 9
        // Allows hyphens -
        // Allows underscores
        return (username!=null &&
                Pattern.matches("[A-Za-z0-9_-]{5,30}$", username));
    }

    public static boolean validIP(String ip) {
        InetAddressValidator IPValidator = new InetAddressValidator();
        return IPValidator.isValid(ip);
    }

    public static boolean validPort(String port) {
        try {
            Integer p = Integer.parseInt(port);
            return (p >= 1024 && p <= 65535);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean validPassword(String password) {
        return (password != null && password.length() >= 16 && password
                .length() < 1000);
    }

    public static boolean validFileName(String fileName) {
        if (fileName == null)
            return false;
        // No dots allowed
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9\\s_-]{1,255}$");
        if (pattern.matcher(fileName).find()) {
            try {
                File file = new File("./" + fileName);
                if (file.createNewFile()) {
                    if (!file.delete())
                        throw new IOException("Created a file that can't be " +
                                "deleted...");
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return false;
    }

    public static boolean validEmail(String email) {
        EmailValidator emailValidator = EmailValidator.getInstance();
        return emailValidator.isValid(email);
    }
}
