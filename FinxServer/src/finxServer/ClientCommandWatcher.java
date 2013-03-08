package finxServer;

import java.io.IOException;

public class ClientCommandWatcher extends Thread{
	
	private FinxServerThread serverThread;
	
	public ClientCommandWatcher(FinxServerThread serverThread) {
		this.serverThread = serverThread;
		start();
	}
	
	public void run() {
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
			command = serverThread.inputProtocol.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("The command issued is: " + command);

		if (command.equals("lastpushtime")) {
			serverThread.sendLastPushTime();
		}
		else if (command.startsWith("push")) {
			String[] info = command.split("#");
			try {
				serverThread.receiveFile(info[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (command.startsWith("fetch")) {
			String[] info = command.split("#");
			try {
				serverThread.pushToClient(info[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {

		}
	}
}
