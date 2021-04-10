package com.silicolife.yeastchassis.env.tasks.components;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.jecoli.algorithm.multiobjective.MOUtils;
import pt.uminho.ceb.biosystems.mew.utilities.java.StringUtils;

public class Result {
	
	public static final String		ID					= "ID";
	public static final String		DELIMITER			= ",";
	public static final String		SOLUTION_DELIMITER	= " ";
	public static final int			DEFAULT_HEAD		= 6;
	public static final int			DEFAULT_TAIL		= 6;
	public static final double		FITNESS_THRESHOLD	= 1e-4;
	
	protected List<String>			_header				= null;
	protected List<String>			_solutionIDs		= null;
	protected List<List<Double>>	_attributes			= null;
	protected List<List<String>>	_solutions			= null;
	protected List<List<Double>>	_solutionQualifiers	= null;
	
	public Result() {
	}
	
	public Result(List<String> header, List<String> solutionIDs, List<List<String>> solutions, List<List<Double>> attributes, List<List<Double>> solutionQualifiers) {
		_header = header;
		_solutionIDs = solutionIDs;
		_solutions = solutions;
		_attributes = attributes;
		_solutionQualifiers = solutionQualifiers;		
	}
	
	public Result(List<String> header, List<String> solutionIDs, List<List<String>> solutions, List<List<Double>> attributes) {
		this(header,solutionIDs, solutions, attributes, null);
	}
	
	public void saveResultsToFile(String file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		
		/**
		 * WRITE HEADER
		 */
		String header = ID + DELIMITER + StringUtils.concat(DELIMITER, get_header());
		bw.append(header);
		
		/**
		 * WRITE SOLUTIONS
		 */
		for (int i = 0; i < size(); i++) {
			String id = get_solutionIDs().get(i);
			String atts = StringUtils.concat(DELIMITER, get_attributes().get(i));
			String solution = (_solutionQualifiers!=null) ? StringUtils.concat(SOLUTION_DELIMITER, processSolutionQualifiers(i)) : StringUtils.concat(SOLUTION_DELIMITER, get_solutions().get(i));			
			
			String line = id + DELIMITER + atts + DELIMITER + solution;
			bw.newLine();
			bw.append(line);
		}
		
		bw.flush();
		bw.close();
	}
	
	private List<String> processSolutionQualifiers(int solIndex) {
		List<String> solution = get_solutions().get(solIndex);
		List<Double> qualifier = get_solutionQualifiers().get(solIndex);
		
		List<String> processed = new ArrayList<String>();
		for(int i=0; i<solution.size(); i++){
			processed.add(solution.get(i)+"="+qualifier.get(i));
		}
		return processed;
	}
	
	public void loadResultsFromFile(String file, boolean solutionsWithQualifiers) throws IOException {
		loadResultsFromFile(file, true, true);
	}

	public void loadResultsFromFile(String file) throws IOException {
		loadResultsFromFile(file, true, false);
	}
	
	public void loadResultsFromFile(String file, boolean containsSolutions, boolean containsQualifiers) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		/***
		 * READ HEADER
		 */
		String[] header = br.readLine().split(DELIMITER);
		String[] cleanHeader = Arrays.copyOfRange(header, 1, header.length);
		
		for (int i = 0; i < cleanHeader.length; i++)
			get_header().add(i, cleanHeader[i]);
		
		/**
		 * READ SOLUTIONS
		 */
		int solcount = 0;
		while (br.ready()) {
			String line = br.readLine();
			
			String[] tokens = line.split(DELIMITER);
			int attribslength = (containsSolutions) ? tokens.length - 1 : tokens.length;
			
			/**
			 * solution ID
			 */
			get_solutionIDs().add(solcount, tokens[0].trim());
			
			/**
			 * attributes
			 */
			ArrayList<Double> attribs = new ArrayList<Double>();
			for (int i = 1; i < attribslength; i++)
				attribs.add(Double.parseDouble(tokens[i].trim()));
			
			get_attributes().add(solcount, attribs);
			
			/**
			 * solution
			 */
			if (containsSolutions) {
				String[] soltokens = tokens[tokens.length - 1].split("\\s");
				
				ArrayList<String> sol = new ArrayList<String>();
				ArrayList<Double> qual = null;
				if(containsQualifiers){ qual = new ArrayList<Double>();	}
				
				for (int j = 0; j < soltokens.length; j++){
					if(containsQualifiers){
						String[] elemTokens = soltokens[j].split("=");
						if(elemTokens.length>=2){
							String ele = elemTokens[0].trim();
							Double q = Double.parseDouble(elemTokens[1].trim());
							sol.add(j,ele);
							qual.add(j,q);
						}else{
							br.close();
							throw new IOException("Option [containsQualifiers] was set to [true] but solution ["+soltokens[j]+"] does not possess a qualifier.");
						}
					}else{
						sol.add(j, soltokens[j].trim());						
					}
				}				
				get_solutions().add(sol);
				if(containsQualifiers){
					get_solutionQualifiers().add(qual);
				}
			}
			
			solcount++;
		}
		
		br.close();
	}
	
	public void loadResultsFromFileMergeQualifiers(String file, boolean containsSolutions) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		/***
		 * READ HEADER
		 */
		String[] header = br.readLine().split(DELIMITER);
		String[] cleanHeader = Arrays.copyOfRange(header, 1, header.length);
		
		for (int i = 0; i < cleanHeader.length; i++)
			get_header().add(i, cleanHeader[i]);
		
		/**
		 * READ SOLUTIONS
		 */
		int solcount = 0;
		while (br.ready()) {
			String line = br.readLine();
			
			String[] tokens = line.split(DELIMITER);
			int attribslength = (containsSolutions) ? tokens.length - 1 : tokens.length;
			
			/**
			 * solution ID
			 */
			get_solutionIDs().add(solcount, tokens[0].trim());
			
			/**
			 * attributes
			 */
			ArrayList<Double> attribs = new ArrayList<Double>();
			for (int i = 1; i < attribslength; i++)
				attribs.add(Double.parseDouble(tokens[i].trim()));
			
			get_attributes().add(solcount, attribs);
			
			/**
			 * solution
			 */
			if (containsSolutions) {
				String[] soltokens = tokens[tokens.length - 1].split("\\s");
				
				ArrayList<String> sol = new ArrayList<String>();				
				
				for (int j = 0; j < soltokens.length; j++){
					String[] elemTokens = soltokens[j].split("=");
					if(elemTokens.length>=2){
						String ele = elemTokens[0].trim();
						Double q = Double.parseDouble(elemTokens[1].trim());
						String qual = q==0.0 ? "K" : q>1.0 ? "O" : "U";
						sol.add(j,qual+"#"+ele);						
					}
					else{
						sol.add(j, soltokens[j].trim());						
					}
				}				
				get_solutions().add(sol);				
			}
			
			solcount++;
		}
		
		br.close();
	}
	
	public String head() {
		return head(DEFAULT_HEAD, true);
	}
	
	public String head(boolean includeHeader) {
		return head(DEFAULT_HEAD, includeHeader);
	}
	
	public String head(int numlines, boolean includeHeader) {
		
		StringBuilder sb = new StringBuilder();
		
		int limit = Math.min(numlines, size());
		if (limit > 0) {
			if (includeHeader)
				sb.append(ID + "\t" + StringUtils.concat("\t", get_header()));
			for (int i = 0; i < limit; i++) {
				if (includeHeader || i > 0)
					sb.append("\n");
				sb.append(get_solutionIDs().get(i) + "\t" + StringUtils.concat("\t", get_attributes().get(i)));
				if (get_solutions() != null && !get_solutions().isEmpty() && get_solutions().get(i) != null && !get_solutions().get(i).isEmpty()){
					List<String> sols = (_solutionQualifiers==null) ? get_solutions().get(i) : processSolutionQualifiers(i);
					sb.append("\t" + StringUtils.concat(",", sols));
				}
			}
		}
		
		return sb.toString();
	}
	
	public void printHead() {
		System.out.println(head());
	}
	
	public void printHead(boolean includeHeader) {
		System.out.println(head(includeHeader));
	}
	
	public String tail() {
		return tail(DEFAULT_TAIL);
	}
	
	public String tail(int numlines) {
		
		StringBuilder sb = new StringBuilder();
		
		int limit = Math.min(numlines, size());
		if (limit > 0) {
			for (int i = size() - limit; i < size(); i++) {
				sb.append(get_solutionIDs().get(i) + "\t" + StringUtils.concat("\t", get_attributes().get(i)));
				if (get_solutions() != null && !get_solutions().isEmpty() && get_solutions().get(i) != null && !get_solutions().get(i).isEmpty()){
					List<String> sols = (_solutionQualifiers==null) ? get_solutions().get(i) : processSolutionQualifiers(i);
					sb.append("\t" + StringUtils.concat(",", sols));
				}
				if (i < size() - 1)
					sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	public void printTail() {
		System.out.println(tail());
	}
	
	public Result subset(int[] rows) {
		Result newres = new Result();
		
		ArrayList<String> header = new ArrayList<String>();
		header.addAll(get_header());
		
		newres.set_header(header);
		
		for (int i = 0; i < rows.length; i++) {
			
			String id = get_solutionIDs().get(rows[i]);
			List<Double> att = new ArrayList<Double>();
			List<String> sol = new ArrayList<String>();
			List<Double> qual = (_solutionQualifiers!=null) ? new ArrayList<Double>() : null;
			
			sol.addAll(get_solutions().get(rows[i]));
			att.addAll(get_attributes().get(rows[i]));
			if(_solutionQualifiers!=null){
				qual.addAll(get_solutionQualifiers().get(rows[i]));
			}
			
			newres.get_solutionIDs().add(i, id);
			newres.get_attributes().add(i, att);
			newres.get_solutions().add(i, sol);
			if(_solutionQualifiers!=null){
				newres.get_solutionQualifiers().add(i,qual);
			}
			
		}
		
		return newres;
	}
	
	public Result subset(int[] rows, int[] columns) {
		Result newres = new Result();
		
		ArrayList<String> header = new ArrayList<String>();
		// header.add(get_header().get(0));
		for (int i = 0; i < columns.length; i++) {
			header.add(get_header().get(columns[i]));
			// if (columns[i] == 0)
			// continue;
			// else
		}
		newres.set_header(header);
		header.add(get_header().get(get_header().size() - 1));
		
		for (int i = 0; i < rows.length; i++) {
			
			String id = get_solutionIDs().get(rows[i]);
			List<Double> att = new ArrayList<Double>();
			List<String> sol = new ArrayList<String>();
			sol.addAll(get_solutions().get(rows[i]));
			List<Double> qual = (_solutionQualifiers!=null) ? new ArrayList<Double>() : null;
			if(_solutionQualifiers!=null){
				qual.addAll(get_solutionQualifiers().get(rows[i]));
			}
			
			for (int j = 0; j < columns.length; j++) {
				att.add(j, get_attributes().get(rows[i]).get(columns[j]));
			}
			
			newres.get_solutionIDs().add(i, id);
			newres.get_attributes().add(i, att);
			newres.get_solutions().add(i, sol);
			if(_solutionQualifiers!=null){
				newres.get_solutionQualifiers().add(i,qual);
			}
			
		}
		
		return newres;
	}
	
	public int[] solutionsWithFitnessAbove(int ofIndex, double threshold) {
		return solutionsWithFitness(ofIndex, threshold, true);
	}
	
	public int[] solutionsWithFitnessBellow(int ofIndex, double threshold) {
		return solutionsWithFitness(ofIndex, threshold, false);
	}
	
	public int[] solutionsWithFitness(int ofIndex, double threshold, boolean above) {
		
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < size(); i++) {
			List<Double> att = get_attributes().get(i);
			double val = att.get(ofIndex);
			if ((above && val >= threshold) || (!above && val <= threshold)) {
				indexes.add(i);
			}
		}
		
		int[] indexArray = new int[indexes.size()];
		for (int i = 0; i < indexes.size(); i++)
			indexArray[i] = indexes.get(i);
		
		return indexArray;
	}
	
	/**
	 * 
	 * @param ofIndex index of the target objetive function
	 * @param value the threshold value
	 * @param percent if the value is to be interpreted as a percentage of the maximum (for the
	 *            objective function)
	 * @param above if it should consider solutions above (true) or bellow (false) the specified
	 *            value
	 * @return int[] a list of solution indexes that respect the filters
	 */
	public int[] solutionsWithFitness(int ofIndex, double value, boolean percent, boolean above) {
		double threshold = value;
		if (percent) {
			double max = 0.0;
			for (int i = 0; i < size(); i++) {
				List<Double> att = get_attributes().get(i);
				double val = att.get(ofIndex);
				if (val > max)
					max = val;
			}
			threshold = value * max;
		}
		
		return solutionsWithFitness(ofIndex, threshold, above);
	}
	
	public int[] getNonDominatedSolutionsForOFs(int[] ofs) {
		double[][] front = new double[size()][ofs.length];
		for (int i = 0; i < size(); i++) {
			for (int j = 0; j < ofs.length; j++) {
				front[i][j] = get_attributes().get(i).get(ofs[j]);
			}
		}
		return MOUtils.filterNonDominatedFrontIndexes(front, size(), ofs.length);
	}
	
	public int[] solutionsWithElements(List<String> elements) {
		String[] elems = new String[elements.size()];
		elems = elements.toArray(elems);
		return solutionsWithElements(elems);
	}
	
	public int[] solutionsWithElements(String... elements) {
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < size(); i++) {
			List<String> sol = get_solutions().get(i);
			boolean containsAll = true;
			for (String e : elements)
				if (!sol.contains(e)) {
					containsAll = false;
					break;
				}
			if (containsAll) {
				indexes.add(i);
			}
		}
		
		int[] indexArray = new int[indexes.size()];
		for (int i = 0; i < indexes.size(); i++)
			indexArray[i] = indexes.get(i);
		
		return indexArray;
	}
	

	public int[] solutionsWithElementsAndQualifiers(List<String> elements, List<String> qualifiers) {
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < size(); i++) {
			List<String> sol = get_solutions().get(i);
			List<Double> quals = get_solutionQualifiers().get(i);
			List<String> qualConverted  = new ArrayList<String>();
			for(Double q : quals){
				String qual = (q==0.0) ? "K" : (q>1.0) ? "O" : "U";
				qualConverted.add(qual);
			}
			boolean containsAll = true;
			for(int j=0; j<elements.size(); j++){
				int idxElem = sol.indexOf(elements.get(j));
				if(idxElem<0){
					containsAll = false;
					break;
				}else{
					String qualElem = qualifiers.get(j);					
					if(!qualElem.equalsIgnoreCase(qualConverted.get(idxElem))){
						containsAll = false;
						break;
					}
				}
			}			
			if (containsAll) {
				indexes.add(i);
			}
		}
		
		int[] indexArray = new int[indexes.size()];
		for (int i = 0; i < indexes.size(); i++)
			indexArray[i] = indexes.get(i);
		
		return indexArray;
	}
	
	/**
	 * This method creates a new Result instance but removes all subset
	 * non-better and repeated solutions
	 * 
	 * @param objectiveFunction
	 * @param maximization
	 * @return
	 */
	public Result removeSubsetSolutions(int objectiveFunction, boolean maximization) {
		Set<Integer> toRemove = new HashSet<Integer>();
		int numSols = size();
		for (int i = 0; i < numSols; i++) {
			System.out.println("Dealing with solution [" + i + "/" + numSols + "]");
			List<String> sol = get_solutions().get(i);
			double att = get_attributes().get(i).get(objectiveFunction);
			int[] competitiveSolutions = solutionsWithElements(sol);
			for (int j = 0; j < competitiveSolutions.length; j++) {
				int altIndex = competitiveSolutions[j];
				int altSize = get_solutions().get(altIndex).size();
				if (i != j) {
					if (sol.size() == altSize) {
						toRemove.add(j);
					} else if (altSize > sol.size()) {
						double altAtt = get_attributes().get(altIndex).get(objectiveFunction);
						boolean better = (maximization) ? (altAtt >= att + FITNESS_THRESHOLD) : (altAtt <= att - FITNESS_THRESHOLD);
						if (!better)
							toRemove.add(j);
					}
				}
			}
		}
		int[] rows = new int[numSols - toRemove.size()];
		int counter = 0;
		for (int i = 0; i < numSols; i++)
			if (!toRemove.contains(i)) {
				rows[counter] = i;
				counter++;
			}
		
		return subset(rows);
	}
	
	public int smallerSolution() {
		int min = Integer.MAX_VALUE;
		int minindex = 0;
		for (int i = 0; i < size(); i++) {
			List<String> sol = get_solutions().get(i);
			int solsize = sol.size();
			if (solsize < min) {
				min = solsize;
				minindex = i;
			}
		}
		
		return minindex;
	}
	
	public int solutionSize(int solutionIndex) {
		List<String> sol = get_solutions().get(solutionIndex);
		return sol.size();
	}
	
	public int size() {
		return get_solutionIDs().size();
	}
	
	public double max(int attribute_index) {
		return top(attribute_index, true);
	}
	
	public double min(int attribute_index) {
		return top(attribute_index, false);
	}
	
	public double top(int attribute_index, boolean max) {
		double top = max ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		for (int i = 0; i < size(); i++) {
			double val = get_attributes().get(i).get(attribute_index);
			if ((max) ? val > top : val < top)
				top = val;
		}
		return top;
	}
	
	public Map<Integer, Double> getBestAttributesPerSolutionSize(int attribute_index, boolean isMaximization) {
		Map<Integer, Double> res = new HashMap<Integer, Double>();
		for (int i = 0; i < size(); i++) {
			
			Integer size = _solutions.get(i).size();
			Double att_val = _attributes.get(i).get(attribute_index);
			Double val = res.get(size);
	
			if (val == null || (isMaximization ? (att_val > val) : (att_val < val)))
				res.put(size, att_val);
		}
		return res;
	}
	
	public int[] dim() {
		int nrows = get_solutionIDs().size();
		int ncols = 0;
		if (nrows > 0) {
			ncols = get_attributes().get(0).size();
		}
		
		return new int[] { nrows, ncols };
	}
	
	public List<String> get_header() {
		if (_header == null)
			_header = new ArrayList<String>();
		return _header;
	}
	
	public List<String> get_solutionIDs() {
		if (_solutionIDs == null)
			_solutionIDs = new ArrayList<String>();
		return _solutionIDs;
	}
	
	public List<List<String>> get_solutions() {
		if (_solutions == null)
			_solutions = new ArrayList<List<String>>();
		return _solutions;
	}
	
	public List<List<Double>> get_attributes() {
		if (_attributes == null)
			_attributes = new ArrayList<List<Double>>();
		return _attributes;
	}
	
	public List<List<Double>> get_solutionQualifiers() {
		if(_solutionQualifiers == null)
			_solutionQualifiers = new ArrayList<List<Double>>();
		return _solutionQualifiers;
	}
	
	
	public double[][] getAttributeMatrix() {
		int numattribs = get_attributes().get(0).size();
		double[][] toret = new double[size()][numattribs];
		
		for (int i = 0; i < size(); i++)
			for (int j = 0; j < numattribs; j++)
				toret[i][j] = get_attributes().get(i).get(j);
		return toret;
	}
	
	public double[][] getAttributeMatrix(int[] indexes) {
		double[][] toret = new double[size()][indexes.length];
		
		for (int i = 0; i < size(); i++)
			for (int j = 0; j < indexes.length; j++)
				toret[i][j] = get_attributes().get(i).get(indexes[j]);
		return toret;
	}
	
	public void set_header(List<String> _header) {
		this._header = _header;
	}
	
	public void set_solutionIDs(List<String> _solutionIDs) {
		this._solutionIDs = _solutionIDs;
	}
	
	public void set_solutions(List<List<String>> _solutions) {
		this._solutions = _solutions;
	}
	
	public void set_attributes(List<List<Double>> _attributes) {
		this._attributes = _attributes;
	}
	
	public static void printTable(String[][] table) {
		// Find out what the maximum number of columns is in any row
		int maxColumns = 0;
		for (int i = 0; i < table.length; i++) {
			maxColumns = Math.max(table[i].length, maxColumns);
		}
		
		// Find the maximum length of a string in each column
		int[] lengths = new int[maxColumns];
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {
				lengths[j] = Math.max(table[i][j].length(), lengths[j]);
			}
		}
		
		// Generate a format string for each column
		String[] formats = new String[lengths.length];
		for (int i = 0; i < lengths.length; i++) {
			formats[i] = "%1$" + lengths[i] + "s" + (i + 1 == lengths.length ? "\n" : " ");
		}
		
		// Print 'em out
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {
				System.out.printf(formats[j], table[i][j]);
			}
		}
	}
}
