import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import es.us.isa.FAMA.models.DebianVariabilityModel.BreaksRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.ConflictsRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.DEBpackage;
import es.us.isa.FAMA.models.DebianVariabilityModel.DebianVariabilityModel;
import es.us.isa.FAMA.models.DebianVariabilityModel.DependsRalation;
import es.us.isa.FAMA.models.DebianVariabilityModel.ProvidesRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.RecommendsRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.Relation;
import es.us.isa.FAMA.models.DebianVariabilityModel.ReplacesRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.SuggestsRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.preDependsRelation;
import es.us.isa.FAMA.models.DebianVariabilityModel.fileformats.PackagesGzReader;
import es.us.isa.FAMA.models.FAMAfeatureModel.Dependency;
import es.us.isa.FAMA.models.FAMAfeatureModel.ExcludesDependency;
import es.us.isa.FAMA.models.FAMAfeatureModel.FAMAFeatureModel;
import es.us.isa.FAMA.models.FAMAfeatureModel.Feature;
import es.us.isa.FAMA.models.FAMAfeatureModel.RequiresDependency;
import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.GraphVizWriter;
import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.XMLWriter;
import es.us.isa.FAMA.models.featureModel.Cardinality;

public class DEB2OVM {

	static Collection<DEBpackage> processed = new LinkedList<DEBpackage>();
	static Map<DEBpackage, Collection<DEBpackage>> replaces = new HashMap<DEBpackage, Collection<DEBpackage>>();
	static Map<Feature, Feature> excludesRel = new HashMap<Feature, Feature>();

	static FAMAFeatureModel fm = new FAMAFeatureModel();

	public static void main(String[] args) throws Exception {
		Collection<String> paths = new ArrayList<String>();

		paths.add("./inputs/main80403");
		// paths.add("./inputs/restricted80403");
		// paths.add("./inputs/multiverse80403");
		// paths.add("./inputs/universe80403");
		PackagesGzReader read = new PackagesGzReader(paths);
		DebianVariabilityModel dvm = (DebianVariabilityModel) read.parseFile();
		for (DEBpackage pkg : dvm.getElements()) {
			for (Relation rel : pkg.getRelations()) {
				if (rel instanceof ReplacesRelation) {
					for (DEBpackage child : rel.getChilds()) {
						if (replaces.containsKey(child)) {
							Collection<DEBpackage> col = replaces.get(child);
							col.add(pkg);
							replaces.put(child, col);
						} else {
							Collection<DEBpackage> col = new ArrayList<DEBpackage>();
							col.add(pkg);
							replaces.put(child, col);
						}
					}
				}
			}
		}
		transform(dvm);
		generateExcludes(dvm);


		XMLWriter writer = new XMLWriter();
		writer.writeFile("./text.xml", fm);
		// OVMWriter writer = new OVMWriter();
		// writer.writeFile("./main.ovm", res);
	}

	private static void generateExcludes(DebianVariabilityModel dvm) {
		for (Entry<Feature, Feature> e : excludesRel.entrySet()) {
			if (!existExcludes(fm, e.getKey(), e.getValue())) {
				ExcludesDependency dep = new ExcludesDependency("", e.getKey(), e.getValue());
				fm.addDependency(dep);

			}

		}
	}

	private static boolean existExcludes(FAMAFeatureModel fm2, Feature key, Feature value) {
		boolean res = false;
		Iterator<Dependency> dependencies = fm.getDependencies();
		while (dependencies.hasNext() && !res) {
			Dependency next = dependencies.next();
			if (next instanceof ExcludesDependency && ((next.getOrigin().getName().equals(key.getName())
					&& next.getDestination().getName().equals(value.getName()))
					|| (next.getOrigin().getName().equals(value.getName())
							&& next.getDestination().getName().equals(key.getName())))) {
				res = true;
			}
		}

		return false;
	}

	private static void createOptional(Feature parent, Feature child) {
		es.us.isa.FAMA.models.FAMAfeatureModel.Relation rel = new es.us.isa.FAMA.models.FAMAfeatureModel.Relation("");
		rel.addCardinality(new Cardinality(0, 1));
		rel.addDestination(child);
		rel.setParent(parent);
		parent.addRelation(rel);
	}

	private static void createMandatory(Feature parent, Feature child) {
		es.us.isa.FAMA.models.FAMAfeatureModel.Relation rel = new es.us.isa.FAMA.models.FAMAfeatureModel.Relation("");
		rel.addCardinality(new Cardinality(1, 1));
		rel.addDestination(child);
		rel.setParent(parent);
		parent.addRelation(rel);
	}

	private static Feature getFeature(String name) {
		Feature res = fm.searchFeatureByName(name);
		if (res == null) {
			for (Feature f : excludesRel.values()) {
				if (f.getName().equals(name)) {
					res = f;
				}
			}
		}
		if (res == null) {
			res = new Feature(name);
		}
		return res;
	}

	public static void transform(DebianVariabilityModel dvm) {
		Feature root = new Feature("root");

		// Every package will be map to a VP
		for (DEBpackage pkg : dvm.getElements()) {
			// create a fake root and link all packages with it
			// if not processed
			if (!processed.contains(pkg)) {
				processed.add(pkg);
				Feature child = getFeature(pkg.getName() + "(" + pkg.getVersion().toString() + ")");
				createOptional(root, child);
				processDEB(pkg, child);
			}
		}
		fm.setRoot(root);
	}

	public static void processDEB(DEBpackage pkg, Feature feat) {
		
		if (!processed.contains(pkg)) {
			processed.add(pkg);
		//	System.out.println("Processing: " + pkg.getName() + "(" + pkg.getVersion().toString() + ")");
			for (Relation rel : pkg.getRelations()) {
				Collection<DEBpackage> childs = rel.getChilds();

				// we will map this to a mandatory relationship
				if (rel instanceof DependsRalation) {
					for (DEBpackage deb : childs) {
						String name = deb.getName() + "(" + deb.getVersion().toString() + ")";
						if (fm.searchFeatureByName(name) == null) {
							// Aun no se ha añadido el paquete
							Feature child = getFeature(name);
							createMandatory(feat, child);
							processDEB(deb, child);
						} else {
							// Existe el paquete
							System.err.println("existe");
							Feature child = getFeature(name);
							Dependency dep = new RequiresDependency("", feat, child);
							fm.addDependency(dep);

						}
					}
					// we can map this to a mandatory/requires relationship
				} else if (rel instanceof preDependsRelation) {
					for (DEBpackage deb : childs) {
						String name = deb.getName() + "(" + deb.getVersion().toString() + ")";
						if (fm.searchFeatureByName(name) == null) {
							// Aun no se ha añadido el paquete
							Feature child = getFeature(name);
							createMandatory(feat, child);
							processDEB(deb, child);
						} else {
							// Existe el paquete
							System.err.println("existe");
							Feature child = getFeature(name);
							Dependency dep = new RequiresDependency("", feat, child);
							fm.addDependency(dep);
						}
					}

				} else if (rel instanceof ConflictsRelation) {
					/**
					 * Should Depend on the VP (requires and excludes are at vp
					 * level)but if we do that its imposible to manage with
					 * replacements
					 */

					for (DEBpackage deb : childs) {
						String nameDest = deb.getName() + "(" + deb.getVersion().toString() + ")";
						excludesRel.put(feat, getFeature(nameDest));
					}
					// we will map this to a optional relationship
				} else if (rel instanceof RecommendsRelation) {
					for (DEBpackage deb : childs) {
						String name = deb.getName() + "(" + deb.getVersion().toString() + ")";
						if (fm.searchFeatureByName(name) == null) {
							// Aun no se ha añadido el paquete
							Feature child = getFeature(name);
							createOptional(feat, child);
							processDEB(deb, child);
						}
					}

					// we will map this to a optional relationship
				} else if (rel instanceof SuggestsRelation) {
					for (DEBpackage deb : childs) {
						String name = deb.getName() + "(" + deb.getVersion().toString() + ")";
						if (fm.searchFeatureByName(name) == null) {
							// Aun no se ha añadido el paquete
							Feature child = getFeature(name);
							createOptional(feat, child);
							processDEB(deb, child);
						}
					}

					// we will map this to a __________ relationship
				} else if (rel instanceof ProvidesRelation) {

					// we will map this to a excludes relationship
				} else if (rel instanceof BreaksRelation) {

					/**
					 * Should Depend on the VP (requires and excludes are at vp
					 * level)but if we do that its imposible to manage with
					 * replacements
					 */

					for (DEBpackage deb : childs) {
						String nameDest = deb.getName() + "(" + deb.getVersion().toString() + ")";
						excludesRel.put(feat, getFeature(nameDest));
					}
					// we will map this to a excludes relationship
				}

			}
		}
	}

}
