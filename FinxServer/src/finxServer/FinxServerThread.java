package finxServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class FinxServerThread extends Thread {

	
	private String finx_server_folder_path;
	private InputStreamReader inReader;
	private BufferedReader input;
	private FileOutputStream output;
	private PrintStream outputProtocol;
	private String stringMAC;
	private String userIdentity;
	private Socket finxSocket;
	private InputStream in;
	private DataInputStream clientData;
	
	
	
	// for the time being this is hardcoded - later it will be user specific
	private String serverLogPath = "Assets/Serverlog.txt";
   
	private File myServerLog;
	private FileReader fileReader;
	private BufferedReader buffFileReader;
	
	public FinxServerThread(Socket finxSocket, String userIdentity) {
		// setup - assuming the FinxFolder has already been created on the Server
		this.finxSocket = finxSocket;
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
		setFileReceptionStream();
		receiveFiles();
		/* Next the client will need to check the Server log */
		
	}
	
	public void receiveFiles() {
		int bytesRead; 
		while (true) {
			 try {
				String fileName = clientData.readUTF();
				setFileOutputStream(fileName);
				long size = clientData.readLong();  
				byte[] buffer = new byte[1024];
				while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1)     
		        {     
		            output.write(buffer, 0, bytesRead);     
		            size -= bytesRead;     
		        }  
			} catch (IOException e) {
				e.printStackTrace();
			}     
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setFileReceptionStream() {
		try {
			in = finxSocket.getInputStream();
			clientData = new DataInputStream(in);
		} catch (IOException e) {
			e.printStackTrace();
		}  
           
	}
	
	public void setFileOutputStream(String fileName) {
		 try {
			output = new FileOutputStream(fileName);
		} catch (FileNotFoundException e) {
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
			outputProtocol = new PrintStream(finxSocket.getOutputStream());
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
		outputProtocol.println("Authenticated");
	}
	
	public void testing() {
		FinxServerController.print_clients_map_Keys();
	}
	

}
