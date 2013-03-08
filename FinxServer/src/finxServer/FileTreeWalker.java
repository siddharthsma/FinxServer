package finxServer;

import java.io.File;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileTreeWalker extends SimpleFileVisitor<Path>{

	private FinxServerThread myServerThread;
	
	public FileTreeWalker(FinxServerThread myServerThread) {
		this.myServerThread = myServerThread;
	}
	
	public FileVisitResult visitFile(Path filePath, BasicFileAttributes attr) {
		File aFile = new File(filePath.toString());
		myServerThread.fetched_map.put(aFile.getName(), aFile);
		return FileVisitResult.CONTINUE;
	}
	
}
