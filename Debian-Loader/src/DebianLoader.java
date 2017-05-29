import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import main.Loader;

public class DebianLoader implements Loader {

	@Override
	public Collection<File> loadFiles() {
		// TODO Auto-generated method stub
		String[] repos= {"main","multiverse","restricted","universe"};
		String server = "archive.ubuntu.com";//
		FTPClient client = new FTPClient();
		try {
			client.connect(server,21);
			client.login("","");
			FTPFile[] listDirectories = client.listDirectories("/ubuntu/dists/");
			
			for (FTPFile f: listDirectories){
				System.out.println(f.getName());
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
