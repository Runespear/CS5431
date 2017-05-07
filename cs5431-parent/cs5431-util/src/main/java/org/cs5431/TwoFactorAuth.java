package org.cs5431;

import java.util.Date;

public class TwoFactorAuth {

    /**
     * Generates and sends a one time pin for 2FA
     * @param userEmail The email to send the OTP to
     * @return The generated OTP
     */
    public static String generateAndSend2fa(String userEmail) {
        //TODO: Brandon
        return null;
    }

    /**
     * Checks whether a given OTP is valid
     * @param otp OTP returned from the generateAndSend2fa method
     * @param receivedOtp OTP entered by user
     * @param otpGenTime Time when the OTP was generated
     * @return True if the OTP is valid, false otherwise
     */
    public static boolean checkOtpValid(String otp, String receivedOtp, Date otpGenTime) {
        //TODO: Brandon
        return false;
    }
}
