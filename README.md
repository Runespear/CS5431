# CS5431

*Assume that you have maven and mysql database running, make sure all dependencies in pom.xml are imported
1. run server setup
2. follow the instructions to create db and certificate
3. run serverview to run as admin
4. run client
5. client to set up truststore password
6. client to accept cert and store it
7. client to disconnect
8. rerun client
9. connect client to the server that has been set up (and should still be running at this point)


##How to run this system##

We are currently working on IntelliJ, and have not determined a proper build
as a whole yet. Open up the following files below and right click to run
to preview the:

* 1) File-Transfer capabilities between TCP_Client and TCP_Server
    In this case, it is client uploading to server
* 2) GUI which is almost done, but not yet hooked up to a working server backend.


###Server (for sending to client_TCP)###
Not working as of yet.

###client_TCP (for testing receiving from Server)###
Not working as of yet. 

Run Server first. (Otherwise there is nothing to communicate with)
Run org.cs5431_client.util.client_tcp as the main class.

###Client (GUI)###
Run org.cs5431_client.view.Client as the main class.
If testing the download button, run Server first.

Currently Server client above not working as intended.

###TCP_Server (Working Test Version so use this first)
Run TCP_Server. This server waits for a client to upload a file.
It will automatically generate a folder called "receive" in the 
current working directory and download the file from client.

Port is currently hardcoded as 8080 for you.

Note that receive is gitignored, but the code will generate the folder
for you automatically

###TCP_Client (Working Test version so use this first)
Run TCP_Client. This client connects to TCP_Server, and automatically
uploads a file. It tells the server how big the file size is as well.

The directory that it sends from is called "send" in your working directory.

"cats.txt" is a test text file that is automatically generated for you 
and it contains 4 lines, where the 4th line is a time stamp (USE time).

Port is currently hardcoded as 8080 for you. 

Under main:

```Java
public static void main(String[] args) {
        String[] fileNames = new String[2];
        fileNames = new String[] {"cats.txt", "Opt2.pdf"};
        TCP_Client fc = new TCP_Client("localhost", 8080, fileNames);

}
```

The 1st filename in the array will be transferred (multiple file transfer
functionality not yet done) They will already be in "send".


You should expect to see the correct file in "receive" (On the server side,
or in the same place if running on localhost)