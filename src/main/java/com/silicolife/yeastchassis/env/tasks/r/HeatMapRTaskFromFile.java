package com.silicolife.yeastchassis.env.tasks.r;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.silicolife.yeastchassis.env.AnalysisEnv;

import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.io.MultipleExtensionAndContainsFileFilter;
import pt.uminho.ceb.biosystems.mew.utilities.java.StringUtils;

public class HeatMapRTaskFromFile extends AbstractRTask {
	
	public static final MultipleExtensionAndContainsFileFilter	INPUT_FILTER			= new MultipleExtensionAndContainsFileFilter(".csv");
	public static final String									OUTPUT_NAME				= "heatmap#";
	public static final String									OUTPUT_SUFFIX			= ".pdf";
																						

	protected List<String>										contains				= null;
	protected String											_inputFile				= null;
	protected String 											_outputFile 			= null;
																						
	public HeatMapRTaskFromFile(String baseDir, AnalysisEnv env, String inputFile) throws Exception {
		super(baseDir, env);
		_inputFile = inputFile;
	}
	
	public void setOutputName(String outputName){
		_outputFile = outputName;
	}
	
	public void execute() throws Exception {
		executeSingleOnDescendants();			
	}
	
	private void executeSingleOnDescendants() throws Exception {
		
		String output = null;
		if(_outputFile == null){
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
			output =  _directory + "/" + OUTPUT_NAME + consensusName + mappingsString + containsString + OUTPUT_SUFFIX;
		}else{
			output = _outputFile;
		}
		
		
		if (!reloaded)
			reloadR();
			
		code().addRCode("data <- load.frequencies(" + treatRfilename(_inputFile) + ")");;			
		code().addRCode("heatmap.variables.2(data,outputFile=" + treatRfilename(output) + ")");
		setCode();
		runOnly();
		reloaded = false;		
	}
	
	public String[] loadFiles() {
		return loadFiles(_directory);
	}
	
	public String[] loadFiles(String path) {
		File[] files = new File(path).listFiles(INPUT_FILTER);
		
		if (files != null) {
			String[] stringFiles = new String[files.length];
			
			for (int i = 0; i < files.length; i++)
				stringFiles[i] = files[i].getAbsolutePath();
				
			return stringFiles;
		} else {
			return null;
		}
	}
	
	@Override
	public FileFilter getInputFileFilter() {
		return INPUT_FILTER;
	}
}
