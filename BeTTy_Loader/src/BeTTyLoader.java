import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;

import es.us.isa.ChocoReasoner.ChocoReasoner;
import es.us.isa.ChocoReasoner.questions.ChocoValidQuestion;
import es.us.isa.FAMA.models.FAMAfeatureModel.FAMAFeatureModel;
import es.us.isa.generator.FM.AbstractFMGenerator;
import es.us.isa.generator.FM.FMGenerator;
import es.us.isa.generator.FM.GeneratorCharacteristics;
import es.us.isa.utils.FMWriter;
import main.Loader;

public class BeTTyLoader implements Loader {

	@Override
	public Collection<File> loadFiles() {

		Collection<File> result = new LinkedList<File>();
		int[] features = { 5, 10, 20, 30, 40, 50, 100, 150, 200, 500, 1000, 2000, 5000, 10000 };
		int[] ctc = { 5, 10, 20, 30, 40, 50, 100 };

		for (int nf : features) {
			for (int nc : ctc) {

				for (int n = 0; n < 10;) {
					GeneratorCharacteristics characteristics = new GeneratorCharacteristics();
					characteristics.setNumberOfFeatures(nf);
					characteristics.setPercentageCTC(nc);

					AbstractFMGenerator generator = new FMGenerator();
					FAMAFeatureModel fm = (FAMAFeatureModel) generator.generateFM(characteristics);

					ChocoReasoner reasoner = new ChocoReasoner();
					fm.transformTo(reasoner);
					ChocoValidQuestion vq = new ChocoValidQuestion();
					vq.answer(reasoner);

					if (vq.isValid()) {
						n++;

						FMWriter writer = new FMWriter();
						try {
							writer.saveFM(fm, "./out/model-" + nf + "-" + nc + "-" + n + ".xml");
							result.add(new File("./out/model-" + nf + "-" + nc + "-" + n + ".xml"));
						} catch (Exception e) {

							e.printStackTrace();
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	public String getFormat() {
		return "FAMA";
	}

	@Override
	public File getOriginData() {
		int[] features = { 5, 10, 20, 30, 40, 50, 100, 150, 200, 500, 1000, 2000, 5000, 10000 };
		int[] ctc = { 5, 10, 20, 30, 40, 50, 100 };
		PrintWriter out;
		File res =new File("./out/data.csv");
		try {
			out = new PrintWriter(res);
			for (int nf : features) {
				for (int nc : ctc) {
					out.println( nf + ";" + nc + ";"+ "./out/model-" + nf + "-" + nc + ".xml");
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		
		return res;
	}

}
