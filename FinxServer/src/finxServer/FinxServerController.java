package finxServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;

public class FinxServerController {

	
	private static ServerSocket finxServerSocket = null;
	private static Socket finxSocket;
	private static HashMap<String,FinxServerThread> clients_map = new HashMap<String, FinxServerThread>();
	public static void main(String[] args) {
	
		//sockets
		set_ServerSocket(9390);
		listen_for_connections();
		
	}

	
	public static void set_ServerSocket(int port) {
		try {
			finxServerSocket = new ServerSocket(port);
		} catch(Exception e) {
			System.out.println(e);
		}
	}
	
	public static void listen_for_connections() {
		try {
			int clientNumber = 0;
			String stringedClientNumber;
			while(true) {
			finxSocket = finxServerSocket.accept();
			stringedClientNumber = Integer.toString(clientNumber);
			clients_map.put(stringedClientNumber, new FinxServerThread(finxSocket,stringedClientNumber));
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
	
	// for testing purposes
	public static void print_clients_map_Keys() {
		System.out.println(clients_map.toString());
	}
	
}
