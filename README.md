# CS5431

Maven

### Assume that you have maven and mysql database running, make sure all dependencies in pom.xml are imported

(Move the pom.xml outside of the src folder)

## How to run this system on localhost for now, please use an IDE like IntelliJ or something for now to run this ###
(From Terminal)

0. Make sure MySQL is setup and running
  ```sh
  sudo apt-get install mysql
  ```
  *Remember what your root password is!
  Also, make sure to import all the dependencies based off the pom.xml (We are using maven)
  A quick and dirty way to do this is to cut away a line from the pom.xml and paste it back. 
  The IDE will automatically download the dependencies for yous

# Server Side
1. run ServerSetup (under src/org/cs5431_server/setup/)
  * i.e. invoke in the correct directory using the IDE run button
  ```sh
  java WORKINGDIRECTORY/src/org/cs5431_server/setup/ServerSetup
  ```

  * There ought to be a main function under ServerSetup.java 
  * The whole thing is a main function actually 
  
2. Follow the prompts to create your database and certificate
  * For IP address type "127.0.0.1" instead of "localhost"
  * When prompted for your MySQL username and password, just use your root account's for now
  * The default port for MySQL is 3306
  * Use any other port number above 1024 for the other 2 ports

3. Run ServerView to run as admin
4. Run Client
5. client to set up truststore password
6. client to accept cert and store it
7. client to disconnect
8. rerun client
9. connect client to the server that has been set up (and should still be running at this point)

We plan to host the server on a web service in the future, but as of now, we are testing the role of server and client
on multiple laptops as of now.

