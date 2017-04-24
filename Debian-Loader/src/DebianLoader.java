import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import main.Loader;

public class DebianLoader implements Loader {

	@Override
	public Collection<File> loadFiles() {
		// TODO Auto-generated method stub
		String[] repos= {"main","multiverse","restricted","universe"};
		String server = "";
		FTPClient client = new FTPClient();
		try {
			client.connect(server);
			FTPFile[] listFiles = client.listDirectories();
			for (FTPFile f: listFiles){
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getOriginData() {
		// TODO Auto-generated method stub
		return null;
	}

}
