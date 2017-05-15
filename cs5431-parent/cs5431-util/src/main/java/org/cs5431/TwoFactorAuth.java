package org.cs5431;

import java.security.SecureRandom;

import static org.cs5431.Constants.DEBUG_MODE;

public class TwoFactorAuth {

    private Email adminEmail;

    TwoFactorAuth(Email adminEmail) {
        this.adminEmail = adminEmail;
    }

    //Using securerandom to generate a random string of a specified length
    public static String getRandomPassword(int Length) {
        String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890,./;'[]-=~!@#$%^&*()_+";
        StringBuilder rs = new StringBuilder();
        SecureRandom random = new SecureRandom();
        while (rs.length() < Length) {
            int index = (int) (random.nextFloat() * CHARS.length());
            rs.append(CHARS.charAt(index));
        }
        String randStr = rs.toString();
        return randStr;
    }

    /**
     * Generates and sends a one time pin for 3FA with phone
     * @param User_Phone  The user's phone number
     * @return The generated OTP
     * CURRENTLY ONLY WORKS WHEN SENDING TEXT MESSAGES TO MY PHONE BECAUSE I NEED TO REGISTER
     * OTHER PHONES
     */
    public String generateAndSend3fa(String User_Phone){

        //Generate a random password
        String otp = getRandomPassword(5);

        adminEmail.send(User_Phone + "@txt.att.net", "OTP for PSFS Account", "Your OTP is: " +
                otp);
        return otp;
    }

    /**
     * Generates and sends a one time pin for 2FA
     * @param userEmail The email to send the OTP to
     * @return The generated OTP
     */
    public String generateAndSend2fa(String userEmail) {

        //Generate a random password
        String otp = getRandomPassword(10);

        adminEmail.send(userEmail, "2FA for PSFS Account", "The following is the second factor " +
                "authentication into your Pretty Secure File Sharing Account: " + otp);
        return otp;
    }

    /**
     * Checks whether a given OTP is valid
     * @param otp OTP returned from the generateAndSend2fa method
     * @param receivedOtp OTP entered by user
     * @param otpGenTime Time when the OTP was generated, use system.nanoTime() to get current time in ns
     * @return True if the OTP is valid, false otherwise
     */
    public boolean checkOtpValid(String otp, String receivedOtp, long otpGenTime) {
        long current_time = System.nanoTime();
        long time_difference;
        //checking for overflow, highly unlikely as it will take 292 yrs to do so
        if (current_time < otpGenTime) {
            time_difference = (current_time - Long.MIN_VALUE) + (Long.MAX_VALUE - otpGenTime);
        }
        else{
            time_difference = current_time - otpGenTime;
        }

        //if otp has existed for more than 2 minutes, we return false
        if (time_difference/1000000000 > 300){
            if (DEBUG_MODE) {
                System.out.println("OTP expired!");
            }
            return false;
        }
        else{
            //check if otp correct
            if (otp.equals(receivedOtp)){
                return true;
            }
            else{
                //TODO: Comment out the print line if you wish
                System.out.println("OTP incorrect!");
                return false;
            }
        }

    }

    //example on using the functions
    /*
    public static void main(String[] args) throws Exception{

        Email email = new Email("psfs5431@gmail.com", "theroadtoA+");
        TwoFactorAuth factor = new TwoFactorAuth(email);

        long start_time = System.nanoTime();
        //String otp = generateAndSend2fa("brandon18031993@hotmail.com");

        String otp = factor.generateAndSend3fa("6073798244");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String user_otp;
        System.out.println("Enter your OTP: ");
        user_otp = br.readLine();

        boolean otp_valid = checkOtpValid(user_otp,otp,start_time);
        System.out.println("Is OTP valid? " + otp_valid);
    }*/
}
