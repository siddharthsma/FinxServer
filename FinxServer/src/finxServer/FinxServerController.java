package finxServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import net.contentobjects.jnotify.JNotify;

public class FinxServerController {

	
	private static ServerSocket finxProtocolsServerSocket = null;
	private static ServerSocket finxFilesServerSocket = null;
	private static Socket finxProtocolsSocket;
	private static Socket finxFilesSocket;
	private static HashMap<String,FinxServerThread> clients_map = new HashMap<String, FinxServerThread>();
	public static final int PROTOCOLS_PORT = 9390;
	public static final int FILES_PORT = 9391;
	
	public static void main(String[] args) {
	
		//sockets
		set_ServerSocket();
		listen_for_connections();
		
	}

	public static void set_ServerSocket() {
		try {
			finxProtocolsServerSocket = new ServerSocket(PROTOCOLS_PORT);
			finxFilesServerSocket = new ServerSocket(FILES_PORT);
		} catch(Exception e) {
			System.out.println(e);
		}
	}
	
	public static void listen_for_connections() {
		try {
			int clientNumber = 0;
			String stringedClientNumber;
			while(true) {
				finxProtocolsSocket = finxProtocolsServerSocket.accept();
				finxFilesSocket = finxFilesServerSocket.accept();
				stringedClientNumber = Integer.toString(clientNumber);
				clients_map.put(stringedClientNumber, new FinxServerThread(finxProtocolsSocket, finxFilesSocket,stringedClientNumber));
				clientNumber++;
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static void replace_clients_map_Key(FinxServerThread itself, String oldKey, String newKey) {
		clients_map.remove(oldKey);
		clients_map.put(newKey, itself);
	}
	
	public static void add_folder_watcher(String folderPath, String MACaddr) throws Exception{
		new FolderWatcherThread(folderPath, clients_map.get(MACaddr));
	}
	
	// for testing purposes
	public static void print_clients_map_Keys() {
		System.out.println(clients_map.toString());
	}
	
}
