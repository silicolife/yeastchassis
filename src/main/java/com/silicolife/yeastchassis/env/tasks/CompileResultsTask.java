package com.silicolife.yeastchassis.env.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.silicolife.yeastchassis.env.AnalysisEnv;

import pt.uminho.ceb.biosystems.mew.core.analysis.solutionanalysis.ISolutionAnalysis;
import pt.uminho.ceb.biosystems.mew.core.model.exceptions.InvalidSteadyStateModelException;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.FluxValueMap;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.GeneticConditions;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SimulationProperties;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SimulationSteadyStateControlCenter;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SteadyStateSimulationResult;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.IObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ObjectiveFunctionParameterType;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.BPCYObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.CYIELDObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.FVAObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.FluxValueObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.NumKnockoutsObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.ProductYieldObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.IStrainOptimizationResult;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.IStrainOptimizationResultSet;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.solutionset.ResultSetFactory;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.solvers.builders.CPLEX3SolverBuilder;
import pt.uminho.ceb.biosystems.mew.solvers.lp.LPSolutionType;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.MapUtils;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.indexedhashmap.IndexedHashMap;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.io.MultipleExtensionFileFilter;
import pt.uminho.ceb.biosystems.mew.utilities.java.StringUtils;

/**
 *  * Calculates and compiles multiple results
 *  
 * @author pmaia
 */
public class CompileResultsTask extends AbstractAnalysisTask {
	
	public static final Logger logger = Logger.getLogger(AbstractAnalysisTask.class);
	
	public static FileFilter					INPUT_FILTER;
	public static final double					MAX_DIST						= 999999999;
	public static final String					OUTPUT_NAME						= "results";
	public static final String					OUTPUT_SUFFIX					= ".csv";
	
	protected boolean							_writeSolutions					= false;
	protected Map<String, String>				_identifiers					= null;
	protected int[]								_ignoreOFifZero					= null;
	protected Set<Integer>						_useNormalizedValueOFIndexes	= null;
	protected boolean							_useSimplified					= true;
	protected Set<String>						_idsToIgnore					= null;
	protected Map<ISolutionAnalysis, String>	_extraAnalysis					= null;
	
	public CompileResultsTask(String baseDir, AnalysisEnv env, LinkedHashMap<Map<String, Object>, String> ofMap) throws InvalidSteadyStateModelException {
		super(baseDir, env);
		overrideObjectiveFunctionsConfiguration = (ofMap == null) ? getDefaultOFMap() : ofMap;
	}
	
	public void addToOFConfigurationMap(Map<Map<String, Object>, String> extraOFConf) {
		for (Entry<Map<String, Object>, String> entry : extraOFConf.entrySet()) {
			overrideObjectiveFunctionsConfiguration.put(entry.getKey(), entry.getValue());
		}
	}
	
	public void setReactionsToIgnore(String... ids2ignore) {
		_idsToIgnore = new HashSet<String>(Arrays.asList(ids2ignore));
	}
	
	public void addExtraAnalysis(ISolutionAnalysis solutionAnalysis, String method) {
		if (_extraAnalysis == null) {
			_extraAnalysis = new IndexedHashMap<>();
		}
		
		_extraAnalysis.put(solutionAnalysis, method);
	}
	
	public void execute() throws Exception {
		
		Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> configurationMap = getConfigurationMap();
		
		if (mappingVar == null) {
			TreeMap<Integer, Pair<String, Map<Integer, String>>> toRun = _env.getTree().getValidatedNodes();
			for (Integer order : toRun.keySet()) {
				Pair<String, Map<Integer, String>> pair = toRun.get(order);
				Map<Integer, String> info = pair.getB();
				
				Map<String, String> convert = _env.convert(info);
				
				String branchDir = _directory + (_directory.endsWith("/") ? "" : "/") + _env.generateBranchBase(convert, "/");
				String confID = convert.get(AnalysisEnv.OPT_ALGORITHMS);
				
				JecoliGenericConfiguration configuration = (JecoliGenericConfiguration) _env.getAllOptAlgorithms().get(confID);
				
				compileResults(branchDir, convert, configuration);
			}
		} else {
			for (String subDir : configurationMap.keySet()) {				
				Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>> subConfMap = configurationMap.get(subDir);
				compileResultsNew(subDir, subConfMap);
			}
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <E extends IStrainOptimizationResult> void compileResultsNew(String outputDir, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>> branchesConvertMaps) throws Exception {
		
		List<Map<String, String>> converts = new ArrayList<>();
		for (Pair<JecoliGenericConfiguration, Map<String, String>> conv : branchesConvertMaps.values()) {
			converts.add(conv.getB());
		}
		
		String mappingsString = (mappings == null || mappings.isEmpty()) ? "" : "#[" + StringUtils.concat(",", mappings) + "]";
		
		String consensusName = "[" + _env.generateBranchBaseIntersect(converts, ",") + "]";
		
		String output = outputDir + "/" + OUTPUT_NAME + "#" + consensusName + mappingsString + OUTPUT_SUFFIX;
		
		boolean fileExists = new File(output).exists();
		
		if (!fileExists || (fileExists && override_if_existent)) {
			
			ResultSetFactory resultSet_factory = new ResultSetFactory();
			System.out.println("Dealing with [" + outputDir + "]");
			IStrainOptimizationResultSet aggregatedSet = null;
			boolean ok = false;
			JecoliGenericConfiguration globalConf = null;
			
			for (String branchDir : branchesConvertMaps.keySet()) {
				System.out.println("\t...dealing with[" + branchDir + "]");
				JecoliGenericConfiguration conf = branchesConvertMaps.get(branchDir).getA();
				if (globalConf == null) {
					globalConf = conf;
				}
				
				if (aggregatedSet == null) {
					aggregatedSet = resultSet_factory.getResultSetInstance(conf.getOptimizationStrategy(), conf);
				}
				
				String ext = getInputSuffix(conf);
				MultipleExtensionFileFilter filter = new MultipleExtensionFileFilter(ext);
				
				File[] files = new File(branchDir).listFiles(filter);
				
				if (files != null) {
					for (File file : files) {
						IStrainOptimizationResultSet currentResultSet = resultSet_factory.getResultSetInstance(conf.getOptimizationStrategy(), conf);
						currentResultSet.readSolutionsFromFile(file.getAbsolutePath());
						aggregatedSet = aggregatedSet.merge(currentResultSet);
					}
					ok = true;
				}
			}
			
			if (ok) {
				Map<String, SimulationSteadyStateControlCenter> controlCenters = getControlCenters(globalConf);
				
				FileWriter fw = new FileWriter(output);
				BufferedWriter bw = new BufferedWriter(fw);
				String HEADER = null;
				
				IndexedHashMap<IObjectiveFunction, String> ofs = globalConf.getObjectiveFunctionsMap();
				
				if (HEADER == null) {
					List<String> ofStrings = new ArrayList<String>();
					for (int o = 0; o < ofs.size(); o++) {
						IObjectiveFunction of = ofs.getKeyAt(o);
						String method = ofs.get(of);
						String head = of.getShortString() + (controlCenters.size() > 1 ? " (" + method + ")" : "");
						ofStrings.add(head);
					}
					
					List<String> saStrings = new ArrayList<String>();
					
					if (_extraAnalysis != null) {
						for (ISolutionAnalysis sa : _extraAnalysis.keySet()) {
							String[] descriptions = sa.getDescriptions();
							saStrings.addAll(Arrays.asList(descriptions));
						}
					}
					
					String OFS_HEADER = StringUtils.concat(",", ofStrings);
					String SAS_HEADER = "";
					if (!saStrings.isEmpty()) {
						SAS_HEADER = "," + StringUtils.concat(",", saStrings);
					}
					HEADER = "ID," + OFS_HEADER + SAS_HEADER + ",SOLUTION";
					
					bw.append(HEADER);
				}
				
				int numSolutions = aggregatedSet.getResultList().size();
				for (int i = 0; i < numSolutions; i++) {
					System.out.println("Dealing with solution [" + (i + 1) + "/" + numSolutions + "]");
					
					List<E> result = aggregatedSet.getResultList();
					GeneticConditions solution = result.get(i).getGeneticConditions();
					if (_idsToIgnore != null) {
						if (globalConf.getIsGeneOptimization()) {
							for (String id : _idsToIgnore) {
								solution.getGeneList().removeGene(id);
							}
							solution.updateReactionsList(globalConf.getGeneReactionSteadyStateModel());
						} else {
							for (String id : _idsToIgnore) {
								solution.getReactionList().removeReaction(id);
							}
						}
					}
					
					Map<String, SteadyStateSimulationResult> mres = new HashMap<String, SteadyStateSimulationResult>();
					for (String method : controlCenters.keySet()) {
						controlCenters.get(method).setGeneticConditions(solution);
						SteadyStateSimulationResult sol_res = controlCenters.get(method).simulate();
						mres.put(method, sol_res);
					}
					
					String[] fits = new String[ofs.size()];
					boolean invalid = false;
					for (int j = 0; j < ofs.size(); j++) {
						IObjectiveFunction of = ofs.getKeyAt(j);
						String method = ofs.get(of);
						SteadyStateSimulationResult sol_res = mres.get(method);
						
						if (sol_res != null && (sol_res.getSolutionType() != LPSolutionType.ERROR) && (sol_res.getSolutionType() != LPSolutionType.INFEASIBLE)) {
							double val = of.evaluate(sol_res);
							fits[j] = (_useNormalizedValueOFIndexes != null && _useNormalizedValueOFIndexes.contains(j)) ? String.valueOf(val) : String.valueOf(of.getUnnormalizedFitness(val));
						} else {
							fits[j] = String.valueOf(0.0);
						}
					}
					
					if (_ignoreOFifZero != null) {
						invalid = anyZero(fits, _ignoreOFifZero);
					}
					
					List<String> analysisRes = new ArrayList<String>();
					
					if (_extraAnalysis != null) {
						for (ISolutionAnalysis sa : _extraAnalysis.keySet()) {
							String method = _extraAnalysis.get(sa);
							SteadyStateSimulationResult sol_res = mres.get(method);
							
							if (sol_res != null && (sol_res.getSolutionType() != LPSolutionType.ERROR) && (sol_res.getSolutionType() != LPSolutionType.INFEASIBLE)) {
								double[] val = sa.analyse(sol_res);
								for (int l = 0; l < val.length; l++) {
									analysisRes.add(Double.toString(val[l]));
								}
							} else {
								for (int l = 0; l < sa.getDescriptions().length; l++) {
									analysisRes.add("NA");
								}
							}
						}
					}
					
					if (!invalid && !allZeros(fits)) {
						bw.newLine();
						String solID = "sol_" + i + "#" + consensusName.replaceAll(",", "#");
						String solString = null;
						if (_identifiers != null) {
							solString = result.get(i).toStringHumanReadableGC(" ");
							String[] elems = solString.split(",");
							String[] newelems = new String[elems.length];
							for (int e = 0; e < elems.length; e++) {
								newelems[e] = _identifiers.get(elems[e]);
							}
							solString = StringUtils.concat(" ", newelems);
							
						} else {
							solString = result.get(i).toStringHumanReadableGC(" ");
						}
						
						String analysisString = "";
						if (!analysisRes.isEmpty()) {
							analysisString = "," + StringUtils.concat(",", analysisRes);
						}
						
						String line_fits = solID + "," + StringUtils.concat(",", fits) + analysisString + "," + solString;
						bw.append(line_fits);
					}
				}
				System.out.println();
				bw.flush();
				bw.close();
				fw.close();
			}
			
		} else {
			System.out.println("File [" + output + "] exists... skipping");
		}
		
	}
	
	private <E extends IStrainOptimizationResult> void compileResults(String branchDir, Map<String, String> convert, JecoliGenericConfiguration configuration) throws Exception {
		
		String output = branchDir + "/" + OUTPUT_NAME + "#" + getBaseName(convert) + OUTPUT_SUFFIX;
		
		boolean fileExists = new File(output).exists();
		
		if (!fileExists || (fileExists && override_if_existent)) {
			
			String strategy = configuration.getOptimizationStrategy();
			
			ResultSetFactory resultSet_factory = new ResultSetFactory();
			
			System.out.println("BRANCH=" + branchDir);
			String inputFile = listFiles(branchDir, getInputSuffix(configuration))[0].getAbsolutePath();
			
			Map<String, SimulationSteadyStateControlCenter> controlCenters = getControlCenters(configuration);
			
			FileWriter fw = new FileWriter(output);
			BufferedWriter bw = new BufferedWriter(fw);
			String HEADER = null;
			
			IndexedHashMap<IObjectiveFunction, String> ofs = configuration.getObjectiveFunctionsMap();
			
			if (HEADER == null) {
				List<String> ofStrings = new ArrayList<String>();
				for (int o = 0; o < ofs.size(); o++) {
					IObjectiveFunction of = ofs.getKeyAt(o);
					String method = ofs.getValueAt(o);
					String head = of.getShortString() + (controlCenters.size() > 1 ? " (" + method + ")" : "");
					ofStrings.add(head);
				}
				
				List<String> saStrings = new ArrayList<String>();
				
				if (_extraAnalysis != null) {
					for (ISolutionAnalysis sa : _extraAnalysis.keySet()) {
						String[] descriptions = sa.getDescriptions();
						saStrings.addAll(Arrays.asList(descriptions));
					}
				}
				
				String OFS_HEADER = StringUtils.concat(",", ofStrings);
				String SAS_HEADER = "";
				if (!saStrings.isEmpty()) {
					SAS_HEADER = "," + StringUtils.concat(",", saStrings);
				}
				HEADER = "ID," + OFS_HEADER + SAS_HEADER + ",SOLUTION";
				
				bw.append(HEADER);
			}
			
			@SuppressWarnings("unchecked")
			IStrainOptimizationResultSet<?, E> inputResultSet = resultSet_factory.getResultSetInstance(strategy, configuration);
			inputResultSet.readSolutionsFromFile(inputFile);
			
			String consensusName = getBaseName(convert);
			int numSolutions = inputResultSet.getResultList().size();
			for (int i = 0; i < numSolutions; i++) {
				logger.info("[" + (i + 1) + "/" + numSolutions + "]");
				
				List<E> result = inputResultSet.getResultList();
				GeneticConditions solution = result.get(i).getGeneticConditions();
				if (_idsToIgnore != null) {
					if (configuration.getIsGeneOptimization()) {
						for (String id : _idsToIgnore) {
							solution.getGeneList().removeGene(id);
						}
						solution.updateReactionsList(configuration.getGeneReactionSteadyStateModel());
					} else {
						for (String id : _idsToIgnore) {
							solution.getReactionList().removeReaction(id);
						}
					}
				}
				
				Map<String, SteadyStateSimulationResult> mres = new HashMap<String, SteadyStateSimulationResult>();
				for (String method : controlCenters.keySet()) {
					controlCenters.get(method).setGeneticConditions(solution);
					SteadyStateSimulationResult sol_res = null;
					try {
						sol_res = controlCenters.get(method).simulate();
					} catch (Exception e) {
						System.out.println("No can do for method [" + method + "]: ");
						e.printStackTrace();
					}
					
					if (sol_res != null)
						mres.put(method, sol_res);
				}
				
				String[] fits = new String[ofs.size()];
				boolean invalid = false;
				for (int j = 0; j < ofs.size(); j++) {
					IObjectiveFunction of = ofs.getKeyAt(j);
					String method = ofs.get(of);
					SteadyStateSimulationResult sol_res = mres.get(method);
					
					if (sol_res != null && (sol_res.getSolutionType() != LPSolutionType.ERROR) && (sol_res.getSolutionType() != LPSolutionType.INFEASIBLE)) {
						double val = of.evaluate(sol_res);
						fits[j] = (_useNormalizedValueOFIndexes != null && _useNormalizedValueOFIndexes.contains(j)) ? String.valueOf(val) : String.valueOf(of.getUnnormalizedFitness(val));
					} else {
						fits[j] = String.valueOf(0.0);
					}
				}
				
				if (_ignoreOFifZero != null) {
					invalid = anyZero(fits, _ignoreOFifZero);
				}
				
				List<String> analysisRes = new ArrayList<String>();
				
				if (_extraAnalysis != null) {
					for (ISolutionAnalysis sa : _extraAnalysis.keySet()) {
						String method = _extraAnalysis.get(sa);
						SteadyStateSimulationResult sol_res = mres.get(method);
						
						if (sol_res != null && (sol_res.getSolutionType() != LPSolutionType.ERROR) && (sol_res.getSolutionType() != LPSolutionType.INFEASIBLE)) {
							double[] val = sa.analyse(sol_res);
							for (int l = 0; l < val.length; l++) {
								analysisRes.add(Double.toString(val[l]));
							}
						} else {
							for (int l = 0; l < sa.getDescriptions().length; l++) {
								analysisRes.add("NA");
							}
						}
					}
				}
				
				if (!invalid && !allZeros(fits)) {
					bw.newLine();
					String solID = "sol_" + i + "#" + consensusName.replaceAll(",", "#");
					String solString = null;
					if (_identifiers != null) {
						solString = result.get(i).toStringHumanReadableGC(" ");
						String[] elems = solString.split(",");
						String[] newelems = new String[elems.length];
						for (int e = 0; e < elems.length; e++) {
							newelems[e] = _identifiers.get(elems[e]);
						}
						solString = StringUtils.concat(" ", newelems);
						
					} else {
						solString = result.get(i).toStringHumanReadableGC(" ");
					}
					
					String analysisString = "";
					if (!analysisRes.isEmpty()) {
						analysisString = "," + StringUtils.concat(",", analysisRes);
					}
					
					String line_fits = solID + "," + StringUtils.concat(",", fits) + analysisString + "," + solString;
					bw.append(line_fits);
				}
			}
			System.out.println();
			bw.flush();
			bw.close();
			fw.close();
			
		} else {
			System.out.println("File [" + output + "] exists... skipping");
		}
		
	}
	
	private Map<String, Map<String, Object>> getSimulationConfigurations(JecoliGenericConfiguration configuration) {
		
		Map<String, Map<String, Object>> originalConfiguration = configuration.getSimulationConfiguration();
		Map<String, Map<String, Object>> newConfiguration = new HashMap<>();
		
		Map<String, Object> defaultConfig = null;
		
		for (String confString : originalConfiguration.keySet()) {
			
			Map<String, Object> originalSubConf = originalConfiguration.get(confString);
			if (originalSubConf.get(SimulationProperties.METHOD_ID).equals(SimulationProperties.FBA) || originalSubConf.get(SimulationProperties.METHOD_ID).equals(SimulationProperties.PFBA)) {
				Map<String, Object> newSubConf = new HashMap<String, Object>();
				newSubConf.putAll(originalSubConf);
				newSubConf.put(SimulationProperties.METHOD_ID, SimulationProperties.PFBA);
				newConfiguration.put(SimulationProperties.PFBA, newSubConf);
				defaultConfig = newSubConf;
			} else if (originalSubConf.get(SimulationProperties.METHOD_ID).equals(SimulationProperties.LMOMA)) {
				Map<String, Object> newSubConf = new HashMap<String, Object>();
				newSubConf.putAll(originalSubConf);
				newSubConf.put(SimulationProperties.METHOD_ID, SimulationProperties.LMOMA);
				newConfiguration.put(SimulationProperties.LMOMA, newSubConf);
				defaultConfig = newSubConf;
			} else if (originalSubConf.get(SimulationProperties.METHOD_ID).equals(SimulationProperties.MOMA)) {
				Map<String, Object> newSubConf = new HashMap<String, Object>();
				newSubConf.putAll(originalSubConf);
				newSubConf.put(SimulationProperties.METHOD_ID, SimulationProperties.MOMA);
				newConfiguration.put(SimulationProperties.MOMA, newSubConf);
				defaultConfig = newSubConf;
			} else if (originalSubConf.get(SimulationProperties.METHOD_ID).equals(SimulationProperties.TDPS)) {
				Map<String, Object> newSubConf = new HashMap<String, Object>();
				newSubConf.putAll(originalSubConf);
				newSubConf.put(SimulationProperties.METHOD_ID, SimulationProperties.TDPS);
				newConfiguration.put(SimulationProperties.TDPS, newSubConf);
				defaultConfig = newSubConf;
			}
		}
		
		if (!newConfiguration.containsKey(SimulationProperties.PFBA)) {
			Map<String, Object> newSubConf = new HashMap<String, Object>();
			newSubConf.putAll(defaultConfig);
			newSubConf.put(SimulationProperties.METHOD_ID, SimulationProperties.PFBA);
			newSubConf.put(SimulationProperties.IS_MAXIMIZATION, true);
			Map<String, Double> of = new HashMap<>();
			of.put(configuration.getSteadyStateModel().getBiomassFlux(), 1.0);
			newSubConf.put(SimulationProperties.OBJECTIVE_FUNCTION, of);
			newConfiguration.put(SimulationProperties.PFBA, newSubConf);
		}
		
		if (!newConfiguration.containsKey(SimulationProperties.LMOMA) || !newConfiguration.containsKey(SimulationProperties.MOMA)) {
			Map<String, Object> newSubConf = new HashMap<String, Object>();
			newSubConf.putAll(defaultConfig);
			newSubConf.put(SimulationProperties.METHOD_ID, SimulationProperties.LMOMA);
			newConfiguration.put(SimulationProperties.LMOMA, newSubConf);
		}
		
		return newConfiguration;
	}
//	}
	
	private Map<String, SimulationSteadyStateControlCenter> getControlCenters(JecoliGenericConfiguration configuration) throws Exception {
		Map<String, SimulationSteadyStateControlCenter> controlCenters = new HashMap<String, SimulationSteadyStateControlCenter>();
		
		Map<String, Map<String, Object>> simulationConfiguration = getSimulationConfigurations(configuration);
		
		for (String method : simulationConfiguration.keySet()) {
			Map<String, Object> methodConf = simulationConfiguration.get(method);
			
			MapUtils.prettyPrint(methodConf);
			SimulationSteadyStateControlCenter cc = new SimulationSteadyStateControlCenter(methodConf);
			
			FluxValueMap wtRef = cc.getWTReference();
			
			if (wtRef == null) {
				SteadyStateSimulationResult ssres = null;
				try {
					ssres = cc.simulate();
				} catch (Exception e) {
					e.printStackTrace();
				}
				wtRef = ssres.getFluxValues();
			}
			cc.setWTReference(wtRef);
			controlCenters.put(method, cc);
		}
		return controlCenters;
	}
	
	protected Map<Map<String, Object>, String> getObjectiveFunctionsConfiguration(String confID, Map<String, String> convert) {
		return (overrideObjectiveFunctionsConfiguration != null) ? overrideObjectiveFunctionsConfiguration : super.getObjectiveFunctionsConfiguration(confID, convert);
	}
	
	public void setReplaceIdentifiers(Map<String, String> ids) {
		_identifiers = ids;
	}
	
	public void setIgnoreOFifZero(int[] ofs) {
		_ignoreOFifZero = ofs;
	}
	
	public void setUseSimplified(boolean useSimplified) {
		_useSimplified = useSimplified;
	}
	
	public void setUseNormalizedValueOFIndexes(int[] indexes) {
		_useNormalizedValueOFIndexes = new HashSet<Integer>();
		for (int i : indexes)
			_useNormalizedValueOFIndexes.add(i);
	}
	
	public static boolean allZeros(String[] fits) {
		for (String f : fits)
			if (Double.parseDouble(f) > 0)
				return false;
			
		return true;
	}
	
	public static boolean anyZero(String[] fits, int[] ofs) {
		for (int i : ofs) {
			if (Double.parseDouble(fits[i]) <= 0)
				return true;
		}
		return false;
	}
	
	public String getInputSuffix(JecoliGenericConfiguration configuration) {
		return (configuration.getIsGeneOptimization()) ? ".gsss" : ".rsss";
	}
	
	private LinkedHashMap<Map<String, Object>, String> getDefaultOFMap() {
		LinkedHashMap<Map<String, Object>, String> ofMap = new LinkedHashMap<>();
		
		Map<String, Object> nkConf = new HashMap<>();
		nkConf.put(NumKnockoutsObjectiveFunction.OBJECTIVE_FUNCTION_ID, NumKnockoutsObjectiveFunction.ID);
		nkConf.put(NumKnockoutsObjectiveFunction.NK_PARAM_MAXIMIZATION, false);
		ofMap.put(nkConf, SimulationProperties.PFBA);
		
		Map<String, Object> fvBioConf = new HashMap<>();
		fvBioConf.put("InstanceID", "biomassPFBA");
		fvBioConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvBioConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvBioConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		ofMap.put(fvBioConf, SimulationProperties.PFBA);
		
		Map<String, Object> fvBioLMOMAConf = new HashMap<>();
		fvBioLMOMAConf.put("InstanceID", "biomassLMOMA");
		fvBioLMOMAConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvBioLMOMAConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvBioLMOMAConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		ofMap.put(fvBioLMOMAConf, SimulationProperties.LMOMA);
		
		/** TDPS */
		Map<String, Object> fvBioTDPSConf = new HashMap<>();
		fvBioTDPSConf.put("InstanceID", "biomassTDPS");
		fvBioTDPSConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvBioTDPSConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvBioTDPSConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_BIOMASS);
//		ofMap.put(fvBioTDPSConf, SimulationProperties.TDPS);
		
		Map<String, Object> fvProdConf = new HashMap<>();
		fvProdConf.put("InstanceID", "productPFBA");
		fvProdConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvProdConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvProdConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		ofMap.put(fvProdConf, SimulationProperties.PFBA);
		
		Map<String, Object> fvProdLMOMAConf = new HashMap<>();
		fvProdLMOMAConf.put("InstanceID", "productLMOMA");
		fvProdLMOMAConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvProdLMOMAConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvProdLMOMAConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		ofMap.put(fvProdLMOMAConf, SimulationProperties.LMOMA);
		
		/** TDPS */
		Map<String, Object> fvProdTDPSConf = new HashMap<>();
		fvProdTDPSConf.put("InstanceID", "productTDPS");
		fvProdTDPSConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvProdTDPSConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvProdTDPSConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_PRODUCT);
//		ofMap.put(fvProdTDPSConf, SimulationProperties.TDPS);
		
		Map<String, Object> fvSubConf = new HashMap<>();
		fvSubConf.put("InstanceID", "substratePFBA");
		fvSubConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvSubConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvSubConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		ofMap.put(fvSubConf, SimulationProperties.PFBA);
		
		Map<String, Object> fvSubLMOMAConf = new HashMap<>();
		fvSubLMOMAConf.put("InstanceID", "substrateLMOMA");
		fvSubLMOMAConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvSubLMOMAConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvSubLMOMAConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		ofMap.put(fvSubLMOMAConf, SimulationProperties.LMOMA);
		
		Map<String, Object> bpcyConf = new HashMap<>();
		bpcyConf.put(BPCYObjectiveFunction.OBJECTIVE_FUNCTION_ID, BPCYObjectiveFunction.ID);
		bpcyConf.put(BPCYObjectiveFunction.BPCY_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		bpcyConf.put(BPCYObjectiveFunction.BPCY_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		bpcyConf.put(BPCYObjectiveFunction.BPCY_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		ofMap.put(bpcyConf, SimulationProperties.PFBA);
		
		Map<String, Object> prodYieldConf = new HashMap<>();
		prodYieldConf.put("InstanceID", "YpsPFBA");
		prodYieldConf.put(ProductYieldObjectiveFunction.OBJECTIVE_FUNCTION_ID, ProductYieldObjectiveFunction.ID);
		prodYieldConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		prodYieldConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		prodYieldConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		prodYieldConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_MIN_BIOMASS, 0.001);
		prodYieldConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_MIN_UPTAKE, 0.001);
		ofMap.put(prodYieldConf, SimulationProperties.PFBA);
		
		Map<String, Object> prodYieldLMOMAConf = new HashMap<>();
		prodYieldLMOMAConf.put("InstanceID", "YpsLMOMA");
		prodYieldLMOMAConf.put(ProductYieldObjectiveFunction.OBJECTIVE_FUNCTION_ID, ProductYieldObjectiveFunction.ID);
		prodYieldLMOMAConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		prodYieldLMOMAConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		prodYieldLMOMAConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		prodYieldLMOMAConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_MIN_BIOMASS, 0.001);
		prodYieldLMOMAConf.put(ProductYieldObjectiveFunction.PYIELD_PARAM_MIN_UPTAKE, 0.001);
		ofMap.put(prodYieldLMOMAConf, SimulationProperties.LMOMA);
		
		Map<String, Object> cyieldConf = new HashMap<>();
		cyieldConf.put(CYIELDObjectiveFunction.OBJECTIVE_FUNCTION_ID, CYIELDObjectiveFunction.ID);
		cyieldConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		cyieldConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		cyieldConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_CONTAINER, ObjectiveFunctionParameterType.CONTAINER);
//		ofMap.put(cyieldConf, SimulationProperties.PFBA);
		
		/** TDPS */
		Map<String, Object> cyieldTDPSConf = new HashMap<>();
		cyieldTDPSConf.put(CYIELDObjectiveFunction.OBJECTIVE_FUNCTION_ID, CYIELDObjectiveFunction.ID);
		cyieldTDPSConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		cyieldTDPSConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		cyieldTDPSConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_CONTAINER, ObjectiveFunctionParameterType.CONTAINER);
//		ofMap.put(cyieldTDPSConf, SimulationProperties.TDPS);
		
		Map<String, Object> fvaMinConf = new HashMap<>();
		fvaMinConf.put(FVAObjectiveFunction.OBJECTIVE_FUNCTION_ID, FVAObjectiveFunction.ID);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_MAXIMIZATION, false);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_SOLVER, CPLEX3SolverBuilder.ID);
		ofMap.put(fvaMinConf, SimulationProperties.PFBA);
		
		Map<String, Object> fvaMaxConf = new HashMap<>();
		fvaMaxConf.put(FVAObjectiveFunction.OBJECTIVE_FUNCTION_ID, FVAObjectiveFunction.ID);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_MAXIMIZATION, true);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_SOLVER, CPLEX3SolverBuilder.ID);
		ofMap.put(fvaMaxConf, SimulationProperties.PFBA);
		
		return ofMap;
	}
}
