package finxServer;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;

public class FolderWatcherThread extends Thread {

	private String folderPath;
	private FinxServerThread serverThread;
	
	public FolderWatcherThread(String folderPath, FinxServerThread serverThread) {
		this.folderPath = folderPath;
		this.serverThread = serverThread;
		start();
	}
	
	public void run() {
		// watch mask, specify events you care about,
	    // or JNotify.FILE_ANY for all events.
	    int mask = JNotify.FILE_CREATED  | 
	               JNotify.FILE_DELETED  | 
	               JNotify.FILE_MODIFIED | 
	               JNotify.FILE_RENAMED;

	    // watch subtree?
	    boolean watchSubtree = true;

	    // add actual watch
	    int watchID;
		try {
			watchID = JNotify.addWatch(folderPath, mask, watchSubtree, new FinxServerListener(serverThread));
			 // sleep a little, the application will exit if you
		    // don't (watching is asynchronous), depending on your
		    // application, this may not be required
		    Thread.sleep(1000000);
		 // to remove watch the watch
		    boolean res = JNotify.removeWatch(watchID);
		    if (!res) {
		       //invalid watch ID specified.
		    }
		} catch (JNotifyException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}  
	}
}
