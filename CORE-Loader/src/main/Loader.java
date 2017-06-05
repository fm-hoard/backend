package main;

import java.io.File;
import java.util.Collection;

public interface Loader {
	Collection<File> loadFiles() throws Exception;
	String getFormat();
	File getOriginData();
}
