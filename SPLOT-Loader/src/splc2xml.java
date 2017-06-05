import java.io.File;

import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.SPLXReader;
import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.XMLWriter;

public class splc2xml {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		File dir = new File("./out");
		for (File f:dir.listFiles()){
			SPLXReader reader = new SPLXReader();
			XMLWriter writer = new XMLWriter();
			writer.writeFile("./out2/"+f.getName().replaceAll(".splx", ""), reader.parseFile("./out/"+f.getName()));
		}
	}

}
