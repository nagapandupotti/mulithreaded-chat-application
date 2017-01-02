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

Execute the Server by typing
$ java Server
You will see the message "The server is running on port 8000"

Create multiple folders namely client1, client2, client3, ..., clientN.
Copy the Client.class, Client$ListenerHandler.class into all of those folders.
Establish connection for each client to the server that is running at port 8000 by executing this command in the respective Terminals.

$ java Client <clientId> 8000

The clientId refers to the user defined clientName.
PortNumber 8000 refers to the port on which the server is running. 

For example., if we want to start one server, and connect 4 clients (client1, client2, client3, client4) to the server. 
1. Open the terminal and go to project directory, 
2. Start server, $ java Server
3. Go to the client1's directory ($ cd client1) and run, $ java Client client1 8000
4. Go to the client2's directory ($ cd client2) and run, $ java Client client2 8000
5. Go to the client3's directory ($ cd client3) and run, $ java Client client3 8000
6. Go to the client4's directory ($ cd client4) and run, $ java Client client4 8000

Now that all the 4 clients are connected to the server, 
(1) Broadcast Message: Go to any of the client's terminal, and type: 
broadcast message "<Message you want to send to all other clients>"
E.g., broadcast message "Hi all" 

Note: Message should be specified in double quotes only!!
Observe the broadcasted message in each of the clients respective terminals.  


(2) Broadcast File:
The file to be broadcasted (or unicasted or blockcasted) MUST be present in the client's directory!
Go to any of the client's terminal, and type: 
broadcast file "<Only the file name you want to send to all other clients>"
E.g., broadcast file "testfile.txt" 

Note: File name should be specified in double quotes only. Filename specified should be in the client (current) directory only. 
Observe that the file received will be in each of the clients respective directories. 


(3) Unicast Message: 
Go to any of the client's terminal, and type:
unicast message "<Message you want to send to a single client>" <Receiver's clientId>
E.g., unicast message "Hello client2" client2

Note: Message should be specified in double quotes only!!
Observe the unicasted (private) message in the receiver client's terminal. 


(4) Unicast File:
The file to be unicasted MUST be present in the client's directory!
unicast file "<Only the file name you want to send to the other client>" <Receiver's clientId>
E.g., unicast file "testfile.txt" client2

Note: File name should be specified in double quotes only. Filename specified should be in the client (current) directory only. 
Observe that the file received will be in the client's (receiver client) directory. 


(5) Blockcast Message: 
Go to any of the client's terminal, and type: 
blockcast message "<Message you want to send to all other clients except for one client>" <Blocked ClientId>
E.g., blockcast message "Hi all except client2" client2

Note: Message should be specified in double quotes only!!
Observe the blockcasted message will be seen in each of the clients respective terminals except in the terminal of the blocked client.  

(6) Blockcast File:
The file to be blockcasted MUST be present in the client's directory!
Go to any of the client's terminal, and type: 
blockcast file "<Only the file name you want to send to all other clients>" <Blocked ClientId>
E.g., blockcast file "testfile.txt" client2

Note: File name should be specified in double quotes only. Filename specified should be in the client (current) directory only. 
Observe that the file received will be seen in each of the clients respective directories except in the directory of the blocked client.  
Few cases handled: 
When a client tries to establish a connection with the server with the duplicate clientId, the server does not allow that to happen, and the error is logged at the server. 
If a client tries to unicast without specifying the receiver's clientId, error is thrown at the client's side. 
If a client tries to send the file that is not present in its directory, file not found error is thrown at the client's side. 


