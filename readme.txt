Refer to README.md 

INSTRUCTIONS TO RUN OUR PROJECT ON
UBUNTU 16.04

WORST CASE SCENARIO:

    REFER TO THE PDF FOR A MORE PICTORIAL GUIDE


+++++++++++++++++++++++++PREREQUISITES+++++++++++++++++++++++++++++++++++++++++++

1) Install maven:

    sudo apt-get install maven

2) Install Java:

    sudo apt-get install openjdk-8-jdk

3) Install MySQL and Workbench //Make sure you remember your root password!

    sudo dpkg -i mysql-apt-config_0.5.3-1_all.deb
    sudo apt-get update
    sudo apt-get install mysql-workbench-community

4) Run MySQL Workbench

    /usr/bin/mysql-workbench
   If this somehow doesn't work, just go to "search your computer" and look for 
   workbench i.e. run it via gnome/unity gui 

++++++++++++++++++++++++END OF PREREQUISITES++++++++++++++++++++++++++++++++++++


5) Navigate to topmost directory of the source code folder and compile

    mvn clean install


==========================SERVER SETUP==========================================
6) Setup the server by running serversetup:

    java -jar ./cs5431-parent/cs5431-server-setup/target/cs5431-server-setup-0.3-MILESTONE-BETA.jar -cp ./cs5431-parent/cs5431-server-setup/target/lib



7)
    Follow the prompts to create the database and the certificate. Take note of the following when filling in the prompts (An example of what you should see is shown below):
        Simply type in the name of the server you want users to see
        For IP address type "127.0.0.1" instead of "localhost"
        Use the default port 3306 for the MySQL database
        Use any other port number above 1024 for the other 2 ports (outward-facing non-SSL port and outward-facing SSL port)
        When prompted for your MySQL username, use “root” as the username 
        When prompted for your MySQL password, use your root password that you have selected in step number 6 under the “Setting up MySQL” section in this readme
        Enter a username and password for your server. *Remember to note down the username and password!!
8) 
    The next set of prompts will generate the keystore file. Take note of the following when filling in the prompts:

        Type in the keystore password you wish to use and re-enter it to confirm the password. *Remember to note down the password!!
        Fill in the rest of the details (e.g. name, organizational unit) as required to generate the certificate. You can just type in anything you wish for now. 
        When prompted with the following:
        Just type “y”.
        Next, when prompted with the following:
        Just press the enter button on the keyboard
        Re-enter the keystore password. The certificate will now be generated. The pictures below will show what you should see.

        ++++++++++++IMPORTANT++++++++++DANGEROUS++++++++++++++++++++++++++++++
        Type “y” to import the certificate into the client’s truststore. Sometimes, the sentence that says “Trust this certificate? [no]:” might be not be visible, but just go ahead and type “y”.
        ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        YES, OR ELSE THERE WILL BE CRITICAL LOSS OF TRUST (Due to how it was
        implemented 3rd party wise for SSL/TLS)

        You will need to delete everything in server-config and user-config
        and drop PSFS5431 schema and delte users

9) 

    Under ./server-config/, you now have .cer, .jks, .priv, .config files and under ./user-config/ we now have a .config and a .pub file (Public Key for the keytransport). As instructed, the server has to distribute this to the clients (done through email or other transmission methods). Since we are currently using localhost, nothing needs to be done. 

10)

    WE ARE NOW DONE WITH SERVER SETUP!!!! YAY


============================RUN SERVERVIEW======================================

11) After server setup, it's time to get the server up and running:

java -jar ./cs5431-parent/cs5431-server/target/cs5431-server-0.3-MILESTONE-BETA.jar -cp ./cs5431-parent/cs5431-server/target/lib

12) Follow the prompts and type in the usernames and passwords correspondingly

13) Keep the server up and running, we re now going to run the client GUI,
    where most of the nice actions happen



============================RUN CLIENTVIEW======================================


14) Run this

java -jar ./cs5431-parent/cs5431-client/target/cs5431-client-0.3-MILESTONE-BETA.jar -cp ./cs5431-parent/cs5431-client/target/lib

15) A nice GUI should appear

16) A bunch of neat functionality should now appear

17) If this is your first time, create a new user first. 

18) Select whether you want 2FA 

======================COOL THINGS TO DO=========================================

Enable password recovery

Nominate 2<=N number of other users 

Choose some number k where  2 <= k <= N 

Use recover password option to participate in Shamir's Secret Sharing!

+++

Enable 2FA
