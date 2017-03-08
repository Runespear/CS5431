# CS5431

##How to run this system##

###Server (for sending to client_TCP)###
Run org.cs5431_server.Main as the main class with program argument 8080 (which indicates the port #). Currently our code is hardcoded to use port 8080 but this will change in the future

###client_TCP (for testing receiving from Server)###
Run Server first. (Otherwise there is nothing to communicate with)
Run org.cs5431_client.util.client_tcp as the main class.

###Client (GUI)###
Run org.cs5431_client.view.Client as the main class.
If testing the download button, run Server first.