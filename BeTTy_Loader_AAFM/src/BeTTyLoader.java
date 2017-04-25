import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;

import es.us.isa.FAMA.models.FAMAAttributedfeatureModel.FAMAAttributedFeatureModel;
import es.us.isa.FAMA.models.domain.Range;
import es.us.isa.generator.FM.AbstractFMGenerator;
import es.us.isa.generator.FM.FMGenerator;
import es.us.isa.generator.FM.attributed.AttributedCharacteristic;
import es.us.isa.generator.FM.attributed.AttributedFMGenerator;
import es.us.isa.utils.FMWriter;
import main.Loader;

public class BeTTyLoader implements Loader {

	Collection<File> result = new LinkedList<File>();
	int[] features = { 5, 10, 20, 30, 40, 50, 100, 150, 200, 500, 1000, 2000, 5000, 10000 };
	int[] ctc = { 5, 10, 20, 30, 40, 50, 100 };
	int[] ectc = { 5, 10, 20, 30, 40, 50, 100 };

	@Override
	public Collection<File> loadFiles() {

		for (int nf : features) {
			for (int nc : ctc) {
				for (int enc : ectc) {

					for (int n = 0; n < 10;) {
						// STEP 1: Specify the user's preferences for the
						// generation
						// (so-called
						// characteristics)
						// Our characteristics are AttributedCharacteristics
						AttributedCharacteristic characteristics = new AttributedCharacteristic();
						characteristics.setNumberOfFeatures(nf); // Number of
																	// features
						characteristics.setPercentageCTC(nc); // Percentage of
																// cross-tree
						// constraints.
						characteristics.setNumberOfExtendedCTC(enc);
						characteristics.setAttributeType(AttributedCharacteristic.INTEGER_TYPE);
						characteristics
								.setDefaultValueDistributionFunction((AttributedCharacteristic.UNIFORM_DISTRIBUTION));
						characteristics.addRange(new Range(3, 100));
						characteristics.setNumberOfAttibutesPerFeature(5);
						String argumentsDistributionFunction[] = { "3", "100" };
						characteristics.setDistributionFunctionArguments(argumentsDistributionFunction);
						characteristics.setHeadAttributeName("Atribute");

						// STEP 2: Generate the model with the specific
						// characteristics (FaMa
						// Attributed FM metamodel is used)
						characteristics.setSeed(characteristics.getSeed() + n);
						AbstractFMGenerator gen = new FMGenerator();
						AttributedFMGenerator generator = new AttributedFMGenerator(gen);
						FAMAAttributedFeatureModel afm = (FAMAAttributedFeatureModel) generator
								.generateFM(characteristics);

						FMWriter writer = new FMWriter();
						writer.saveFM(afm, "./out/model-" + nf + "-" + nc + "-" + enc + "-" + n + "-3-100-" + ".afm");

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
		
		PrintWriter out;
		File res = new File("./out/data.csv");
		try {
			out = new PrintWriter(res);
			for (int nf : features) {
				for (int nc : ctc) {
					for(int enc : ectc){
						for (int n = 0; n < 10;) {
					out.println( nf + ";" + nc +";"+enc+";"+n+ "-3-100;"+"./out/model-" + nf + "-" + nc +"-"+enc+"-"+n+ "-3-100-"+".afm");
						}
					}
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return res;
	}

}
