package finxServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FinxServerThread extends Thread {


	private String finx_server_folder_path;
	
	private InputStreamReader inProtocolReader;
	private BufferedReader inputProtocol;
	private PrintStream outputProtocol;
	
	private String stringMAC;
	private String userIdentity;
	private Socket finxSocket;
	private int fileWorkerCount = 0;

	// for the time being this is hardcoded - later it will be user specific
	private String serverLogPath = "Assets/Serverlog.txt";

	private File myServerLog;
	private FileReader fileReader;
	private BufferedReader buffFileReader;

	public FinxServerThread(Socket finxSocket, String userIdentity) {
		// setup - assuming the FinxFolder has already been created on the Server
		this.finxSocket = finxSocket;
		this.userIdentity = userIdentity;
		setInputProtocolStream(finxSocket);
		setOutputProtocolStream(finxSocket);
		start();
	}

	// Interactions with individual Clients done here
	public void run() {

		/* The first thing that the client must do is authenticate using a MAC address
		then make the MAC address the new key in the clients_map hash so we know who the
		thread belongs to */
		authenticate();
		FinxServerController.replace_clients_map_Key(this, userIdentity, stringMAC);
		waitForCommands();
	}

	public void waitForCommands() {
		/* The idea is that the Server is Passive, which means that it will wait
		 * for commands to be sent by the Client via protocol messages and then react
		 * accordingly.
		 */
		String command = null;
		try {
			while ((command = inputProtocol.readLine()) != null) {
				System.out.println("The command issued is: " + command);
				
				if (command.equals("lastpushtime")) {
					sendLastPushTime();
				}
				else if (command.startsWith("push")) {
					String[] info = command.split(":");
					openServerSocket();
					try {
						receiveFile(info[1]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("Unrecognised command");
				}
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveFile(String fileName) throws IOException {
	/*	int filesize=6022386; // filesize temporary hardcoded

	    long start = System.currentTimeMillis();
	    int bytesRead;
	    int current = 0;

	    // receive file
	    byte [] mybytearray  = new byte [filesize];
	    InputStream is = finxSocket.getInputStream();
	    FileOutputStream fos = new FileOutputStream(fileName);
	    BufferedOutputStream bos = new BufferedOutputStream(fos);
	    bytesRead = is.read(mybytearray,0,mybytearray.length);
	    current = bytesRead;

	    do {
	       bytesRead =
	          is.read(mybytearray, current, (mybytearray.length-current));
	       if(bytesRead >= 0) current += bytesRead;
	    } while(bytesRead > -1);

	    bos.write(mybytearray, 0 , current);
	    bos.flush();
	    long end = System.currentTimeMillis();
	    System.out.println(end-start);
	    bos.close();*/
	    //finxSocket.close();
	  }

	public static void throwException(String message) throws Exception {  
		throw new Exception(message);  
	}  
	
	public void openServerSocket() {
		fileWorkerCount++;
		int port = 9390 + fileWorkerCount;
		
		try {
			ServerSocket fileWorkerSocket = new ServerSocket(port);
			Socket workerSocket = fileWorkerSocket.accept();
			
			// now check that the right client has connected to the Socket
			if (!workerSocket.getInetAddress().equals(finxSocket.getInetAddress())) {
				// wrong client has connected - so terminate Socket and make port available
				workerSocket.setReuseAddress(true);
				workerSocket.close();
				try {
					sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendLastPushTime() {
		String stringedPushTime = "noTime";
		String logLine;
		String[] logLineParts;
		setFileInputStream(myServerLog, serverLogPath);
		try {
			while ((logLine = buffFileReader.readLine()) != null)  {
				logLineParts = logLine.split(",");
				if (logLineParts[1].equals(stringMAC)) {
					// 3rd column contains the times of changes
					stringedPushTime = logLineParts[3];
					break;
				}
			}
			outputProtocol.println(stringedPushTime);
			System.out.println("outputProtocol has sent: " + stringedPushTime);
		} catch (IOException e) {
			System.out.println("Exception at sendLastPushTime");
			e.printStackTrace();
		}
	}

	public void setFileInputStream(File theFile, String path) {
		try {
			theFile = new File(path);
			fileReader = new FileReader(theFile);
			buffFileReader = new BufferedReader(fileReader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void setInputProtocolStream(Socket finxSocket) {
		try {
			inProtocolReader = new InputStreamReader(finxSocket.getInputStream());
			inputProtocol = new BufferedReader(inProtocolReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setOutputProtocolStream(Socket finxSocket) {
		try {
			outputProtocol = new PrintStream(finxSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public void authenticate() {
		try {
			stringMAC = inputProtocol.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}
		// for the time being just authenticate any UUID
		outputProtocol.println("Authenticated");
	}

	public void testing() {
		FinxServerController.print_clients_map_Keys();
	}


}
