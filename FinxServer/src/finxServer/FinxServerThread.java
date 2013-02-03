package finxServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class FinxServerThread extends Thread {

	
	private String finx_server_folder_path;
	private InputStreamReader inReader;
	private BufferedReader input;
	private PrintStream output;
	private String stringMAC;
	private String userIdentity;
	
	// for the time being this is hardcoded - later it will be user specific
	private String serverLogPath = "Assets/Serverlog.txt";
   
	private File myServerLog;
	private FileReader fileReader;
	private BufferedReader buffFileReader;
	
	public FinxServerThread(Socket finxSocket, String userIdentity) {
		// setup - assuming the FinxFolder has already been created on the Server
		this.userIdentity = userIdentity;
		setProtocolInputStream(finxSocket);
		setProtocolOutputStream(finxSocket);
		start();
	}
	
	// Interactions with individual Clients done here
	public void run() {
		
		/* The first thing that the client must do is authenticate using a MAC address
		then make the MAC address the new key in the clients_map hash so we know who the
		thread belongs to */
		authenticate();
		FinxServerController.replace_clients_map_Key(this, userIdentity, stringMAC);
		testing();
		sendLastPushTime();
		/* Next the client will need to check the Server log */
		
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
			output.println(stringedPushTime);
		} catch (IOException e) {
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
	
	public void setProtocolInputStream(Socket finxSocket) {
		try {
			inReader = new InputStreamReader(finxSocket.getInputStream());
			input = new BufferedReader(inReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setProtocolOutputStream(Socket finxSocket) {
		try {
			output = new PrintStream(finxSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void authenticate() {
		try {
			stringMAC = input.readLine();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		// for the time being just authenticate any UUID
		output.println("Authenticated");
	}
	
	public void testing() {
		FinxServerController.print_clients_map_Keys();
	}
	

}
