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
import es.us.isa.FAMA.models.featureModel.Cardinality;

public class DEB2FM {

	Map<DEBpackage, Collection<DEBpackage>> replaces = new HashMap<DEBpackage, Collection<DEBpackage>>();
	Map<Feature, Feature> excludesRel = new HashMap<Feature, Feature>();
	Map<Feature, Feature> dependsRel = new HashMap<Feature, Feature>();
	
	FAMAFeatureModel fm = new FAMAFeatureModel();

	public FAMAFeatureModel extractModel(String... paths) throws Exception {
		Collection<String> listOfRepos = new LinkedList<>();
		for (String s : paths) {
			listOfRepos.add(s);
		}
		PackagesGzReader read = new PackagesGzReader(listOfRepos);
		DebianVariabilityModel dvm = (DebianVariabilityModel) read.parseFile();
//		for (DEBpackage pkg : dvm.getElements()) {
//			for (Relation rel : pkg.getRelations()) {
//				if (rel instanceof ReplacesRelation) {
//					for (DEBpackage child : rel.getChilds()) {
//						if (replaces.containsKey(child)) {
//							Collection<DEBpackage> col = replaces.get(child);
//							col.add(pkg);
//							replaces.put(child, col);
//						} else {
//							Collection<DEBpackage> col = new ArrayList<DEBpackage>();
//							col.add(pkg);
//							replaces.put(child, col);
//						}
//					}
//				}
//			}
//		}
		transform(dvm);
		generateDepends();
		generateExcludes();

		return fm;
	}

	private void generateDepends() {
		for (Entry<Feature, Feature> e : dependsRel.entrySet()) {
			Feature p = fm.searchFeatureByName(e.getKey().getName());
			Feature c = fm.searchFeatureByName(e.getValue().getName());
			
			if (e.getValue() != null && e.getKey() != null && p != null && c != null) {
				RequiresDependency dep = new RequiresDependency("", p, c);
				fm.addDependency(dep);

			}

		}

	}

	private void generateExcludes() {
		for (Entry<Feature, Feature> e : excludesRel.entrySet()) {
			Feature p = fm.searchFeatureByName(e.getKey().getName());
			Feature c = fm.searchFeatureByName(e.getValue().getName());

			if (e.getValue() != null  
					&& e.getKey() != null 
					&& p != null 
					&& c != null 
					//&& !existExcludes(fm, p, c)
					) {
				ExcludesDependency dep = new ExcludesDependency("", p, c);
				fm.addDependency(dep);

			}

		}
	}

	private boolean existExcludes(FAMAFeatureModel fm2, Feature key, Feature value) {
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

	private void createOptional(Feature parent, Feature child) {
		es.us.isa.FAMA.models.FAMAfeatureModel.Relation rel = new es.us.isa.FAMA.models.FAMAfeatureModel.Relation("");
		rel.addCardinality(new Cardinality(0, 1));
		rel.addDestination(child);
		rel.setParent(parent);
		child.setParent(rel);
		parent.addRelation(rel);
	}

	private void createMandatory(Feature parent, Feature child) {
		es.us.isa.FAMA.models.FAMAfeatureModel.Relation rel = new es.us.isa.FAMA.models.FAMAfeatureModel.Relation("");
		rel.addCardinality(new Cardinality(1, 1));
		rel.addDestination(child);
		rel.setParent(parent);
		child.setParent(rel);
		parent.addRelation(rel);
	}

	private Feature getFeature(String name) {
		
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
	private Feature getFeature(DEBpackage pkg) {
		String name= pkg.getName() + "(" + pkg.getVersion().toString() + ")";
		return this.getFeature(name);
	}
	
	private boolean processed(DEBpackage pkg) {
		
		String name= pkg.getName() + "(" + pkg.getVersion().toString() + ")";
		boolean res = fm.searchFeatureByName(name)!=null;
		return res;
	}
	

	public void transform(DebianVariabilityModel dvm) {
		Feature root = new Feature("root");
		fm.setRoot(root);

		// Every package will be map to a VP
		for (DEBpackage pkg : dvm.getElements()) {
			// create a fake root and link all packages with it
			// if not processed
			if (!processed(pkg)) {
				Feature child = getFeature(pkg);
				createOptional(root, child);
				processDEB(pkg, child);
			}
		}
	}

	public void processDEB(DEBpackage pkg, Feature feat) {

		// System.out.println("Processing: " + pkg.getName() + "(" +
		// pkg.getVersion().toString() + ")");
		for (Relation rel : pkg.getRelations()) {
			Collection<DEBpackage> childs = rel.getChilds();

			// we will map this to a mandatory relationship
			if (rel instanceof DependsRalation) {
				for (DEBpackage deb : childs) {
					if (!processed(deb)) {
						// Aun no se ha añadido el paquete
						Feature child = getFeature(deb);
						createMandatory(feat, child);
						processDEB(deb, child);

					} else {
						// Existe el paquete
						Feature child = getFeature(deb);
						dependsRel.put(feat, child);

					}
				}
				// we can map this to a mandatory/requires relationship
			} else if (rel instanceof preDependsRelation) {
				for (DEBpackage deb : childs) {
					if (!processed(deb)) {
						Feature child = getFeature(deb);
						createMandatory(feat, child);
						processDEB(deb, child);

					} else {
						// Existe el paquete
						Feature child = getFeature(deb);
						dependsRel.put(feat, child);
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
					if (!processed(deb)) {
						// Aun no se ha añadido el paquete
						Feature child = getFeature(deb);
						createOptional(feat, child);
						processDEB(deb, child);

					}
				}

				// we will map this to a optional relationship
			} else if (rel instanceof SuggestsRelation) {
				for (DEBpackage deb : childs) {
					if (!processed(deb)) {
						// Aun no se ha añadido el paquete
						Feature child = getFeature(deb);
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
