package finxServer;

import java.io.File;
import java.io.IOException;


import net.contentobjects.jnotify.JNotifyListener;

public class FinxServerListener implements JNotifyListener{
	

	private FinxServerThread serverThread;
	
	public FinxServerListener(FinxServerThread serverThread) {
		super();
		this.serverThread = serverThread;
	}
	
	public void fileRenamed(int wd, String rootPath, String oldName,
			String newName) {
		
	}

	public void fileModified(int wd, String rootPath, String name) {
		
		String filePath = rootPath + name;
		File theFile = new File(rootPath + name);
		serverThread.fetched_map.put(theFile.getName(), theFile);
		serverThread.sendFetchRequest(filePath);
		
	}

	public void fileDeleted(int wd, String rootPath, String name) {
		
		
	}

	public void fileCreated(int wd, String rootPath, String name) {
		
		String filePath = rootPath + name;
		System.out.println("The listener path is: " + filePath);
		File theFile = new File(rootPath + name);
		if (theFile.isFile()) {
			serverThread.fetched_map.put(theFile.getName(), theFile);
			serverThread.sendFetchRequest(filePath);
		}
		
		
	}

	void print(String msg) {
		System.err.println(msg);
	}
}
