import java.net.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

public class Client {
	String message;
	String clientId;
	boolean firstMsgFlag = true;
	
	private static Socket requestSocket;

	public Client(String clientId) {
		this.clientId = clientId;
	}

	void run(Socket requestSocket) {
		try {
			//initialize inputStream and outputStream
			OutputStream outRaw = requestSocket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outRaw);
			out.flush();
			DataOutputStream dos = new DataOutputStream(outRaw);
			
			if(firstMsgFlag) {
				sendMessage(out, "firstmsg:" + this.clientId);
				firstMsgFlag = false;
			}

			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			
			while(true) {
				System.out.println("Please enter command as a string:");
				
				//read a sentence from the standard input
				this.message = bufferedReader.readLine();
				
				if(!(isBroadCast() || isBlockCast() || isUniCast())) {
					System.out.println("[ERROR] Illegal command encountered, please try again");
					continue;
				}
				
				if(message.contains("file")) {					
					String pathToFile = matchDoubleQuotes(message);
					
					try {
						
						Path path = Paths.get(pathToFile);
						Boolean fileExists = Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
						
						if(fileExists) {
							
							// communication1: send the entire sentence to the server
							sendMessage(out, message);
							
							byte[] bFile = Files.readAllBytes(path);
							int len = bFile.length;					
							System.out.println("Length of file -> " + len);
		
							// communication2: send length of file to the server
							dos.writeInt(len);
							
							// communication3: send file to the server
							if(len > 0) {
								dos.write(bFile, 0, len);
							}
							
							dos.flush();
							
						} else {
							System.out.println("[ERROR] File does not exist, please try again");
						}
					
					} catch (IOException e) {
						e.printStackTrace();
					}	
					
				} else { // normal message
					
					sendMessage(out, message);
					
				}
				
			}// while true
		}
		catch (ConnectException e) {
    			System.err.println("[ERROR] Connection refused. You need to initiate a server first.");
		} 
		catch(UnknownHostException unknownHost) {
			System.err.println("[ERROR] You are trying to connect to an unknown host!");
		}
		catch(IOException ioException) {
			ioException.printStackTrace();
		}
		finally {
			//Close connections
			//try{
				//in.close();
				//out.close();
				//requestSocket.close();
			//}
			//catch(IOException ioException){
				//ioException.printStackTrace();
			//}
		}
	}
	
	public boolean isBroadCast() {
		return this.message.toLowerCase().contains("broadcast");
	}
	
	public boolean isUniCast() {
		return this.message.toLowerCase().contains("unicast");
	}
	
	public boolean isBlockCast() {
		return this.message.toLowerCase().contains("blockcast");
	}
	
	//send a message to the output stream
	void sendMessage(ObjectOutputStream out, String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
	public String matchDoubleQuotes(String msg) {
		Pattern p = Pattern.compile("\"([^\"]*)\"");
		Matcher m = p.matcher(msg);
		while (m.find()) {
		  return m.group(1);
		}
		
		return "";
	}

	//main method
	public static void main(String args[])
	{	
		
		String clientId = args[0].toString();
		int sPort  = Integer.parseInt(args[1]);
		System.out.println("clientId -> " + clientId + ", sPort -> " + sPort);
		
		Client client = new Client(clientId);

		try {
			requestSocket = new Socket("localhost", sPort);
			System.out.println("Connected to localhost:" + sPort);
			
			// listening for server messages
			new ListenHandler(requestSocket).start();;
			
			// listening for user input
			client.run(requestSocket);			
			
			
		} catch (IOException io) {
			System.out.println("[ERROR] IOException occurred in main() !!");
		}
		
	}
	
	// Listens to input from server
	private static class ListenHandler extends Thread {
		private Socket requestSocket;
		
		ListenHandler (Socket requestSocket) {
			this.requestSocket = requestSocket;
		}
		
		public void run() {
			//System.out.println("Running thread to handle INPUT from server!");
			
			try {
				InputStream inRaw = requestSocket.getInputStream();
				ObjectInputStream in = new ObjectInputStream(inRaw);
				DataInputStream dis = new DataInputStream(inRaw);
				
				while(true) {
					//System.out.println("Waiting for message from server ...");
					String message = (String) in.readObject();	
					//System.out.println("\nmessage -> " + message);
					
					if(message.contains("file")) { // get ready to receive file byte array
						String fileName = message.split(":")[1].trim();
						System.out.println("Downloaded file: " + fileName);
						
						// get the file length
						int len = dis.readInt();
						//System.out.println("Length of data received -> " + len);
					
						byte[] bFile = new byte[len];
						if(len > 0) {
							// read the file into a buffer
							//System.out.println("Reading data from dataInputStream into bFile byte[]...");
							dis.readFully(bFile);
							
							// writing byte array into file on disk
							File file = new File(fileName);
							file.createNewFile(); // if file already exists will do nothing 
							FileOutputStream fos = new FileOutputStream(file, false); // override file contents							
							fos.write(bFile);
							fos.close();
							
							//System.out.println("Finished writing to file!");	
						}
						
					} else {
						System.out.println("Received message: " + message);
					}
				}
			
			} catch (IOException e) {
				e.printStackTrace();
				
				try {
					this.requestSocket.close();
					System.out.println("[ERROR] Shutting down client.");
					System.exit(0);
				} catch (IOException e1) {
					//e1.printStackTrace();
				}
				
			} catch (ClassNotFoundException cnf) {
				System.out.println("[ERROR] ClassNotFoundException occurred in ListenHandler run() !!");
				
			}

		}
	}
}