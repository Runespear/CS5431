package org.cs5431;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.security.SecureRandom;

public class TwoFactorAuth {

    // Obtain Account Sid and Token at twilio.com/user/account,
    // TODO: REMOVE THIS WHEN DONE username: psfs5431@gmail.com, password: psfs5431psfs5431
    // TODO: This information should be keyed in by the admin
    public static final String ACCOUNT_SID = "AC3933f4f181f6a36ef0f1484cd9316e48";
    public static final String AUTH_TOKEN = "8c29f7034b30c0decae9da4f147bb215";

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
     * @param ACCOUNT_SID The sms service's account SID
     * @param AUTH_TOKEN  The sms service's auth token
     * @param User_Phone  The user's phone number
     * @return The generated OTP
     * CURRENTLY ONLY WORKS WHEN SENDING TEXT MESSAGES TO MY PHONE BECAUSE I NEED TO REGISTER
     * OTHER PHONES
     */
    public static String generateAndSend3fa(String ACCOUNT_SID, String AUTH_TOKEN, String User_Phone){

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        String otp = getRandomPassword(4);
        Message message = Message
                .creator(new PhoneNumber("+" + User_Phone),  // to
                        new PhoneNumber("+16072755133"),  // from
                        otp)  // message
                .create();
        return otp;
    }

    /**
     * Generates and sends a one time pin for 2FA
     * @param userEmail The email to send the OTP to
     * @return The generated OTP
     */
    public String generateAndSend2fa(String userEmail) {

        //TODO: Hardcode admin mail for now?
        //Generate a random password
        String otp = getRandomPassword(20);

        adminEmail.send(userEmail, "2FA for PSFS Account", "The following is the second factor " +
                "authentication into your Pretty Secure File Sharing Account:" + otp);
        return otp;
    }

    /**
     * Checks whether a given OTP is valid
     * @param otp OTP returned from the generateAndSend2fa method
     * @param receivedOtp OTP entered by user
     * @param otpGenTime Time when the OTP was generated, use system.nanoTime() to get current time in ns
     * @return True if the OTP is valid, false otherwise
     */
    public static boolean checkOtpValid(String otp, String receivedOtp, long otpGenTime) {
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
        if (time_difference/1000000000 > 60){
            //TODO: Comment out the print line if you wish
            System.out.println("OTP expired!");
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
    /*public static void main(String[] args) throws Exception{

        long start_time = System.nanoTime();
        String otp = generateAndSend2fa("brandon18031993@hotmail.com");

        //String otp = generateAndSend3fa(ACCOUNT_SID,AUTH_TOKEN,"12797806");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String user_otp;
        System.out.println("Enter your OTP: ");
        user_otp = br.readLine();

        boolean otp_valid = checkOtpValid(user_otp,otp,start_time);
        System.out.println("Is OTP valid? " + otp_valid);
    }*/
}
