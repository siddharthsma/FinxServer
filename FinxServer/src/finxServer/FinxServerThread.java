package finxServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class FinxServerThread extends Thread {

	private InputStreamReader inProtocolReader;
	public BufferedReader inputProtocol;
	public PrintStream outputProtocol;

	private String stringMAC;
	private String userIdentity;
	private Socket finxProtocolsSocket;
	private Socket finxFilesSocket;
	private ClientCommandWatcher clientCommands;

	// for the time being this is hardcoded - later it will be user specific
	private String serverLogPath = "Assets/Serverlog.txt";
	private String serverPath = "/Users/sameerambegaonkar/Desktop/FinxServerFolder/";
	private Path FinxServerFolderPath;

	private File myServerLog;
	private FileReader fileReader;
	private BufferedReader buffFileReader;
	
	// Hashes that store
	public HashMap<String, File> fetched_map = new HashMap<String, File>();
	public HashMap<String, File> pushed_map = new HashMap<String, File>();
	
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
		setPath(serverPath);
		walkFileTree();
		FinxServerController.replace_clients_map_Key(this, userIdentity, stringMAC);
		try {
			FinxServerController.add_folder_watcher(serverPath, stringMAC);
		} catch (Exception e) {
			e.printStackTrace();
		}
		clientCommands = new ClientCommandWatcher(this);
		sendFetchRequests();
	}

	public void walkFileTree() {
		try {
			Files.walkFileTree(FinxServerFolderPath, new FileTreeWalker(this));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void pushToClient(String relFilePath) {
		try {
			String[] pathSplit = relFilePath.split("/");
			String fileName = pathSplit[pathSplit.length-1];
			sendFile(fetched_map.get(fileName).getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void receiveFile(String relFilePath) throws Exception {

		// Set up Input Object Stream on File transfer socket 
		ObjectInputStream ois = new ObjectInputStream(finxFilesSocket.getInputStream()); 
		
		// Set up the FileOutput Stream and byte array buffer
		FileOutputStream fos = null;  
		byte [] buffer = new byte[BUFFER_SIZE];  

		// Create directories indicated in the relFilePath
		String[] directories = relFilePath.split("/");
		String relDirPath = "";
		for (int i=0; i < directories.length - 1; i++) {
			if (i==0) {
				relDirPath = directories[i];
			} else {
				relDirPath += "/" + directories[i];
			}
		}
		File theFile = new File(serverPath + relDirPath);
		theFile.mkdirs();
		
		// 1. Read file name.  
		Object o = ois.readObject();  

		if (o instanceof String) {  
			fos = new FileOutputStream(serverPath + relFilePath);  
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
	
	public void sendFetchRequests() {
		/* Actually at this point we would want to read from the server log and 
		 * determine what needs to be sent to the Client for the fetching operation
		 */
		Iterator<String> fetchIterator = fetched_map.keySet().iterator();
		while (fetchIterator.hasNext()) {
	        /* Send the protocol message */
			File myFile = fetched_map.get(fetchIterator.next());
			String[] filePathSplit = myFile.getPath().split("FinxServerFolder/");
			outputProtocol.println("fetchrequest#" + filePathSplit[1] );
		}
	}
	
	public void sendFetchRequest(String absolutePath) {
		String[] filePathSplit = absolutePath.split("FinxServerFolder/");
		outputProtocol.println("fetchrequest#" + filePathSplit[1] );
	}

	public void sendFile(String myFilePath) throws IOException {   

		File myFile = new File(myFilePath);  
		
		//Set Output Object Streams on the file transfer socket 
		ObjectOutputStream oos = new ObjectOutputStream(finxFilesSocket.getOutputStream());  

		// Write the name of the file
		oos.writeObject(myFile.getName());  

		// Write the rest of the file
		FileInputStream fis = new FileInputStream(myFile);  
		byte [] buffer = new byte[BUFFER_SIZE];  
		Integer bytesRead = 0;  

		while ((bytesRead = fis.read(buffer)) > 0) {  
			oos.writeObject(bytesRead);  
			oos.writeObject(Arrays.copyOf(buffer, buffer.length));  
		}   
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
	
	public void setPath(String Path) {
		FinxServerFolderPath = Paths.get(Path);
	}

	public void testing() {
		FinxServerController.print_clients_map_Keys();
	}

}
