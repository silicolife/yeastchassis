package com.silicolife.yeastchassis.env.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.silicolife.yeastchassis.env.AnalysisEnv;
import com.silicolife.yeastchassis.env.tasks.components.MongoDBLinks;
import com.silicolife.yeastchassis.env.tasks.components.Result;
import com.silicolife.yeastchassis.env.tasks.r.HeatMapRTaskFromFile;

import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.list.ListUtilities;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.indexedhashmap.IndexedHashMap;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.PairValueComparator;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.tuples.Triplet;
import pt.uminho.ceb.biosystems.mew.utilities.io.MultipleExtensionAndContainsFileFilter;
import pt.uminho.ceb.biosystems.mew.utilities.java.StringUtils;
import pt.uminho.ceb.biosystems.mew.utilities.math.MathUtils;

/**
 * 
 * @author pmaia
 */
public class ChassisTask extends AbstractAnalysisTask {
	
	public static final MultipleExtensionAndContainsFileFilter	INPUT_FILTER				= new MultipleExtensionAndContainsFileFilter(".csv");
	public static final DecimalFormat							formater					= new DecimalFormat("0.0000");
	public static final String									INPUT_RESULTS_PREFIX		= "results#";
	public static final String									OUTPUT_NAME					= "mostFrequent#";
	public static final String									OUTPUT_NAME_TABLE			= "mostFrequentTable#";
	public static final String									OUTPUT_NAME_TREE			= "mostFrequentTree#";
	public static final String									OUTPUT_NAME_HEATMAP			= "heatMap#";
	public static final String									OUTPUT_SUFFIX				= ".html";
	public static final String									OUTPUT_SUFFIX_TREE			= ".csv";
	public static final String									OUTPUT_SUFFIX_TABLE			= ".csv";
	public static final String									OUTPUT_SUFFIX_HEATMAP		= ".pdf";
	
	public static final String									ARROW_UP					= "\u2191";
	public static final String									ARROW_DOWN					= "\u2193";
	public static final String									DELTA						= "\u0394";
	
	public String												_output_base_url			= null;
	public int[]												objectiveFunctions			= null;
	public int													_target						= -1;
	public double												_percent					= 1.00;
	public List<String>											_varIgnore					= null;
	private int													_comb_min					= 2;
	private int													_comb_max					= 4;
	private Map<String, String>									_std2systematic				= null;
	private int													_attributeForScore			= 0;
	private boolean												_saveGeneFrequencyTable		= false;
	protected List<String>										contains					= null;
	private Map<Integer, Triplet<Double, Boolean, Boolean>>		_filters					= null;
	private int													_maxSolutionsPerVariable	= 10;
	private String												description;
	private boolean												useQualifiers				= false;
	
	public ChassisTask(String baseDir, AnalysisEnv env, int comb_min, int comb_max) throws Exception {
		super(baseDir, env);
		_comb_min = comb_min;
		_comb_max = comb_max;
	}
	
	public void setStandard2systematic(Map<String, String> map, boolean invert) {
		if (invert) {
			_std2systematic = new HashMap<String, String>();
			for (String s : map.keySet())
				_std2systematic.put(map.get(s), s);
		} else {
			_std2systematic = map;
		}
	}
	
	public void setStandard2systematic(String file, boolean invert) {
		setStandard2systematic(loadGIDS(file), invert);
	}
	
	public void setBaseUrl(String base) {
		_output_base_url = base;
	}
	
	public void setSaveGeneFrequencyTableAndHeatMap(boolean saveGeneFrequencyTable) {
		_saveGeneFrequencyTable = saveGeneFrequencyTable;
	}
	
	public void setVariableIgnore(String[] ignore) {
		if (ignore != null & ignore.length > 0) {
			_varIgnore = new ArrayList<String>();
			for (String i : ignore)
				_varIgnore.add(i);
		}
	}
	
	public void considerTopScoreSolutions(double percentageOfMax) {
		_percent = percentageOfMax;
	}
	
	public void setObjectiveFunctionForScoring(int objectiveFunctionIndex) {
		_attributeForScore = objectiveFunctionIndex;
	}
	
	public void addFilterAllowSolutions(Integer ofInded, Double value, Boolean percent, Boolean above) {
		
		Triplet<Double, Boolean, Boolean> triplet = new Triplet<Double, Boolean, Boolean>(value, percent, above);
		get_filters().put(ofInded, triplet);
	}
	
	public void setObjectiveFunctionIndexes(int[] indexes) {
		this.objectiveFunctions = indexes;
	}
	
	public void setMaxSolutionsPerVariable(int maxSolutionsPerVariable) {
		this._maxSolutionsPerVariable = maxSolutionsPerVariable;
	}
	
	public void setContains(String... contains) {
		this.contains = new ArrayList<String>();
		for (String cont : contains) {
			this.contains.add(cont);
		}
		INPUT_FILTER.addContains(this.contains);
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public void setUseQualifiers(boolean useQualifiers) {
		this.useQualifiers = useQualifiers;
	}
	
	public void execute() throws Exception {
		if (useQualifiers) {
			executeAggregationOnDescendantsQualifiers();
		} else {
			executeAggregationOnDescendants();
		}
	}
	
	private List<String> buildHTMLHref(List<String> elements) {
		List<String> toret = new ArrayList<String>();
		for (String e : elements) {
			String link = _output_base_url + _std2systematic.get(e);
			String hrefElem = "<a target=\"_blank\" href=\"" + link + "\">" + _std2systematic.get(e) + "</a>" + "<sup>" + e + "</sup>";
			toret.add(hrefElem);
		}
		return toret;
	}
	
	public int getObjectiveFunctionIndex(int ofIndex) {
		int scoreAttributeIndex = 0;
		for (int i = 0; i < objectiveFunctions.length; i++) {
			if (objectiveFunctions[i] == ofIndex) {
				scoreAttributeIndex = i;
			}
		}
		return scoreAttributeIndex;
	}
	
	public void executeAggregationOnDescendants() throws Exception {
		Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> configurationMap = getConfigurationMapResultsOnVariable();
		
		String mappingsString = (mappings == null || mappings.isEmpty()) ? "" : "#[" + StringUtils.concat(",", mappings) + "]";
		String containsString = (contains == null || contains.isEmpty()) ? "" : "#[" + StringUtils.concat(",", contains) + "]";
		System.out.println("MAPPINGS_STRING:"+mappingsString);
		System.out.println("CONTAINS_STRING:"+containsString);
		
		List<Map<String, String>> converts = new ArrayList<>();
		for (Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>> conv : configurationMap.values()) {
			for (Pair<JecoliGenericConfiguration, Map<String, String>> pair : conv.values()) {
				converts.add(pair.getB());
			}
		}
		
		String consensusName = "[" + _env.generateBranchBaseIntersect(converts, ",") + "]";
		System.out.println("CONSENSUS:"+consensusName);
		
		String kmax = "K=" + ((_comb_min == _comb_max) ? _comb_max : (_comb_min + "-" + _comb_max));
		String top = "Top=" + _percent + "pct";
		List<String> ignoreFilters = new ArrayList<String>();
		
		for (Integer filt : get_filters().keySet()) {
			Triplet<Double, Boolean, Boolean> triple = get_filters().get(filt);
			String f = filt + ":" + (triple.getC() ? ">=" : "<=") + triple.getA() + (triple.getB() ? "pct" : "");
			ignoreFilters.add(f);
		}
		String ignore = "Filter=[" + StringUtils.concat(",", ignoreFilters) + "]";
		
		String output_file = _directory + "/" + OUTPUT_NAME + kmax + "#" + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX;
		String output_table = _directory + "/" + OUTPUT_NAME_TABLE + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX_TABLE;
		String output_heatmap = _directory + "/" + OUTPUT_NAME_HEATMAP + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX_HEATMAP;
		String output_tree = _directory + "/" + OUTPUT_NAME_TREE + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX_TREE;
		
		Map<String, Result> results = new HashMap<String, Result>();
		List<String> consideredStates = new ArrayList<String>();
		Map<Integer, String> ofStrings = new HashMap<>();
		for (String subDir : configurationMap.keySet()) {
			System.out.println("SUBDIR:"+subDir);
			Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>> subMap = configurationMap.get(subDir);
			
			String var = subMap.values().iterator().next().getB().get(mappingVar);
			
			if (_varIgnore == null || !_varIgnore.contains(var)) {
				consideredStates.add(var);
				String[] files = loadFiles(subDir);
				String resultsFile = null;
				for (String f : files) {
					if (f.contains(INPUT_RESULTS_PREFIX))
						resultsFile = f;
				}
				
				if (resultsFile == null)
					throw new Exception("Results file must be present in directory [" + subDir + "]");
				
				Result res = new Result();
				res.loadResultsFromFileMergeQualifiers(resultsFile, true);
				System.out.println("[" + getClass().getSimpleName() + "] reading file [" + resultsFile + "]");
				
				if (objectiveFunctions != null) {
					int[] intArray = ListUtilities.getIntegerRangeArray(0, res.size() - 1, 1);
					if (ofStrings.isEmpty()) {
						for (int ofIdx : objectiveFunctions) {
							ofStrings.put(ofIdx, res.get_header().get(ofIdx));
						}
					}
					res = res.subset(intArray, objectiveFunctions);
				}
				
				for (int key : get_filters().keySet()) {
					
					int ofIndex = getObjectiveFunctionIndex(key);
					Triplet<Double, Boolean, Boolean> filter = get_filters().get(key);
					int[] filtSolutions = res.solutionsWithFitness(ofIndex, filter.getA(), filter.getB(), filter.getC());
					
					if (filtSolutions.length > 0) {
						res = res.subset(filtSolutions);
					} else {
						res = new Result();
					}
				}
				
				results.put(var, res);
			}
		}
		
		Set<String> setEntries = new HashSet<String>();
		List<String> listEntries = new ArrayList<String>();
		for (String v : results.keySet()) {
			for (List<String> ls : results.get(v).get_solutions()) {
				setEntries.addAll(ls);
			}
		}
		listEntries.addAll(setEntries);
		
		double[][] freqs = new double[listEntries.size()][consideredStates.size()];
		
		for (int i = 0; i < consideredStates.size(); i++) {
			String var = consideredStates.get(i);
			int size = results.get(var).size();
			for (int j = 0; j < listEntries.size(); j++) {
				int ncontains = 0;
				for (List<String> sol : results.get(var).get_solutions()) {
					if (sol.contains(listEntries.get(j)))
						ncontains++;
				}
				freqs[j][i] = ((double) ncontains) / ((double) size);
			}
		}
		
		List<Pair<String, Double>> scores = new ArrayList<Pair<String, Double>>();
		
		Map<String, double[]> freqByGene = null;
		if (_saveGeneFrequencyTable)
			freqByGene = new HashMap<String, double[]>();
		
		for (int i = 0; i < freqs.length; i++) {
			double sum = MathUtils.sumDoubleArray(freqs[i]);
			int divisor = 0;
			for (int j = 0; j < freqs[0].length; j++) {
				if (freqs[i][j] > 0)
					divisor++;
			}
			double score = sum * ((double) divisor);
			
			Pair<String, Double> pair = new Pair<String, Double>(listEntries.get(i), score);
			scores.add(i, pair);
			
			if (_saveGeneFrequencyTable)
				freqByGene.put(listEntries.get(i), freqs[i]);
		}
		
		Collections.sort(scores, new PairValueComparator<Double>());
		
		int limit = (int) Math.floor(_percent * scores.size());
		
		String[] initialSet = new String[limit];
		
		BufferedWriter bwGF = null;
		if (_saveGeneFrequencyTable) {
			bwGF = new BufferedWriter(new FileWriter(output_table));
			String header = "GeneID,Score";
			for (int i = 0; i < consideredStates.size(); i++)
				header += ("," + consideredStates.get(i));
			bwGF.append(header);
		}
		
		int ind = 0;
		for (int k = scores.size() - 1; k > scores.size() - limit - 1; k--) {
			initialSet[ind] = scores.get(k).getA();
			
			if (_saveGeneFrequencyTable) {
				bwGF.newLine();
				String geneID = MongoDBLinks.getSystematicToSTDWithQualifiers(scores.get(k).getA());
//				String geneID = (_std2systematic != null) ? _std2systematic.get(scores.get(k).getA()) : scores.get(k).getA();
				bwGF.append(geneID + "," + scores.get(k).getB() + "," + StringUtils.concat(",", freqByGene.get(scores.get(k).getA())));
			}
			ind++;
		}
		
		if (_saveGeneFrequencyTable) {
			bwGF.flush();
			bwGF.close();
		}
		
		if (_saveGeneFrequencyTable) {
			HeatMapRTaskFromFile heatTask = new HeatMapRTaskFromFile(_directory, _env, output_table);
			heatTask.setOutputName(output_heatmap);
			try {
				heatTask.run();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("INITIAL SET [" + limit + "] = " + Arrays.toString(initialSet));
		
		ICombinatoricsVector<String> initialVector = Factory.createVector(initialSet);
		
		IndexedHashMap<String, Map<String, Result>> resmap = new IndexedHashMap<String, Map<String, Result>>();
		List<Pair<String, Double>> combsScore = new ArrayList<Pair<String, Double>>();
		
		int scoreAttributeIndex = 0;
		for (int i = 0; i < objectiveFunctions.length; i++) {
			if (objectiveFunctions[i] == _attributeForScore) {
				scoreAttributeIndex = i;
			}
		}
		
		System.out.println("[" + getClass().getSimpleName() + "] objective function for scoring = [" + objectiveFunctions[scoreAttributeIndex] + "]");
		
		for (int n = _comb_min; n <= _comb_max; n++) {
			
			Generator<String> gen = Factory.createSimpleCombinationGenerator(initialVector, n);
			
			// Print all possible combinations
			int count = 0;
			for (ICombinatoricsVector<String> combination : gen) {
				if (count % 1000 == 0)
					System.out.println("[" + count + "]: " + combination);
				
				List<String> comb = new ArrayList<String>();
				Iterator<String> it = combination.iterator();
				while (it.hasNext())
					comb.add(it.next());
				String combString = StringUtils.concat(",", comb);
				
				double sumMaxVar = 0.0;
				
				Map<String, Result> aux = new HashMap<String, Result>();
				for (String var : results.keySet()) {
					Result res = results.get(var);
					Result subset = res.subset(res.solutionsWithElements(comb));
					
					if (subset.size() == 0) {
//						res = new Result();
//						continue;
					} else {
						int smaller = subset.smallerSolution();
						sumMaxVar += (subset.max(scoreAttributeIndex) / subset.solutionSize(smaller));
						
						aux.put(var, subset);
					}
					
				}
				if (aux.size() >= 2) {
					if (!resmap.containsKey(combString))
						resmap.put(combString, new IndexedHashMap<String, Result>());
					
					for (String var : aux.keySet())
						resmap.get(combString).put(var, aux.get(var));
					
					if (sumMaxVar > 0.0)
						combsScore.add(new Pair<String, Double>(combString, sumMaxVar));
				}
				
				count++;
			}
		}
		
		Collections.sort(combsScore, Collections.reverseOrder(new PairValueComparator<Double>()));
		
//		MongoChassisPersister chassisPersister = new MongoChassisPersister(resmap, combsScore, description, _filters, contains, mappings, _percent, _attributeForScore, _comb_min, _comb_max, ofStrings,
//				output_heatmap, output_table);
//		chassisPersister.persist();
		
		/**
		 * HTML FILE
		 */
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(output_file));
		
		bw2.append("<!DOCTYPE html>\n" + "<html>\n");
		bw2.append("<style>\n" + "sup {\n" + "font-size: 0.5em;\n" + "vertical-align: super;\n" + "line-height: 1.5em;\n" + "}\n" + "a {\n" + "font-size: 0.8em;\n" + "}\n" + "li{\n"
				+ "line-height: 1.5em;\n" + "}\n" + "</style>\n");
		bw2.append("<body>\n");
		bw2.append("<h1>" + OUTPUT_NAME + kmax + "#" + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + "</h1>\n");
		
		bw2.append("<ul>\n");
		for (Pair<String, Double> p : combsScore)
		
		{
			String comb = p.getA();
			List<String> combList = new ArrayList<String>();
			for (String s : comb.split(","))
				combList.add(s);
			Collections.sort(combList);
			
			bw2.append("<hr>\n");
			bw2.append("<li>" + StringUtils.concat(" ", buildHTMLHref(combList)) + "</li>\n");
			bw2.append("\t<ul>\n");
			for (String var : resmap.get(comb).keySet()) {
				
				bw2.append("\t\t<li>" + var + "</li>\n");
				bw2.append("\t\t<ul>\n");
				Result res = resmap.get(comb).get(var);
				List<Pair<Integer, Double>> pairs = new ArrayList<Pair<Integer, Double>>();
				for (int i = 0; i < res.size(); i++) {
					List<String> sol = res.get_solutions().get(i);
					sol.removeAll(combList);
					Collections.sort(sol);
					pairs.add(new Pair<Integer, Double>(i, (double) sol.size()));
				}
				
				Collections.sort(pairs, new PairValueComparator<Double>());
				
				int max = Math.min(res.size(), _maxSolutionsPerVariable);
				for (int i = 0; i < max; i++) {
					List<String> sol = res.get_solutions().get(pairs.get(i).getA());
					sol.removeAll(combList);
					Collections.sort(sol);
					List<Double> atts = res.get_attributes().get(pairs.get(i).getA());
					String attribString = "";
					for (int a = 0; a < atts.size(); a++) {
						attribString += res.get_header().get(a) + "=" + formater.format(atts.get(a));
						if (a < atts.size() - 1)
							attribString += ", ";
					}
					bw2.append("\t\t\t<li>" + StringUtils.concat(" ", buildHTMLHref(sol)) + "\t\t[" + attribString + "]</li>\n");
				}
				bw2.append("\t\t</ul>\n");
			}
			bw2.append("\t</ul>\n");
			bw2.newLine();
		}
		bw2.append("</ul>\n");
		bw2.append("</body>\n" + "</html>");
		
		bw2.flush();
		bw2.close();
		
		/**
		 * TREE FILE
		 */
		BufferedWriter bw = new BufferedWriter(new FileWriter(output_tree));

		for (Pair<String, Double> p : combsScore)

		{
			String comb = p.getA();
			List<String> combList = new ArrayList<String>();
			for (String s : comb.split(",")) {				
				combList.add(_std2systematic.get(s));
			}
			Collections.sort(combList);

			bw.append(StringUtils.concat(",", combList));
			for (String var : resmap.get(comb).keySet()) {
				bw.newLine();
				bw.append("\t" + var + ":");
				Result res = resmap.get(comb).get(var);
				List<Pair<Integer, Double>> pairs = new ArrayList<Pair<Integer, Double>>();
				for (int i = 0; i < res.size(); i++) {
					// bw.newLine();
					List<String> sol = res.get_solutions().get(i);
					sol = sol.stream().map(x->_std2systematic.get(x)).collect(Collectors.toList());
					sol.removeAll(combList);
					Collections.sort(sol);
					pairs.add(new Pair<Integer, Double>(i, (double) sol.size()));
				}

				Collections.sort(pairs, new PairValueComparator<Double>());

				int max = Math.min(res.size(), _maxSolutionsPerVariable);
				for (int i = 0; i < max; i++) {
					List<String> sol = res.get_solutions().get(pairs.get(i).getA());
					sol = sol.stream().map(x->_std2systematic.get(x)).collect(Collectors.toList());
					sol.removeAll(combList);
					Collections.sort(sol);
					bw.newLine();
					bw.append("\t\t" + sol.size() + "\t"+ res.get_attributes().get(pairs.get(i).getA()).get(1));
				}
			}
			bw.newLine();
		}

		bw.flush();
		bw.close();

	}
	
	public void executeAggregationOnDescendantsQualifiers() throws Exception {
		Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> configurationMap = getConfigurationMapResultsOnVariable();
		
		String mappingsString = (mappings == null || mappings.isEmpty()) ? "" : "#[" + StringUtils.concat(",", mappings) + "]";
		String containsString = (contains == null || contains.isEmpty()) ? "" : "#[" + StringUtils.concat(",", contains) + "]";
		
		List<Map<String, String>> converts = new ArrayList<>();
		for (Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>> conv : configurationMap.values()) {
			for (Pair<JecoliGenericConfiguration, Map<String, String>> pair : conv.values()) {
				converts.add(pair.getB());
			}
		}
		
		String consensusName = "[" + _env.generateBranchBaseIntersect(converts, ",") + "]";
		
		String kmax = "K=" + ((_comb_min == _comb_max) ? _comb_max : (_comb_min + "-" + _comb_max));
		String top = "Top=" + _percent + "pct";
		List<String> ignoreFilters = new ArrayList<String>();
		
		for (Integer filt : get_filters().keySet()) {
			Triplet<Double, Boolean, Boolean> triple = get_filters().get(filt);
			String f = filt + ":" + (triple.getC() ? ">=" : "<=") + triple.getA() + (triple.getB() ? "pct" : "");
			ignoreFilters.add(f);
		}
		String ignore = "Filter=[" + StringUtils.concat(",", ignoreFilters) + "]";
		
		String output_file = _directory + "/" + OUTPUT_NAME + kmax + "#" + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX;
		String output_table = _directory + "/" + OUTPUT_NAME_TABLE + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX_TABLE;
		String output_heatmap = _directory + "/" + OUTPUT_NAME_HEATMAP + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + OUTPUT_SUFFIX_HEATMAP;
		
		Map<String, Result> results = new HashMap<String, Result>();
		List<String> consideredStates = new ArrayList<String>();
		Map<Integer, String> ofStrings = new HashMap<>();
		for (String subDir : configurationMap.keySet()) {
			
			Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>> subMap = configurationMap.get(subDir);
			
			String var = subMap.values().iterator().next().getB().get(mappingVar);
			
			if (_varIgnore == null || !_varIgnore.contains(var)) {
				consideredStates.add(var);
				String[] files = loadFiles(subDir);
				String resultsFile = null;
				for (String f : files) {
					if (f.contains(INPUT_RESULTS_PREFIX))
						resultsFile = f;
				}
				
				if (resultsFile == null)
					throw new Exception("Results file must be present in directory [" + subDir + "]");
				
				Result res = new Result();
				res.loadResultsFromFile(resultsFile, true);
				System.out.println("[" + getClass().getSimpleName() + "] reading file [" + resultsFile + "]");
				
				if (objectiveFunctions != null) {
					int[] intArray = ListUtilities.getIntegerRangeArray(0, res.size() - 1, 1);
					if (ofStrings.isEmpty()) {
						for (int ofIdx : objectiveFunctions) {
							ofStrings.put(ofIdx, res.get_header().get(ofIdx));
						}
					}
					res = res.subset(intArray, objectiveFunctions);
				}
				
				for (int key : get_filters().keySet()) {
					
					int ofIndex = getObjectiveFunctionIndex(key);
					Triplet<Double, Boolean, Boolean> filter = get_filters().get(key);
					int[] filtSolutions = res.solutionsWithFitness(ofIndex, filter.getA(), filter.getB(), filter.getC());
					
					if (filtSolutions.length > 0) {
						res = res.subset(filtSolutions);
					} else {
						res = new Result();
					}
				}
				
				results.put(var, res);
			}
		}
		
		Set<String> setEntries = new HashSet<String>();
		List<String> listEntries = new ArrayList<String>();
		for (String v : results.keySet()) {
			List<List<String>> lss = results.get(v).get_solutions();
			List<List<Double>> qualss = results.get(v).get_solutionQualifiers();
			for (int i = 0; i < lss.size(); i++) {
				List<String> ls = lss.get(i);
				List<Double> quals = qualss.get(i);
				setEntries.addAll(appendQualifiers(ls, quals));
			}
		}
		listEntries.addAll(setEntries);
		
		double[][] freqs = new double[listEntries.size()][consideredStates.size()];
		
		for (int i = 0; i < consideredStates.size(); i++) {
			String var = consideredStates.get(i);
			int size = results.get(var).size();
			for (int j = 0; j < listEntries.size(); j++) {
				int ncontains = 0;
				for (List<String> sol : listOfSolutionWithQualifiers(results.get(var))) {
					if (sol.contains(listEntries.get(j)))
						ncontains++;
				}
				freqs[j][i] = ((double) ncontains) / ((double) size);
			}
		}
		
		List<Pair<String, Double>> scores = new ArrayList<Pair<String, Double>>();
		
		Map<String, double[]> freqByGene = null;
		if (_saveGeneFrequencyTable)
			freqByGene = new HashMap<String, double[]>();
		
		for (int i = 0; i < freqs.length; i++) {
			double sum = MathUtils.sumDoubleArray(freqs[i]);
			int divisor = 0;
			for (int j = 0; j < freqs[0].length; j++) {
				if (freqs[i][j] > 0)
					divisor++;
			}
			double score = sum * ((double) divisor);
			
			Pair<String, Double> pair = new Pair<String, Double>(listEntries.get(i), score);
			scores.add(i, pair);
			
			if (_saveGeneFrequencyTable)
				freqByGene.put(listEntries.get(i), freqs[i]);
		}
		
		Collections.sort(scores, new PairValueComparator<Double>());
		
		int limit = (int) Math.floor(_percent * scores.size());
		
		String[] initialSet = new String[limit];
		
		BufferedWriter bwGF = null;
		if (_saveGeneFrequencyTable) {
			bwGF = new BufferedWriter(new FileWriter(output_table));
			String header = "GeneID,Score";
			for (int i = 0; i < consideredStates.size(); i++)
				header += ("," + consideredStates.get(i));
			bwGF.append(header);
		}
		
		int ind = 0;
		for (int k = scores.size() - 1; k > scores.size() - limit - 1; k--) {
			initialSet[ind] = scores.get(k).getA();
			
			if (_saveGeneFrequencyTable) {
				bwGF.newLine();
				String[] geneTokens = scores.get(k).getA().split("#");
				String qual = geneTokens[0];
				String gene = geneTokens[1];
				String geneID = (_std2systematic != null) ? _std2systematic.get(gene) : gene;
				geneID = qual+"#"+geneID;
				bwGF.append(geneID + "," + scores.get(k).getB() + "," + StringUtils.concat(",", freqByGene.get(scores.get(k).getA())));
			}
			ind++;
		}
		
		if (_saveGeneFrequencyTable) {
			bwGF.flush();
			bwGF.close();
		}
		
		if (_saveGeneFrequencyTable) {
			HeatMapRTaskFromFile heatTask = new HeatMapRTaskFromFile(_directory, _env, output_table);
			heatTask.setOutputName(output_heatmap);
			try {
				heatTask.run();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("INITIAL SET [" + limit + "] = " + Arrays.toString(initialSet));
		
		ICombinatoricsVector<String> initialVector = Factory.createVector(initialSet);
		
		IndexedHashMap<String, Map<String, Result>> resmap = new IndexedHashMap<String, Map<String, Result>>();
		List<Pair<String, Double>> combsScore = new ArrayList<Pair<String, Double>>();
		
		int scoreAttributeIndex = 0;
		for (int i = 0; i < objectiveFunctions.length; i++) {
			if (objectiveFunctions[i] == _attributeForScore) {
				scoreAttributeIndex = i;
			}
		}
		
		System.out.println("[" + getClass().getSimpleName() + "] objective function for scoring = [" + objectiveFunctions[scoreAttributeIndex] + "]");
		
		for (int n = _comb_min; n <= _comb_max; n++) {
			
			Generator<String> gen = Factory.createSimpleCombinationGenerator(initialVector, n);
			
			// Print all possible combinations
			int count = 0;
			for (ICombinatoricsVector<String> combination : gen) {
				if (count % 1000 == 0)
					System.out.println("[" + count + "]: " + combination);
				
				List<String> comb = new ArrayList<String>();
				
				Pair<List<String>,List<String>> combsAndQuals = processCombsAndQuals(comb);
				
				Iterator<String> it = combination.iterator();
				while (it.hasNext())
					comb.add(it.next());
				String combString = StringUtils.concat(",", comb);
				
				double sumMaxVar = 0.0;
				
				Map<String, Result> aux = new HashMap<String, Result>();
				for (String var : results.keySet()) {
					Result res = results.get(var);
					Result subset = res.subset(res.solutionsWithElementsAndQualifiers(combsAndQuals.getA(),combsAndQuals.getB()));
					
					if (subset.size() == 0) {
//						res = new Result();
//						continue;
					} else {
						int smaller = subset.smallerSolution();
						sumMaxVar += (subset.max(scoreAttributeIndex) / subset.solutionSize(smaller));
						
						aux.put(var, subset);
					}
					
				}
				if (aux.size() >= 2) {
					if (!resmap.containsKey(combString))
						resmap.put(combString, new IndexedHashMap<String, Result>());
					
					for (String var : aux.keySet())
						resmap.get(combString).put(var, aux.get(var));
					
					if (sumMaxVar > 0.0)
						combsScore.add(new Pair<String, Double>(combString, sumMaxVar));
				}
				
				count++;
			}
		}
		
		Collections.sort(combsScore, Collections.reverseOrder(new PairValueComparator<Double>()));
		
//		MongoChassisPersister chassisPersister = new MongoChassisPersister(resmap, combsScore, description, _filters, contains, mappings, _percent, _attributeForScore, _comb_min, _comb_max, ofStrings,
//				output_heatmap, output_table);
//		chassisPersister.persist();
		
		/**
		 * HTML FILE
		 */
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(output_file));
		
		bw2.append("<!DOCTYPE html>\n" + "<html>\n");
		bw2.append("<style>\n" + "sup {\n" + "font-size: 0.5em;\n" + "vertical-align: super;\n" + "line-height: 1.5em;\n" + "}\n" + "a {\n" + "font-size: 0.8em;\n" + "}\n" + "li{\n"
				+ "line-height: 1.5em;\n" + "}\n" + "</style>\n");
		bw2.append("<body>\n");
		bw2.append("<h1>" + OUTPUT_NAME + kmax + "#" + top + "#" + ignore + "#" + consensusName + mappingsString + containsString + "</h1>\n");
		
		bw2.append("<ul>\n");
		for (Pair<String, Double> p : combsScore)
		
		{
			String comb = p.getA();
			List<String> combList = new ArrayList<String>();
			for (String s : comb.split(","))
				combList.add(s);
			Collections.sort(combList);
			
			bw2.append("<hr>\n");
			bw2.append("<li>" + StringUtils.concat(" ", buildHTMLHref(combList)) + "</li>\n");
			bw2.append("\t<ul>\n");
			for (String var : resmap.get(comb).keySet()) {
				
				bw2.append("\t\t<li>" + var + "</li>\n");
				bw2.append("\t\t<ul>\n");
				Result res = resmap.get(comb).get(var);
				List<Pair<Integer, Double>> pairs = new ArrayList<Pair<Integer, Double>>();
				for (int i = 0; i < res.size(); i++) {
					List<String> sol = res.get_solutions().get(i);
					sol.removeAll(combList);
					Collections.sort(sol);
					pairs.add(new Pair<Integer, Double>(i, (double) sol.size()));
				}
				
				Collections.sort(pairs, new PairValueComparator<Double>());
				
				int max = Math.min(res.size(), _maxSolutionsPerVariable);
				for (int i = 0; i < max; i++) {
					List<String> sol = res.get_solutions().get(pairs.get(i).getA());
					sol.removeAll(combList);
					Collections.sort(sol);
					List<Double> atts = res.get_attributes().get(pairs.get(i).getA());
					String attribString = "";
					for (int a = 0; a < atts.size(); a++) {
						attribString += res.get_header().get(a) + "=" + formater.format(atts.get(a));
						if (a < atts.size() - 1)
							attribString += ", ";
					}
					bw2.append("\t\t\t<li>" + StringUtils.concat(" ", buildHTMLHref(sol)) + "\t\t[" + attribString + "]</li>\n");
				}
				bw2.append("\t\t</ul>\n");
				//					bw2.append("<hr>\n");
			}
			bw2.append("\t</ul>\n");
			bw2.newLine();
		}
		bw2.append("</ul>\n");
		bw2.append("</body>\n" + "</html>");
		
		bw2.flush();
		bw2.close();
		
	}
	
	private Pair<List<String>, List<String>> processCombsAndQuals(List<String> comb) {
		List<String> combs = new ArrayList<String>(comb.size());
		List<String> quals = new ArrayList<String>(comb.size());
		
		for(int i=0; i<comb.size(); i++){
			String[] tks = comb.get(i).split("#");
			combs.add(i, tks[0].trim());
			quals.add(i, tks[1].trim());
		}
		
		return new Pair<>(combs,quals);
	}

	private List<String> appendQualifiers(List<String> ls, List<Double> quals) {
		List<String> toret = new ArrayList<String>();
		for (int i = 0; i < ls.size(); i++) {
			Double val = quals.get(i);
			String qual = val==0.0 ? "K" : val > 1.0 ? "O" : "U";
			toret.add(qual + "#" + ls.get(i));
		}
		return toret;
	}
	
	private List<List<String>> listOfSolutionWithQualifiers(Result res) {
		List<List<String>> toret = new ArrayList<List<String>>();
		for (int i = 0; i < res.size(); i++) {
			List<String> sols = res.get_solutions().get(i);
			List<Double> quals = res.get_solutionQualifiers().get(i);
			toret.add(appendQualifiers(sols, quals));
		}
		return toret;
	}
	
	public String[] loadFiles() {
		return loadFiles(_directory);
	}
	
	public String[] loadFiles(String path) {
		File[] files = new File(path).listFiles(INPUT_FILTER);
		String[] stringFiles = new String[files.length];
		
		for (int i = 0; i < files.length; i++)
			stringFiles[i] = files[i].getAbsolutePath();
		
		return stringFiles;
	}
	
	public FileFilter getInputFileFilter() {
		return INPUT_FILTER;
	}
	
	private Map<String, String> loadGIDS(String gIDS) {
		Map<String, String> toret = new HashMap<String, String>();
		try {
			BufferedReader br = null;
			br = new BufferedReader(new FileReader(gIDS));
			
			while (br.ready()) {
				String line = br.readLine();
				String[] tokens = line.split("\t");
				if (tokens.length == 2) {
					String tk2 = tokens[1].trim();
					if (tk2.isEmpty())
						toret.put(tokens[0].trim(), tokens[0].trim());
					else
						toret.put(tokens[0].trim(), tokens[1].trim());
				} else
					toret.put(tokens[0].trim(), tokens[0].trim());
				
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return toret;
	}
	
	public Map<Integer, Triplet<Double, Boolean, Boolean>> get_filters() {
		if (_filters == null) {
			_filters = new HashMap<>();
		}
		return _filters;
	}
}