package finxServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class FinxServerThread extends Thread {

	private InputStreamReader inProtocolReader;
	private BufferedReader inputProtocol;
	private PrintStream outputProtocol;

	private String stringMAC;
	private String userIdentity;
	private Socket finxProtocolsSocket;
	private Socket finxFilesSocket;

	// for the time being this is hardcoded - later it will be user specific
	private String serverLogPath = "Assets/Serverlog.txt";
	private String serverPath = "/Users/sameerambegaonkar/Desktop/FinxServerFolder/";

	private File myServerLog;
	private FileReader fileReader;
	private BufferedReader buffFileReader;

	public static final int BUFFER_SIZE = 100;  

	public FinxServerThread(Socket finxProtocolsSocket, Socket finxFilesSocket, String userIdentity) {
		// setup - assuming the FinxFolder has already been created on the Server
		this.finxProtocolsSocket = finxProtocolsSocket;
		this.finxFilesSocket = finxFilesSocket;
		System.out.println(finxProtocolsSocket.isConnected());
		System.out.println(finxFilesSocket.isConnected());
		this.userIdentity = userIdentity;
		setInputProtocolStream();
		setOutputProtocolStream();
		start();
	}

	// Interactions with individual Clients done here
	public void run() {

		/* The first thing that the client must do is authenticate using a MAC address
		then make the MAC address the new key in the clients_map hash so we know who the
		thread belongs to */
		authenticate();
		testing();
		FinxServerController.replace_clients_map_Key(this, userIdentity, stringMAC);
		while(true) {
			waitForCommands();
		}
	}

	public void waitForCommands() {
		/* The idea is that the Server is Passive, which means that it will wait
		 * for commands to be sent by the Client via protocol messages and then react
		 * accordingly.
		 */
		String command = null;
		try {
			command = inputProtocol.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("The command issued is: " + command);

		if (command.equals("lastpushtime")) {
			sendLastPushTime();
		}
		else if (command.startsWith("push")) {
			String[] info = command.split("#");
			try {
				//receiveFile(info[1], Long.valueOf((info[2])) );
				receiveFile(info[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {

		}
	}

	public void receiveFile(String fileName) throws Exception {

		ObjectOutputStream oos = new ObjectOutputStream(finxFilesSocket.getOutputStream());  
		ObjectInputStream ois = new ObjectInputStream(finxFilesSocket.getInputStream());  
		FileOutputStream fos = null;  
		byte [] buffer = new byte[BUFFER_SIZE];  

		// 1. Read file name.  
		Object o = ois.readObject();  

		if (o instanceof String) {  
			fos = new FileOutputStream(serverPath + o.toString());  
		} else {  
			throwException("Something is wrong");  
		}  

		// 2. Read file to the end.  
		Integer bytesRead = 0;  

		do {  
			o = ois.readObject();  

			if (!(o instanceof Integer)) {  
				throwException("Something is wrong");  
			}  

			bytesRead = (Integer)o;  

			o = ois.readObject();  

			if (!(o instanceof byte[])) {  
				throwException("Something is wrong");  
			}  

			buffer = (byte[])o;  

			// 3. Write data to output file.  
			fos.write(buffer, 0, bytesRead);  
		} while (bytesRead == BUFFER_SIZE);  

		fos.close();  
	}

	public static void throwException(String message) throws Exception {  
		throw new Exception(message);  
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

	public void setInputProtocolStream() {
		try {
			inProtocolReader = new InputStreamReader(finxProtocolsSocket.getInputStream());
			inputProtocol = new BufferedReader(inProtocolReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setOutputProtocolStream() {
		try {
			outputProtocol = new PrintStream(finxProtocolsSocket.getOutputStream(), true /* autoflush */);
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
