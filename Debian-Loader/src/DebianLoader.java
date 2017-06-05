import java.io.File;
import java.util.Collection;

import es.us.isa.FAMA.models.FAMAfeatureModel.FAMAFeatureModel;
import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.XMLReader;
import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.XMLWriter;
import es.us.isa.FAMA.models.variabilityModel.VariabilityModel;
import es.us.isa.FAMA.models.variabilityModel.parsers.WrongFormatException;
import main.Loader;

public class DebianLoader implements Loader {

	@Override
	public Collection<File> loadFiles() throws Exception {
		File f= new File("./inputs/");
		for(File subfile:f.listFiles()){
			if(subfile.isDirectory()&&!subfile.getName().startsWith(".")){
				DEB2FM parser = new DEB2FM();
				FAMAFeatureModel res =parser.extractModel(
							f.getPath()+"/"+subfile.getName()+"/main",
							f.getPath()+"/"+subfile.getName()+"/multiverse",
							f.getPath()+"/"+subfile.getName()+"/restricted",
							f.getPath()+"/"+subfile.getName()+"/universe"							
							);
					XMLWriter w = new XMLWriter();
					w.writeFile("./out/"+subfile.getName()+".xml", res);
					XMLReader reader = new XMLReader();
					FAMAFeatureModel parseFile = (FAMAFeatureModel)reader.parseFile("./out/"+subfile.getName()+".xml");
					System.out.println(res.getFeatures().size()+" "+res.getNumberOfDependencies());
					System.out.println(parseFile.getFeatures().size()+" "+parseFile.getNumberOfDependencies());
					
			}
		}
		return null;
	}

	@Override
	public String getFormat() {
		return "FAMAXML";
	}

	@Override
	public File getOriginData() {
		// TODO Auto-generated method stub
		return null;
	}

}
