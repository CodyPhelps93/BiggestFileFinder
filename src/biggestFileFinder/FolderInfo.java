package biggestFileFinder;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FolderInfo {
Path path;
long size;
FileTime lastModifiedTime;

public FolderInfo(Path path, long size, FileTime lastModifiedTime) {
	this.path = path;
	this.size = size;
	this.lastModifiedTime = lastModifiedTime;
}

public Path getPath() {
	return path;
}

public long getSize() {
	return size;
}

public FileTime getLastModifiedTime() {
	return lastModifiedTime;
}

}
