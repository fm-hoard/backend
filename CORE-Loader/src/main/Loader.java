package main;

import java.io.File;
import java.util.Collection;

public interface Loader {
	Collection<File> loadFiles();
	String getFormat();
	File getOriginData();
}
