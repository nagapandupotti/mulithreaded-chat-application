# mulithreaded-chat-application
Java based Internet chat application with Multithreading. It has a server program and a client program, where the server manages a chat group, allowing any number of clients to join the group with a user name at any time. This application has features such as broadcast, unicast and blockcast messages and files. 

To get the project working, 
Server Components
-----------------
1. Server.java
2. Server.class
3. Server$ListenHandler.class
4. Server$SendHandler.class
5. CustomStream.class

Client Components
------------------ 
1. Client.java 
2. Client.class
3. Client$ListenerHandler.class), and Open Terminal from the same directory. 

$ java Server

Create multiple folders namely client1, client2, client3, ..., clientN.
Copy the Client.class, Client$ListenerHandler.class into all of those folders.
Establish connection for each client to the server that is running at port 8000 by executing this command in the respective Terminals.

$ java Client <clientId> 8000

The clientId refers to the user defined clientName. PortNumber 8000 refers to the port on which the server is running. 

For example., if we want to start one server, and connect 4 clients (client1, client2, client3, client4) to the server. 

$ java Client client1 8000

$ java Client client2 8000

$ java Client client3 8000

$ java Client client4 8000

The following are example commands for the features available, 

$ broadcast message "Hi all" 

$ broadcast file "testfile.txt" 

$ unicast message "Hello client2" client2

$ unicast file "testfile.txt" client2

$ blockcast message "Hi all except client2" client2

$ blockcast file "testfile.txt" client2
