package com.silicolife.yeastchassis.env.tasks;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.log4j.Logger;

import com.rits.cloning.Cloner;
import com.silicolife.yeastchassis.env.AnalysisEnv;
import com.silicolife.yeastchassis.env.container.ContainerEnv;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.core.criticality.OptimizationTargetsControlCenter;
import pt.uminho.ceb.biosystems.mew.core.criticality.TargetIDStrategy;
import pt.uminho.ceb.biosystems.mew.core.model.components.EnvironmentalConditions;
import pt.uminho.ceb.biosystems.mew.core.model.exceptions.InvalidSteadyStateModelException;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SimulationProperties;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.configuration.GenericOptimizationProperties;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.configuration.IGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.AbstractObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.IObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.InvalidObjectiveFunctionConfiguration;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ObjectiveFunctionParameterType;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ObjectiveFunctionsFactory;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliOptimizationProperties;
import pt.uminho.ceb.biosystems.mew.solvers.builders.CPLEX3SolverBuilder;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.indexedhashmap.IndexedHashMap;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.io.Delimiter;
import pt.uminho.ceb.biosystems.mew.utilities.io.MultipleExtensionFileFilter;

/**
 * 
 * @author pmaia
 */
public abstract class AbstractAnalysisTask {
	
	public static final Logger logger = Logger.getLogger(AbstractAnalysisTask.class);
	
	public static final boolean					_debug									= false;
	public static final Pattern					NODE_SPLIT_PATTERN						= Pattern.compile("([1-9]+|[a-z]+)");
	
	protected String							_directory								= null;
	protected AnalysisEnv	_env;
	protected boolean							override_if_existent					= false;
	public String								mappingVar								= null;
	public List<String>							mappings								= null;
	protected Map<String, Map<String, Object>>	overrideSimulationConfigurations		= null;
	protected Map<Map<String, Object>, String>	overrideObjectiveFunctionsConfiguration	= null;
	
	public AbstractAnalysisTask(String directory, AnalysisEnv env) {
		_directory = directory;
		_env = env;
	}
	
	public void run() throws Throwable {
		execute();
	}
	
	public void setOverrideIfExistent(boolean overrideIfExistent) {
		this.override_if_existent = overrideIfExistent;
	}
	
	public void setMapping(String mappingVar, String... mapping) {
		this.mappingVar = mappingVar;
		if (mappings == null) {
			mappings = new ArrayList<String>();
		}
		for (String mapp : mapping) {
			this.mappings.add(mapp);
		}
	}
	
	public abstract void execute() throws Exception;
	
	protected String getBaseName(Map<String, String> convert) {
		
		String base = _env.generateBranchBase(convert, Delimiter.COMMA.toString());
		String vartag = "[" + base + "]";
		
		return vartag;
	}
	
	protected File[] listFiles(String dir, String... extensions) {
		FileFilter filter = new MultipleExtensionFileFilter(extensions);
		File[] files = new File(dir).listFiles(filter);
		return files;
	}
	
	public Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> getConfigurationMap() throws Exception {
		TreeMap<Integer, Pair<String, Map<Integer, String>>> toRun = _env.getTree().getValidatedNodes();
		
		Integer indexOfVar = null;
		if (mappingVar != null) {
			indexOfVar = _env.getOrderedValues().indexOf(mappingVar);
		} else {
			indexOfVar = _env.getOrderedValues().size() - 1;
		}
		
		Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> confMap = new LinkedHashMap<>();
		
		for (Integer order : toRun.keySet()) {
			Pair<String, Map<Integer, String>> pair = toRun.get(order);
			
			Map<Integer, String> info = pair.getB();
			Map<String, String> convert = _env.convert(info);
			
			String varValue = convert.get(mappingVar);
			if (mappings == null || mappings.size() == 0 || (mappings.size() > 0 && containsAll(varValue, mappings))) {
				String subDir = _directory + (_directory.endsWith("/") ? "" : "/") + _env.generateBranchBaseUntil(convert, "/", indexOfVar);
				
				String branchDir = _directory + (_directory.endsWith("/") ? "" : "/") + _env.generateBranchBase(convert, "/");
				
				IGenericConfiguration genericConf = (IGenericConfiguration) generateConfigurationForBranch(convert);
				JecoliGenericConfiguration configuration = new JecoliGenericConfiguration(genericConf.getPropertyMap());				
				
				if (confMap.containsKey(subDir)) {
					confMap.get(subDir).put(branchDir, new Pair<JecoliGenericConfiguration, Map<String, String>>(configuration, convert));
				} else {
					HashMap<String, Pair<JecoliGenericConfiguration, Map<String, String>>> subConf = new HashMap<>();
					subConf.put(branchDir, new Pair<JecoliGenericConfiguration, Map<String, String>>(configuration, convert));
					confMap.put(subDir, subConf);
				}
			}
		}
		
		return confMap;
	}
	
	public Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> getConfigurationMapResultsOnVariable() throws Exception {
		TreeMap<Integer, Pair<String, Map<Integer, String>>> toRun = _env.getTree().getValidatedNodes();
		
		Integer indexOfVar = null;
		if (mappingVar != null) {
			indexOfVar = _env.getOrderedValues().indexOf(mappingVar);
		} else {
			indexOfVar = _env.getOrderedValues().size() - 1;
		}
		
		Map<String, Map<String, Pair<JecoliGenericConfiguration, Map<String, String>>>> confMap = new LinkedHashMap<>();
		
		for (Integer order : toRun.keySet()) {
			Pair<String, Map<Integer, String>> pair = toRun.get(order);
			
			Map<Integer, String> info = pair.getB();
			Map<String, String> convert = _env.convert(info);
			
			String varValue = convert.get(mappingVar);
			if (mappings == null || mappings.size() == 0 || (mappings.size() > 0 && containsAll(varValue, mappings))) {
				String subDir = _directory + (_directory.endsWith("/") ? "" : "/") + _env.generateBranchBaseUntil(convert, "/", indexOfVar + 1);
				
				String branchDir = _directory + (_directory.endsWith("/") ? "" : "/") + _env.generateBranchBase(convert, "/");				
				
				IGenericConfiguration genericConf = (IGenericConfiguration) generateConfigurationForBranch(convert);
				JecoliGenericConfiguration configuration = new JecoliGenericConfiguration(genericConf.getPropertyMap());
				
				if (confMap.containsKey(subDir)) {
					confMap.get(subDir).put(branchDir, new Pair<JecoliGenericConfiguration, Map<String, String>>(configuration, convert));
				} else {
					HashMap<String, Pair<JecoliGenericConfiguration, Map<String, String>>> subConf = new HashMap<>();
					subConf.put(branchDir, new Pair<JecoliGenericConfiguration, Map<String, String>>(configuration, convert));
					confMap.put(subDir, subConf);
				}
			}
		}
		
		return confMap;
	}
	
	private boolean containsAll(String varValue, List<String> mappings) {
		for (String mapp : mappings) {
			if (!varValue.contains(mapp)) {
				return false;
			}
		}
		return true;
	}
	
	public String splitNodeKey(String key, int until) {
		String ret = "";
		
		Matcher matcher = NODE_SPLIT_PATTERN.matcher(key);
		int current = 0;
		while (matcher.find() && current < until - 1) {
			ret += matcher.group().trim();
			current++;
		}
		
		return ret;
	}
	
	protected IGenericConfiguration generateConfigurationForBranch(Map<String, String> convert) throws Exception {
		
		IGenericConfiguration configuration = (IGenericConfiguration) _env.getAllOptAlgorithms().get(convert.get(AnalysisEnv.OPT_ALGORITHMS));
		
		/**
		 * MODEL
		 */
		String containerID = convert.get(AnalysisEnv.CONTAINER);
		ContainerEnv containerEnv = (ContainerEnv) _env.getAllContainers().get(containerID);
		ISteadyStateModel model = null;
		try {
			Container container = containerEnv.getContainer();
			Set<String> toRemove = container.identifyMetabolitesIdByPattern(Pattern.compile(".*_b"));
			container.removeMetabolites(toRemove);
			container.putDrainsInReactantsDirection();
			model = containerEnv.getModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/**
		 * ENV CONDITIONS
		 */
		String ecID = convert.get(AnalysisEnv.ENV_CONDITIONS);
		EnvironmentalConditions ec = null;
		if (ecID != null) {
			ec = (EnvironmentalConditions) _env.getAllEnvConditions().get(ecID);
			if (ec == null) {
				ec = new EnvironmentalConditions();
			}
		}
		
		/**
		 * INITIAL GENETIC CONDITIONS
		 */
		String initgcID = convert.get(AnalysisEnv.INIT_GC);
		EnvironmentalConditions initGC = null;
		if (initgcID != null) {
			initGC = (EnvironmentalConditions) _env.getAllGeneConditions().get(initgcID);
			if (initGC != null) {
				ec.addAllReactionConstraints(initGC);
			}
		}
		
		/**
		 * PRODUCTION TARGETS
		 */
		String productionTargetID = convert.get(AnalysisEnv.PRODUCTION_TARGETS);
		String productionTarget = (String) _env.getAllProductionTargets().get(productionTargetID);
		
		/**
		 * BIOMASS
		 */
		String biomass = _env.getBioMemory().get(ecID);
		if (biomass == null) {
			biomass = model.getBiomassFlux();
		} else {
			model.setBiomassFlux(biomass);
			System.out.println("Setting biomass reaction ID [" + biomass + "] for branch [" + _env.generateBranchID(convert) + "]");
		}
		
		/**
		 * SUBSTRATE
		 */
		
		String substrate = _env.getEcMemory().get(ecID);
		
		/**
		 * CONTAINER
		 */
		Container container = containerEnv.getContainer();
		
		/**
		 * CONFIGURATION (SIMULATION)
		 */
		String simulationConfID = convert.get(AnalysisEnv.OPT_SIMULATION_CONF);
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> simulationConfigurations = (overrideSimulationConfigurations != null) ? overrideSimulationConfigurations
				: (Map<String, Map<String, Object>>) _env.getAllOptSimulationConfig().get(simulationConfID);
		try {
			simulationConfigurations = processSimulationConfigurations(model, ec, biomass, productionTarget, container, simulationConfigurations);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		for (String simMethod : simulationConfigurations.keySet()) {
			Set<Pair<String, ?>> extraSimConf = _env.getExtraSimulationConfigurationMemory().get(ecID, simMethod);
			if (extraSimConf != null) {
				for (Pair<String, ?> pair : extraSimConf) {
					simulationConfigurations.get(simMethod).put(pair.getA(), pair.getB());
				}
			}
		}
		
		/**
		 * CONFIGURATION (OBJECTIVE FUNCTION)
		 */
		String confID = convert.get(AnalysisEnv.OPT_OBJECTIVE_FUNCTION_CONF);
		Map<Map<String, Object>, String> ofTypesMap = (overrideObjectiveFunctionsConfiguration != null) ? overrideObjectiveFunctionsConfiguration : getObjectiveFunctionsConfiguration(confID, convert);
		
		Set<String> simulationMethods = new HashSet<>();
		for (String sim : ofTypesMap.values())
			simulationMethods.add(sim);
		
		Map<IObjectiveFunction, String> mapOF2Sim = new IndexedHashMap<>();

		for (Entry<Map<String, Object>, String> entry : ofTypesMap.entrySet()) {
			String method = entry.getValue();
			Map<String, Object> ofConfiguration = entry.getKey();
			IObjectiveFunction of = null;
			try {
				of = processObjectiveFunction(productionTarget, biomass, substrate, model, containerEnv.getContainer(), ofConfiguration, _env.getOfFactory());
			} catch (InvalidObjectiveFunctionConfiguration | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				e.printStackTrace();
			}
			if (of != null) {
				mapOF2Sim.put(of, method);
			}
		}
		
		/*****************************************
		 * SET ALL PARAMETERS IN CONFIGURATION ! *
		 ****************************************/
		configuration.setProperty(GenericOptimizationProperties.STEADY_STATE_MODEL, model);
		configuration.setProperty(GenericOptimizationProperties.STEADY_STATE_GENE_REACTION_MODEL, model);
		configuration.setProperty(GenericOptimizationProperties.SIMULATION_CONFIGURATION, simulationConfigurations);
		configuration.setProperty(GenericOptimizationProperties.MAP_OF2_SIM, mapOF2Sim);
		
		Set<Pair<String, ?>> extraConf = _env.getExtraConfigurationMemory().get(ecID, confID);
		if (extraConf != null) {
			for (Pair<String, ?> pair : extraConf) {
				configuration.setProperty(pair.getA(), pair.getB());
			}
		}				
		
		Map<String, List<String>> swapsMap = _env.getSwapsMemory().get(containerID);
		if (swapsMap != null) {
			configuration.setProperty(JecoliOptimizationProperties.REACTION_SWAP_MAP, swapsMap);
		}
		
		return configuration;
	}
	
	protected Map<Map<String, Object>, String> getObjectiveFunctionsConfiguration(String confID, Map<String, String> convert) {
		@SuppressWarnings("unchecked")
		Map<Map<String, Object>, String> ofTypesMap = (Map<Map<String, Object>, String>) _env.getAllOptObjectiveFunctionConfig().get(confID);
		return ofTypesMap;
	}
	
	public Map<Map<String, Object>, String> getOverrideObjectiveFunctionsConfiguration() {
		return overrideObjectiveFunctionsConfiguration;
	}
	
	public void setOverrideObjectiveFunctionsConfiguration(Map<Map<String, Object>, String> overrideObjectiveFunctionsConfiguration) {
		this.overrideObjectiveFunctionsConfiguration = overrideObjectiveFunctionsConfiguration;
	}
	
	public Map<String, Map<String, Object>> getOverrideSimulationConfigurations() {
		return overrideSimulationConfigurations;
	}
	
	public void setOverrideSimulationConfigurations(Map<String, Map<String, Object>> overrideSimulationConfigurations) {
		this.overrideSimulationConfigurations = overrideSimulationConfigurations;
	}
	
	public Map<String, Map<String, Object>> processSimulationConfigurations(ISteadyStateModel model, EnvironmentalConditions ec, String biomass, String productionTarget, Container container,
			Map<String, Map<String, Object>> simulationConfiguration) throws Exception {
			
		Map<String, Map<String, Object>> toret = new Cloner().deepClone(simulationConfiguration);
		
		List<String> basicConfigPropIDs = Arrays.asList(SimulationProperties.MODEL, 
														SimulationProperties.ENVIRONMENTAL_CONDITIONS, 
														SimulationProperties.OBJECTIVE_FUNCTION, 
														SimulationProperties.SOLVER);
		
		for (String method : toret.keySet()) {
			Map<String, Object> methodConf = toret.get(method);
			
			// Put basic configurations
			for (String propID : basicConfigPropIDs) {
				if(!methodConf.containsKey(propID)){
					methodConf.put(propID, null);
				}
			}
			
			for (String prop : methodConf.keySet()) {
				if (methodConf.get(prop) == null) {
					switch (prop) {
						case SimulationProperties.MODEL: {
							logger.info(" GOT A [" + method + "/" + prop + "], DEALING WITH IT!");
							model.setBiomassFlux(biomass);
							methodConf.put(prop, model);
							break;
						}
						case SimulationProperties.ENVIRONMENTAL_CONDITIONS: {
							logger.info(" GOT A [" + method + "/" + prop + "], DEALING WITH IT!");
							if(ec != null)
								methodConf.put(prop, ec);
							break;
						}
						case SimulationProperties.OBJECTIVE_FUNCTION: {
							logger.info(" GOT A [" + method + "/" + prop + "], DEALING WITH IT!");
							Map<String, Double> of = new HashMap<>();
							of.put(biomass, 1.0);
							methodConf.put(prop, of);
							break;
						}
						case SimulationProperties.SOLVER: {
							logger.info(" GOT A [" + method + "/" + prop + "], DEALING WITH IT!");
							methodConf.put(prop, CPLEX3SolverBuilder.ID);
							break;
						}
						default:
							throw new Exception("Invalid simulation configuration property [" + prop + "] for simulation method [" + method + "]. Cannot be null.");
					}
				}
			}
			toret.put(method, methodConf);
		}
		
		return toret;
	}
	
	public IObjectiveFunction processObjectiveFunction(String productionTarget, String biomass, String substrate, ISteadyStateModel model, Container container, Map<String, Object> ofConfiguration, ObjectiveFunctionsFactory ofFactory)
			throws InvalidObjectiveFunctionConfiguration, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException {
			
		Map<String, Object> cloneConfig = new Cloner().deepClone(ofConfiguration);
		String ofID = (String) cloneConfig.get(AbstractObjectiveFunction.OBJECTIVE_FUNCTION_ID);
		if (ofID == null) {
			throw new InvalidObjectiveFunctionConfiguration("Invalid objective function configuration. Parameter [" + AbstractObjectiveFunction.OBJECTIVE_FUNCTION_ID + "] must be set.");
		} else {
			Map<String, ObjectiveFunctionParameterType> mandatoryParameters = ofFactory.getObjectiveFunctionParameterTypes(ofID);
			for (String param : mandatoryParameters.keySet()) {
					ObjectiveFunctionParameterType paramType = mandatoryParameters.get(param);
					switch (paramType) {
						case REACTION_BIOMASS:
							cloneConfig.put(param, biomass);
							break;
						case REACTION_PRODUCT:
							cloneConfig.put(param, productionTarget);
							break;
						case REACTION_SUBSTRATE:
							cloneConfig.put(param, substrate);
							break;
						case MODEL:
							cloneConfig.put(param, model);
							break;
						case CONTAINER:
							cloneConfig.put(param, container);
							break;
						case REACTION:{
							ObjectiveFunctionParameterType reactionType = (ObjectiveFunctionParameterType) cloneConfig.get(param);
							switch (reactionType) {
								case REACTION_BIOMASS:
									cloneConfig.put(param, biomass);
									break;
								case REACTION_PRODUCT:
									cloneConfig.put(param, productionTarget);
									break;
								case REACTION_SUBSTRATE:
									cloneConfig.put(param, substrate);
								default:
									break;
							}
							break;
						}
						default:
							break;
					}
					
				if (cloneConfig.get(param)!=null && ObjectiveFunctionParameterType.class.isAssignableFrom(cloneConfig.get(param).getClass())) {
					ObjectiveFunctionParameterType subtype = (ObjectiveFunctionParameterType) cloneConfig.get(param);
					switch (subtype) {
						case REACTION_BIOMASS:
							cloneConfig.put(param, biomass);
							break;
						case REACTION_PRODUCT:
							cloneConfig.put(param, productionTarget);
							break;
						case REACTION_SUBSTRATE:
							cloneConfig.put(param, substrate);
							break;
						case MODEL:
							cloneConfig.put(param, model);
							break;
						case CONTAINER:
							cloneConfig.put(param, container);
							break;
						default:
							break;
					}
				}
			}
		}
		return ofFactory.getObjectiveFunction(cloneConfig);
	}
	
	protected Set<String> getNonTargets(
			String containerID, 
			ContainerEnv containerEnv, 
			ISteadyStateModel model, 
			EnvironmentalConditions ec, 
			String strategy, 
			String pathwwayID_mem, 
			String ecID_mem,
			String initgcID_mem, 
			MultiKeyMap<String, 
			Set<String>> nonTargetsMemory) throws InvalidSteadyStateModelException, Exception {
		
		Set<String> nonTargets;
		nonTargets = nonTargetsMemory.get(containerID, pathwwayID_mem, ecID_mem, initgcID_mem);
		
		if (nonTargets == null) {
			logger.info("Non-targets not available... computing defaults for strategy [" + strategy + "]...");
			OptimizationTargetsControlCenter targetsCC = new OptimizationTargetsControlCenter(strategy, CPLEX3SolverBuilder.ID, containerEnv.getContainer(), model, ec, null, null, 7);
			targetsCC.enable(TargetIDStrategy.IDENTIFY_CRITICAL);
			targetsCC.enable(TargetIDStrategy.IDENTIFY_DRAINS_TRANSPORTS);
			targetsCC.enable(TargetIDStrategy.IDENTIFY_EQUIVALENCES);
			targetsCC.enable(TargetIDStrategy.IDENTIFY_ZEROS);
			targetsCC.setOnlyDrains(false);
			targetsCC.process();
			nonTargets = targetsCC.getNonTargets();
			nonTargetsMemory.put(containerID, pathwwayID_mem, ecID_mem, initgcID_mem, nonTargets);
		}
		return nonTargets;
	}
	
}
