import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CustomStream {
	public OutputStream outRaw;
	public ObjectOutputStream out;
	public DataOutputStream dos;
	
	CustomStream(OutputStream outRaw, ObjectOutputStream out, DataOutputStream dos) {
		this.outRaw = outRaw;
		this.out = out;
		this.dos = dos;
	}	
}

public class Server {
	
	private static final int sPort = 8000;
	private static Hashtable<String, CustomStream> clientOutStreams = new Hashtable<String, CustomStream>();

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running on port " + sPort);
		ServerSocket listener = new ServerSocket(sPort);
		try {
			while (true) {
				new ListenHandler(listener.accept()).start();
			}
		} finally {
			System.out.println("Server main() method: Closing listener");
			listener.close();
		}

	}

	/**
	 * Handles communication for a single client
	 * If there are 4 clients, 4 threads are spawned
	 */
	private static class ListenHandler extends Thread {
		private Socket connection;
		private String senderClientId;

		public ListenHandler(Socket connection) {
			this.connection = connection;
		}

		public void run() {
			boolean duplicateFlag = false;
			
			try {
				//System.out.println("initialize Input and Output streams");
				
				OutputStream outRaw = connection.getOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(outRaw);
				out.flush();
				DataOutputStream dos = new DataOutputStream(outRaw);
				
				InputStream inRaw = connection.getInputStream();
				ObjectInputStream in = new ObjectInputStream(inRaw);	
				DataInputStream dis = new DataInputStream(inRaw);
				
				try {
					while (true) {
						
						String message = (String) in.readObject(); // blocking							
						boolean isFirstMsg = message.toLowerCase().contains("firstmsg:");
						
						// get the clientId if the client messages for the first time
						if(isFirstMsg) {
							this.senderClientId = message.split(":")[1].trim();
							
							System.out.println("Client " + this.senderClientId +" connected");
							
							if(clientOutStreams.containsKey(this.senderClientId)) {
								System.out.println("[ERROR] Duplicate clientId: " + this.senderClientId + ", cannot connect");
								
								duplicateFlag = true;
								in.close();
								out.close();
								connection.close();
								
							} else {
								clientOutStreams.put(this.senderClientId, new CustomStream(outRaw, out, dos));
							}
							
						} else {
							//System.out.println("Operation: " + message);
							
							byte[] bFile = new byte[0];
							
							if(message.contains("file")) {
								//System.out.println("message contains file keyword!");								
								
								int len = dis.readInt();
								//System.out.println("Length of data received -> " + len);
								
								bFile = new byte[len];
								if(len > 0) {
									//System.out.println("Reading data from dataInputStream into bFile byte[]...");
									dis.readFully(bFile);
								}
							}
							
							// handles both message and file
							new SendHandler(this.senderClientId, message, bFile).start();
						}
					}
				} catch (ClassNotFoundException classnot) {
					System.err.println("Data received in unknown format");
				}
			} catch (IOException ioException) {
				System.out.println("Disconnected with Client " + this.senderClientId);
			} finally {
				//System.out.println("Server: Close connections");
				try {
					if(!duplicateFlag) { // normal Ctrl+C shutdown
					 //System.out.println("Removing " + this.senderClientId + " from hashtable...");
					 clientOutStreams.remove(this.senderClientId);
					}
					
					//in.close();
					//out.close();
					connection.close();
				} catch (IOException ioException) {
					//System.out.println("Disconnect with Client " + this.senderClientId);
				}
			}
		}	
	}
	
	/**
	 * sends messages to the appropriate broadcast/unicast/blockcast clients
	 * eg: blockcast "hey there" client3
	 *
	 */
	private static class SendHandler extends Thread {
		String senderClientId;
		String message;
		byte[] bFile;
		
		public SendHandler(String senderClientId, String msg, byte[] bFile) {
			this.senderClientId = senderClientId;
			this.message = msg;
			this.bFile = bFile;
		}
		
		public void run() {			
			String dataInQuotes = matchDoubleQuotes(this.message); // message or filename
			String msgToSend = "@" + this.senderClientId + ":" + dataInQuotes;

			Set<String> clientIds = clientOutStreams.keySet();
			//System.out.println("clientOutStreams.size() -> " + clientOutStreams.size());
			
			if (isBroadCast()) {
				System.out.println("--- BROADCAST ---");
				for(String clientId: clientIds) {
					if(clientId.equals(this.senderClientId)) {
						continue;
					}
					
					System.out.println("Sending " + msgToSend + " to @" + clientId);
					
					CustomStream cs = clientOutStreams.get(clientId);
					ObjectOutputStream outStream = cs.out;
					DataOutputStream dosStream = cs.dos;
					
					// send message
					sendMessage(outStream, msgToSend);
					
					// send file
					if(bFile.length > 0) {
						//System.out.println("Sending file -> " + dataInQuotes + " to " + clientId);
						sendMessage(outStream, "file:" + dataInQuotes); // send fileName to client
						sendFile(dosStream, bFile); // send file to client
					}
				}
				
				System.out.println("---------------\n");
				
			} else if (isBlockCast()) {
				System.out.println("--- BLOCKCAST ---");
				String clientIdBlock = lastWord(this.message);
				System.out.println("Block ClientId -> " + clientIdBlock);

				for(String clientId: clientIds) {
					if((clientId.equals(this.senderClientId)) || (clientId.equals(clientIdBlock))) {
						continue;
					}
					
					System.out.println("Sending " + msgToSend + " to @" + clientId);
					
					CustomStream cs = clientOutStreams.get(clientId);
					//OutputStream outRawStream =  cs.outRaw;
					ObjectOutputStream outStream = cs.out;
					DataOutputStream dosStream = cs.dos;
					
					// send message
					sendMessage(outStream, msgToSend);
					
					// send file
					if(bFile.length > 0) {
						//System.out.println("Sending file -> " + dataInQuotes + "to " + clientId);
						sendMessage(outStream, "file:" + dataInQuotes); // send fileName to client
						sendFile(dosStream, bFile); // send file to client
					}
				}
				
				System.out.println("---------------\n");

			} else if (isUniCast()) {
				System.out.println("--- UNICAST ---");
				String clientIdUnicast = lastWord(this.message); // last word
				//System.out.println("Unicast ClientId -> " + clientIdUnicast);
				System.out.println("Sending " + msgToSend + " to @" + clientIdUnicast);
				
				CustomStream cs = clientOutStreams.get(clientIdUnicast);
				
				if(cs == null) {
					System.out.println("[ERROR] Client not found, please specify a valid clientId");
					
				} else {
					//OutputStream outRawStream =  cs.outRaw;
					ObjectOutputStream outStream = cs.out;
					DataOutputStream dosStream = cs.dos;
					
					// send message
					sendMessage(outStream, msgToSend);
					
					// send file
					if(bFile.length > 0) {
						//System.out.println("Sending file -> " + dataInQuotes + "to " + clientIdUnicast);
						sendMessage(outStream, "file:" + dataInQuotes); // send fileName to client
						sendFile(dosStream, bFile); // send file to client
					}
				}
				
				System.out.println("---------------\n");
				
			} else {
				System.out.println("[ERROR] Illegal command encountered, please try again");
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
		
		/**
		 * helper method to parse out the message text or file name from
		 * double quotes specified in the user's input
		 */
		public String matchDoubleQuotes(String msg) {
			Pattern p = Pattern.compile("\"([^\"]*)\"");
			Matcher m = p.matcher(msg);
			while (m.find()) {
			  return m.group(1);
			}
			
			return "";
		}
		
		// to get the clientId
		public String lastWord(String msg) {
			return msg.substring(msg.lastIndexOf(" ")+1);
		}
		
		// send a message to the output stream of the client
		public void sendMessage(ObjectOutputStream out, String msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
		
		// send a file to the output stream of the client
		public void sendFile(DataOutputStream dos, byte[] bFile) {
			try {
				int len = bFile.length;
				
				// Step2: Send file length
				//System.out.println("Sending length of file ...");
				dos.writeInt(len);
				
				// Step3: send file
				if(len > 0) {
					//System.out.println("Sending file contents ...");
					dos.write(bFile, 0, len);
				}
				
				dos.flush();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			}
			
		}	
	}
}