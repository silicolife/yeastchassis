package com.silicolife.yeastchassis.env;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.log4j.Logger;

import com.silicolife.yeastchassis.env.components.AbstractMultRegistTreeEnv;
import com.silicolife.yeastchassis.env.components.BasicNodeTree;
import com.silicolife.yeastchassis.env.components.INodeTree;
import com.silicolife.yeastchassis.env.components.IRunTreeStatus;
import com.silicolife.yeastchassis.env.components.RegistEnvNode;
import com.silicolife.yeastchassis.env.container.ContainerEnv;

import pt.uminho.ceb.biosystems.jecoli.algorithm.components.terminationcriteria.ITerminationCriteria;
import pt.uminho.ceb.biosystems.jecoli.algorithm.components.terminationcriteria.InvalidNumberOfIterationsException;
import pt.uminho.ceb.biosystems.jecoli.algorithm.components.terminationcriteria.InvalidTerminationCriteriaParameter;
import pt.uminho.ceb.biosystems.jecoli.algorithm.components.terminationcriteria.IterationListenerHybridTerminationCriteria;
import pt.uminho.ceb.biosystems.jecoli.algorithm.components.terminationcriteria.NumFunctionEvaluationsListenerHybridTerminationCriteria;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.core.cmd.searchtools.ClusterConstants;
import pt.uminho.ceb.biosystems.mew.core.cmd.searchtools.configuration.OptimizationConfiguration;
import pt.uminho.ceb.biosystems.mew.core.model.components.EnvironmentalConditions;
import pt.uminho.ceb.biosystems.mew.core.model.components.ReactionConstraint;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SimulationProperties;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.configuration.GenericConfiguration;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.configuration.GenericOptimizationProperties;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.configuration.IGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.AbstractObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ObjectiveFunctionsFactory;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.IStrainOptimizationResultSet;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliOptimizationProperties;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.MapUtils;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.tree.generictree.TreeNode;
import pt.uminho.ceb.biosystems.mew.utilities.io.Delimiter;
import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;
import pt.uminho.ceb.biosystems.mew.utilities.io.MultipleExtensionFileFilter;
import pt.uminho.ceb.biosystems.mew.utilities.java.StringUtils;

/**
 * Analysis environment
 * 
 * @author pmaia
 */
public class AnalysisEnv extends AbstractMultRegistTreeEnv {
	
	private static final long										serialVersionUID					= 1L;
	private static final Logger										logger								= Logger.getLogger(AnalysisEnv.class);
	
	public static final String										CONTAINER							= "CONTAINER";
	public static final String										PATHWAYS							= "PATHWAYS";
	public static final String										NON_TARGETS							= "NON_TARGETS";
	public static final String										VAR_NON_TARGETS						= "VAR_TARGETS";
	public static final String										ENV_CONDITIONS						= "ENV_CONDITIONS";
	public static final String										INIT_GC								= "INIT_GC";
	public static final String										PRODUCTION_TARGETS					= "PRODUCTION_TARGETS";
	public static final String										OPT_ALGORITHMS						= "OPTIMIZATION_ALGORITHMS";
	public static final String										OPT_SIMULATION_CONF					= "OPTIMIZATION_SIMULATION_CONF";
	public static final String										OPT_OBJECTIVE_FUNCTION_CONF			= "OPTIMIZATION_OBJECTIVE_FUNCTION_CONFIGURATION";	
	public static final String										NUM_RUNS							= "NUM_RUNS";
	public static final String										TASK_TYPE_ID						= "TASK_TYPE_ID";
	public static final String										FETCH_JOBS							= "FETCH_JOBS";
	
	protected Map<String, String>									ecMemory							= null;
	protected Map<String, String>									bioMemory							= null;
	protected Map<String, Map<String, List<String>>>				swapsMemory							= null;
	protected MultiKeyMap<String, Set<String>>						nonTargetsMemory					= null;
	protected MultiKeyMap<String, Set<Pair<String, ?>>>				extraConfigurationMemory			= null;
	protected MultiKeyMap<String, Set<Pair<String, ?>>>				extraSimulationConfigurationMemory	= null;
	
	protected static ObjectiveFunctionsFactory						ofFactory							= new ObjectiveFunctionsFactory();
	
	protected Set<String>											mandatoryClasses					= null;
	
	public AnalysisEnv() {
		super();
		nonTargetsMemory = new MultiKeyMap<>();
		ecMemory = new HashMap<>();
		bioMemory = new HashMap<>();
		extraConfigurationMemory = new MultiKeyMap<>();
		extraSimulationConfigurationMemory = new MultiKeyMap<>();
		swapsMemory = new HashMap<>();
	}

	public Map<String, String> getEcMemory() {
		return ecMemory;
	}
	
	public Map<String, String> getBioMemory() {
		return bioMemory;
	}
	
	public Map<String, Map<String, List<String>>> getSwapsMemory() {
		return swapsMemory;
	}
	
	public MultiKeyMap<String, Set<String>> getNonTargetsMemory() {
		return nonTargetsMemory;
	}
	
	public MultiKeyMap<String, Set<Pair<String, ?>>> getExtraConfigurationMemory() {
		return extraConfigurationMemory;
	}
	
	public MultiKeyMap<String, Set<Pair<String, ?>>> getExtraSimulationConfigurationMemory() {
		return extraSimulationConfigurationMemory;
	}
	
	public ObjectiveFunctionsFactory getOfFactory() {
		return ofFactory;
	}
	
	@Override
	protected Map<String, Class<?>> possibleRegisteredClasses() {
		Map<String, Class<?>> possibles = new HashMap<String, Class<?>>();
		possibles.put(CONTAINER, ContainerEnv.class);
		possibles.put(PATHWAYS, String.class);
		possibles.put(ENV_CONDITIONS, EnvironmentalConditions.class);
		possibles.put(INIT_GC, EnvironmentalConditions.class);
		possibles.put(NON_TARGETS, Set.class);
		possibles.put(VAR_NON_TARGETS, Set.class);
		possibles.put(PRODUCTION_TARGETS, String.class);
		possibles.put(OPT_ALGORITHMS, IGenericConfiguration.class);
		possibles.put(OPT_SIMULATION_CONF, Map.class);
		possibles.put(OPT_OBJECTIVE_FUNCTION_CONF, Map.class);
		
		return possibles;
	}
	
	@Override
	protected List<String> createOrderValues() {
		List<String> order = Arrays.asList(CONTAINER, PATHWAYS, ENV_CONDITIONS, INIT_GC, NON_TARGETS, VAR_NON_TARGETS, PRODUCTION_TARGETS, OPT_ALGORITHMS, OPT_SIMULATION_CONF,OPT_OBJECTIVE_FUNCTION_CONF);
		return order;
	}
	
	@Override
	protected Set<String> createMandatoryValues() {
		mandatoryClasses = new HashSet<String>();
		mandatoryClasses.add(CONTAINER);
		mandatoryClasses.add(PRODUCTION_TARGETS);
		mandatoryClasses.add(OPT_ALGORITHMS);
		mandatoryClasses.add(OPT_SIMULATION_CONF);
		mandatoryClasses.add(OPT_OBJECTIVE_FUNCTION_CONF);
		return mandatoryClasses;
	}
	
	public Set<String> getMandatoryClasses() {
		if (mandatoryClasses == null)
			createMandatoryValues();
		return mandatoryClasses;
	}
	
	@Override
	protected String infoTreeString(Object result) {
		return ((IRunTreeStatus) result).getInfo();
	}
	
	@Override
	protected String infoConsoleToRun(Object result) {
		return ((IRunTreeStatus) result).getDetailedInfo();
	}
	
	protected Map<Integer, String> convertMap(Map<String, String> toConvert) {
		Map<Integer, String> toRet = new TreeMap<Integer, String>();
		for (String key : toConvert.keySet()) {
			toRet.put(ordv.indexOf(key), toConvert.get(key));
		}
		return toRet;
	}

	protected Map<String, Object> createExtraParamsMap(int numRuns) {
		HashMap<String, Object> toRet = new HashMap<String, Object>();
		return toRet;
	}
	
	public void registerContainer(String id, ContainerEnv env) {
		registValue(CONTAINER, id, env);
	}
	
	public void registerContainer(String id, ContainerEnv env, Map<String, List<String>> swapsMap) {
		addToSwapsMemory(id, swapsMap);
		registValue(CONTAINER, id, env);
	}	
	
	public void registerEC(String id, String carbonSourceID, EnvironmentalConditions ec) {
		addToECMemory(id, carbonSourceID);
		registValue(ENV_CONDITIONS, id, copyEcAndId(id, ec));
	}
	
	public void registerEC(String id, String carbonSourceID, String biomassReactionID, EnvironmentalConditions ec) {
		addToECMemory(id, carbonSourceID);
		addBiomassToMemory(id, biomassReactionID);
		registValue(ENV_CONDITIONS, id, copyEcAndId(id, ec));
	}
	
	protected EnvironmentalConditions copyEcAndId(String id, EnvironmentalConditions ec) {
		EnvironmentalConditions copyEc = ec.copy();
		if (copyEc.getId().isEmpty()) {
			copyEc.setId(id);
		}
		return copyEc;
	}
	
	private void addToECMemory(String id, String carbonSourceID) {
		ecMemory.put(id, carbonSourceID);
	}
	
	private void addBiomassToMemory(String id, String biomassReactionID) {
		bioMemory.put(id, biomassReactionID);
	}
	
	private void addToSwapsMemory(String id, Map<String, List<String>> swapsMap) {
		swapsMemory.put(id, swapsMap);
	}
	
	public void registerInitGC(String id, EnvironmentalConditions gc) {
		registValue(INIT_GC, id, gc);
	}
	
	public void registerNonTargets(String id) {
		registValue(NON_TARGETS, id, null); // register variable id only, the defaults will be automatically computed
	}
	
	public void registerNonTargets(String id, List<String> nonTargets) {
		Set<String> togo = new HashSet<>(); //removes all repeatead
		togo.addAll(nonTargets);
		registerNonTargets(id, togo);
	}
	
	public void registerNonTargets(String id, Set<String> nonTargets) {
		registValue(NON_TARGETS, id, nonTargets);
	}
	
	public void registerNonTargets(String id, String... nonTargets) {
		Set<String> nt = new HashSet<>();
		for (int i = 0; i < nonTargets.length; i++) {
			nt.add(nonTargets[i]);
		}
		registerNonTargets(id, nt);
	}
	
	public void registerVariableNonTargets(String id, Set<String> variableNonTargets) {
		registValue(VAR_NON_TARGETS, id, variableNonTargets);
	}
	
	public void registerVariableNonTargets(String id, String... variableNonTargets) {
		Set<String> nt = new HashSet<>();
		for (int i = 0; i < variableNonTargets.length; i++) {
			nt.add(variableNonTargets[i]);
		}
		registerVariableNonTargets(id, nt);
	}
	
	public void registerVariableNonTargets(String id, List<String> variableNonTargets) {
		Set<String> nt = new HashSet<>();
		nt.addAll(variableNonTargets);
		registerVariableNonTargets(id, nt);
	}
	
	public void registerProductionTarget(String id, String productionTarget) {
		registValue(PRODUCTION_TARGETS, id, productionTarget);
	}
	
	public void registerOptimizationAlgorithm(String id, GenericConfiguration algorithm) {
		if (getAllObjects(OPT_ALGORITHMS).containsKey(id)) {
			logger.warn("[WARNING] - another optimization algorithm with ID [" + id + "] is already registered. Will use this one instead.");
		} else {
			registValue(OPT_ALGORITHMS, id, algorithm);
		}
	}
	
	public void registerSimulationConfiguration(String id, Map<String, Map<String, Object>> conf) {
		if (getAllObjects(OPT_SIMULATION_CONF).containsKey(id)) {
			logger.warn("[WARNING] - another simulation configuration with ID [" + id + "] is already registered. Will use this one instead.");
		} else {
			registValue(OPT_SIMULATION_CONF, id, conf);
		}
	}
	
	public void registerObjectiveFunctionConf(String id, Map<Map<String, Object>, String> conf) {
		if (getAllObjects(OPT_OBJECTIVE_FUNCTION_CONF).containsKey(id)) {
			logger.warn("[WARNING] - another objective function configuration with ID [" + id + "] is already registered. Will use this one instead.");
		} else {
			registValue(OPT_OBJECTIVE_FUNCTION_CONF, id, conf);
		}
	}
	
	public void addRunContainer(String containerId) {
		addRun(CONTAINER, containerId);
	}

	public void addRunEC(String ecId) {
		addRun(ENV_CONDITIONS, ecId);
	}
	
	public void addRunInitGC(String gc) {
		addRun(INIT_GC, gc);
	}
	
	public void addRunNonTargets(String nonTargets) {
		addRun(NON_TARGETS, nonTargets);
	}
	
	public void addRunVarNonTargets(String varNonTargets) {
		addRun(VAR_NON_TARGETS, varNonTargets);
	}
	
	public void addRunProductionTarget(String productionTarget) {
		addRun(PRODUCTION_TARGETS, productionTarget);
	}
	
	public void addRunOptimizationAlgorithm(String optimizationAlgorithm) {
		addRun(OPT_ALGORITHMS, optimizationAlgorithm);
	}
	
	public void addRunSimulationConfiguration(String optimizationAlgorithm) {
		addRun(OPT_SIMULATION_CONF, optimizationAlgorithm);
	}
	
	public void addRunObjectiveFunctionConfiguration(String objectiveFunctionConfiguration) {
		addRun(OPT_OBJECTIVE_FUNCTION_CONF, objectiveFunctionConfiguration);
	}
	
	public void addRunECAt(String nodeId, String ecId) {
		addRun(nodeId, ENV_CONDITIONS, ecId);
	}
	
	
	public void addRunInitGCAt(String nodeId, String obj) {
		addRun(nodeId, INIT_GC, obj);
	}
	
	public void addRunInitGCIn(String nodeId, String ecId) {
		addRunInAllObjects(nodeId, INIT_GC, ecId);
	}
	
	public void addRunProductionTargetAt(String nodeId, String obj) {
		addRun(nodeId, PRODUCTION_TARGETS, obj);
	}
	
	public void addRunOptimizationAlgorithmAt(String nodeId, String obj) {
		addRun(nodeId, OPT_ALGORITHMS, obj);
	}
	
	public void addRunSimulationConfigurationAt(String nodeId, String obj) {
		addRun(nodeId, OPT_SIMULATION_CONF, obj);
	}
	
	public void addRunObjectiveFunctionConfigurationAt(String nodeId, String obj) {
		addRun(nodeId, OPT_OBJECTIVE_FUNCTION_CONF, obj);
	}
	
	public void addRunECIn(String nodeId, String ecId) {
		addRunInAllObjects(nodeId, ENV_CONDITIONS, ecId);
	}
	
	public void addRunNonTargetsIn(String nodeId, String obj) {
		addRunInAllObjects(nodeId, NON_TARGETS, obj);
	}
	
	public void addRunVarNonTargetsIn(String nodeId, String ecId) {
		addRunInAllObjects(nodeId, VAR_NON_TARGETS, ecId);
	}
	
	public void addRunProductionTargetIn(String nodeId, String obj) {
		addRunInAllObjects(nodeId, PRODUCTION_TARGETS, obj);
	}
	
	public void addRunOptimizationAlgorithmIn(String nodeId, String obj) {
		addRunInAllObjects(nodeId, OPT_ALGORITHMS, obj);
	}
	
	public void addRunSimulationConfigurationIn(String nodeId, String obj) {
		addRunInAllObjects(nodeId, OPT_SIMULATION_CONF, obj);
	}
	
	public void addRunObjectiveFunctionConfIn(String nodeId, String obj) {
		addRunInAllObjects(nodeId, OPT_OBJECTIVE_FUNCTION_CONF, obj);
	}
	
	public void addRun(String classId, String objectId) {
		getObject(classId, objectId);
		add(classId, objectId);
		printTree();
	}
	
	public void addRun(String nodeId, String classId, String objectId) {
		getObject(classId, objectId);
		addNodeAt(nodeId, classId, objectId);
		printTree();
	}
	
	public void addRunInAllObjects(String searchId, String classId, String objectId) {
		getObject(classId, objectId);
		getTree().addNodeInObjectId(searchId, classId, objectId);
		printTree();
	}
	
	@SuppressWarnings("unchecked")
	public void addExtraPropertiesToConfiguration(String ecID, String configurationID, Pair<String, ?>... propertyPairs) {
		if (extraConfigurationMemory.containsKey(ecID, configurationID)) {
			for (Pair<String, ?> pair : propertyPairs) {
				extraConfigurationMemory.get(ecID, configurationID).add(pair);
			}
		} else {
			Set<Pair<String, ?>> propSet = new HashSet<>();
			for (Pair<String, ?> pair : propertyPairs) {
				propSet.add(pair);
			}
			extraConfigurationMemory.put(ecID, configurationID, propSet);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addExtraPropertiesToSimulationConfiguration(String ecID, String configurationID, Pair<String, ?>... propertyPairs) {
		if (extraSimulationConfigurationMemory.containsKey(ecID, configurationID)) {
			for (Pair<String, ?> pair : propertyPairs) {
				extraSimulationConfigurationMemory.get(ecID, configurationID).add(pair);
			}
		} else {
			Set<Pair<String, ?>> propSet = new HashSet<>();
			for (Pair<String, ?> pair : propertyPairs) {
				propSet.add(pair);
			}
			extraSimulationConfigurationMemory.put(ecID, configurationID, propSet);
		}
	}

	
	/**
	 * Add the configuration to the tree and ready to run
	 * 
	 * @param convertMap String-String map in which the key is the tree class and the value is the
	 *            object id
	 * @param objectsMap String-Object map in which the key is the object id and the value is the
	 *            object
	 * @return Last node that was added or used in the tree
	 */
	protected INodeTree addConfigurationToRun(Map<String, String> convertMap, Map<String, Object> objectsMap) {
		INodeTree lastInsertedNode = null;
		
		for (String classId : ordv) {
			if (convertMap.containsKey(classId)) {
				String objectId = convertMap.get(classId);
				if (!objectsMap.containsKey(objectId)) {
					lastInsertedNode = addSingleInformationToRun(classId, objectId, lastInsertedNode, convertMap);
				} else {
					logger.warn("Object '" + classId + "' is missing in object map! This can result in population problems.");
				}
			} else {
				
				ArrayList<INodeTree> allEmptyDesc = new ArrayList<>();
				if (lastInsertedNode == null) {
					allEmptyDesc = getAllEmptyDescendentsAtLevel(getTree().getNode(""), classId);
				} else {
					allEmptyDesc = getAllEmptyDescendentsAtLevel(lastInsertedNode, classId);
				}
				
				if (!allEmptyDesc.isEmpty())
					lastInsertedNode = allEmptyDesc.get(0);
			}
		}
		
		return lastInsertedNode;
	}
	
	protected INodeTree addSingleInformationToRun(String classId, String objectId, INodeTree ancestralNode, Map<String, String> convertMap) {
		
		switch (classId) {
			case CONTAINER:
				// Check if tree already has objectId at the container level
				// In the case of the container only one can exist
				LinkedHashSet<INodeTree> containerTwinNodes = hasTwinNodes(classId, objectId);
				if (containerTwinNodes == null || containerTwinNodes.isEmpty()) {
					addRunContainer(objectId);
				} else {
					logger.debug("Found container twin: " + containerTwinNodes.iterator().next().getId());
					return containerTwinNodes.iterator().next();
				}
				
				logger.debug("Add: " + objectId + " in root");
				// If the add returned the INodeTree it helped to skip this line
				containerTwinNodes = hasTwinNodes(classId, objectId);
				
				return containerTwinNodes.iterator().next();
			
			default:
				// Handle other info
				LinkedHashSet<INodeTree> twinNodes = hasTwinNodes(classId, objectId);
				
				// If there are twins then check if it is not necessary to 
				if (twinNodes != null) {
					for (INodeTree node : twinNodes) {
						
						logger.debug("Object: " + objectId + " has twins: " + node.getId());
						if (areNodesRelated(node, ancestralNode)) {
							logger.debug(node.getId() + " Is relative of: " + objectId);
							
							return node;
						}
						logger.debug(node.getId() + " Is not relative of: " + objectId);
					}
				}
				
				if (twinNodes == null || twinNodes.isEmpty()) {
					// If it does not has twins then is new in the tree 
					// Add to run in the ancestral node
					INodeTree addedNode = addNodeInAncestral(classId, objectId, ancestralNode);
					
					return addedNode;
				} else {
					// If it already exists then verify if these twin nodes are linked to the ancestralNode
					
					// If no related nodes were found then it is a new node to be added in this ancestral
					INodeTree addedNode = addNodeInAncestral(classId, objectId, ancestralNode);
					
					return addedNode;
				}
		}
		
	}
	
	protected INodeTree addNodeInAncestral(String classId, String objectId, INodeTree ancestralNode) {
		logger.debug("ADD: " + objectId + " IN: " + ancestralNode.getId() + " ID");
		
		addNodeAt(ancestralNode.getId(), classId, objectId);
		
		LinkedHashSet<INodeTree> allTwins = hasTwinNodes(classId, objectId);
		
		for (INodeTree twin : allTwins) {
			if (areNodesRelated(twin, ancestralNode)) {
				return twin;
			}
		}
		
		return null;
	}
	
	protected void addInfoToEcMemoryMap(Object toAdd) {
		Map<String, String> toPut = (Map<String, String>) toAdd;
		ecMemory.putAll(toPut);
	}
	
	protected void addInfoToBioMemoryMap(Object toAdd) {
		Map<String, String> toPut = (Map<String, String>) toAdd;
		bioMemory.putAll(toPut);
	}
	
	protected void addInfoToSwapsMemoryMap(Object toAdd) {
		Map<String, Map<String, List<String>>> toPut = (Map<String, Map<String, List<String>>>) toAdd;
		swapsMemory.putAll(toPut);
	}
	
	protected void addInfoToNonTargetsMemoryMap(Object toAdd) {
		MultiKeyMap<String, Set<String>> toPut = (MultiKeyMap<String, Set<String>>) toAdd;
		nonTargetsMemory.putAll(toPut);
	}
	
	protected void addInfoToExtraConfigurationMemoryMap(Object toAdd) {
		MultiKeyMap<String, Set<Pair<String, ?>>> toPut = (MultiKeyMap<String, Set<Pair<String, ?>>>) toAdd;
		extraConfigurationMemory.putAll(toPut);
	}
	
	protected void addInfoToExtraSimulationConfigurationMemoryMap(Object toAdd) {
		MultiKeyMap<String, Set<Pair<String, ?>>> toPut = (MultiKeyMap<String, Set<Pair<String, ?>>>) toAdd;
		extraSimulationConfigurationMemory.putAll(toPut);
	}
	
	protected boolean registerSingleInformation(String classId, String valueId, Object value, boolean forceOverride) {
		switch (classId) {
			case CONTAINER:
				Container container = (Container) value;
				ContainerEnv containerEnv = new ContainerEnv(container);
				
				if (!getAllContainers().containsKey(valueId)) {
					registerContainer(valueId, containerEnv);
					logger.info("Regist " + classId + " with name " + valueId);
					return true;
				} else {
					logger.warn("Already exists a " + classId + " with name " + valueId);
					if (forceOverride) {
						getAllContainers().put(valueId, containerEnv);
						return true;
					}
					return false;
				}
				
			default:
				// Handle other info
				if (!getAllObjects(classId).containsKey(valueId)) {
					registValue(classId, valueId, value);
					logger.info("Regist " + classId + " with name " + valueId);
					return true;
				} else {
					logger.warn("Already exists a " + classId + " with name " + valueId);
					if (forceOverride) {
						getAllObjects(classId).put(valueId, value);
						return true;
					}
					return false;
				}
		}
	}
	
	protected void addNodeInTree(String classId, String valueId, Object value) {
		TreeMap<Integer, INodeTree> order = getTree().getOrdered();
		logger.debug(order.size() + "");
		
	}
	
	// ---------------------------- //  ---------------------------- //
	
	// ---------------------------- GETTERS ---------------------------- //
	
	public Map<String, Object> getAllContainers() {
		return getAllObjects(AnalysisEnv.CONTAINER);
	}
	
	public Map<String, Object> getAllNonTargets() {
		return getAllObjects(AnalysisEnv.NON_TARGETS);
	}
	
	public Map<String, Object> getAllVariableNonTargets() {
		return getAllObjects(AnalysisEnv.VAR_NON_TARGETS);
	}
	
	public Map<String, Object> getAllEnvConditions() {
		return getAllObjects(AnalysisEnv.ENV_CONDITIONS);
	}
	
	public Map<String, Object> getAllGeneConditions() {
		return getAllObjects(AnalysisEnv.INIT_GC);
	}
	
	public Map<String, Object> getAllProductionTargets() {
		return getAllObjects(AnalysisEnv.PRODUCTION_TARGETS);
	}
	
	public Map<String, Object> getAllOptAlgorithms() {
		return getAllObjects(AnalysisEnv.OPT_ALGORITHMS);
	}
	
	public Map<String, Object> getAllOptSimulationConfig() {
		return getAllObjects(AnalysisEnv.OPT_SIMULATION_CONF);
	}
	
	public Map<String, Object> getAllOptObjectiveFunctionConfig() {
		return getAllObjects(AnalysisEnv.OPT_OBJECTIVE_FUNCTION_CONF);
	}
	
	// --------------- // --------------- //
	
	// ---------------------------- Configurations ---------------------------- //
	
	public void addPropertyToConfigurationMap(Map<String, Object> configuration, String propertyID, Object propertyValue) {
		if (configuration.containsKey(propertyID)) {
			logger.warn("Property " + propertyID + " already exists! Replace: " + configuration.get(propertyID));
		}
		configuration.put(propertyID, propertyValue);
	}
	
	public void removePropertyToConfigurationMap(Map<String, Object> configuration, String propertyID) {
		configuration.remove(propertyID);
	}
	
	public void addPropertyToOptimizationConfiguration(GenericConfiguration config, String propertyID, Object propertyValue) {
		if (config.getPropertyMap().containsKey(propertyID)) {
			logger.warn("Property " + propertyID + " already exists! Replace: " + config.getPropertyMap().get(propertyID));
		}
		config.getPropertyMap().put(propertyID, propertyValue);
	}
	
	public void removePropertyToOptimizationConfiguration(GenericConfiguration config, String propertyID) {
		config.getPropertyMap().remove(propertyID);
	}
	
	public Map<Map<String, Object>, String> generateAssociationBetweenOptObjFuncAndSimulationConfig(Map<String, Object> optObjectiveFunction, String simulationConfigID) {
		Map<Map<String, Object>, String> toRet = new HashMap<>();
		toRet.put(optObjectiveFunction, simulationConfigID);
		return toRet;
	}
	
	public void associateOptObjFuncAndSimulationConfig(Map<Map<String, Object>, String> associationMap, Map<String, Object> optObjectiveFunction, String simulationConfigID) {
		if (associationMap == null) {
			associationMap = new HashMap<>();
		}
		associationMap.put(optObjectiveFunction, simulationConfigID);
	}
	
	// Simulation Configuration //
	public Map<String, Map<String, Object>> generateSimulationConfiguration(String simulationID, Map<String, Object> singleSimulationConfiguration) {
		Map<String, Map<String, Object>> toRet = new HashMap<String, Map<String, Object>>();
		toRet.put(simulationID, singleSimulationConfiguration);
		return toRet;
	}
	
	public void addSingleConfigurationToSimulationConfiguration(Map<String, Map<String, Object>> configuration, String simulationID, Map<String, Object> singleSimulationConfiguration) {
		if (configuration.containsKey(simulationID)) {
			logger.warn("[WARNING] - another simulation configuration with ID [" + simulationID + "] is already registered. Will use this one instead.");
		}
		configuration.put(simulationID, singleSimulationConfiguration);
	}
	
	public Map<String, Object> generateSingleSimulationConfiguration(String methodID, boolean isMaximization, EnvironmentalConditions ec, boolean isOverUnderSim, boolean isOverUnder2Step) {
		HashMap<String, Object> toRet = new HashMap<>();
		addPropertyToConfigurationMap(toRet, SimulationProperties.METHOD_ID, methodID);
		addPropertyToConfigurationMap(toRet, SimulationProperties.IS_MAXIMIZATION, isMaximization);
		addPropertyToConfigurationMap(toRet, SimulationProperties.ENVIRONMENTAL_CONDITIONS, ec);
		addPropertyToConfigurationMap(toRet, SimulationProperties.IS_OVERUNDER_SIMULATION, isOverUnderSim);
		addPropertyToConfigurationMap(toRet, SimulationProperties.OVERUNDER_2STEP_APPROACH, isOverUnder2Step);
		return toRet;
	}
	
	public Map<String, Object> generateSingleSimulationConfiguration(String methodID, boolean isMaximization) {
		HashMap<String, Object> toRet = new HashMap<>();
		addPropertyToConfigurationMap(toRet, SimulationProperties.METHOD_ID, methodID);
		addPropertyToConfigurationMap(toRet, SimulationProperties.IS_MAXIMIZATION, isMaximization);
		return toRet;
	}
	
	public void addMethodToConfiguration(Map<String, Object> configuration, String methodID) {
		addPropertyToConfigurationMap(configuration, SimulationProperties.METHOD_ID, methodID);
	}
	
	public void addIsMaximizationToConfiguration(Map<String, Object> configuration, boolean isMaximization) {
		addPropertyToConfigurationMap(configuration, SimulationProperties.IS_MAXIMIZATION, isMaximization);
	}
	
	public void addEnvConditionToConfiguration(Map<String, Object> configuration, EnvironmentalConditions ec) {
		addPropertyToConfigurationMap(configuration, SimulationProperties.ENVIRONMENTAL_CONDITIONS, ec);
	}
	
	public void addIsOverUnderToConfiguration(Map<String, Object> configuration, boolean isOverUnderSim) {
		addPropertyToConfigurationMap(configuration, SimulationProperties.IS_OVERUNDER_SIMULATION, isOverUnderSim);
	}
	
	public void addIsOverUnder2StepToConfiguration(Map<String, Object> configuration, boolean isOverUnder2Step) {
		addPropertyToConfigurationMap(configuration, SimulationProperties.OVERUNDER_2STEP_APPROACH, isOverUnder2Step);
	}
	
	public void removeMethodToConfiguration(Map<String, Object> configuration) {
		removePropertyToConfigurationMap(configuration, SimulationProperties.METHOD_ID);
	}
	
	public void removeIsMaximizationToConfiguration(Map<String, Object> configuration) {
		removePropertyToConfigurationMap(configuration, SimulationProperties.IS_MAXIMIZATION);
	}
	
	public void removeEnvConditionToConfiguration(Map<String, Object> configuration) {
		removePropertyToConfigurationMap(configuration, SimulationProperties.ENVIRONMENTAL_CONDITIONS);
	}
	
	public void removeIsOverUnderToConfiguration(Map<String, Object> configuration) {
		removePropertyToConfigurationMap(configuration, SimulationProperties.IS_OVERUNDER_SIMULATION);
	}
	
	public void removeIsOverUnder2StepToConfiguration(Map<String, Object> configuration) {
		removePropertyToConfigurationMap(configuration, SimulationProperties.OVERUNDER_2STEP_APPROACH);
	}
	
	public Map<String, Object> cloneSimulationConfiguration(Map<String, Object> baseConfig) {
		HashMap<String, Object> toRet = new HashMap<String, Object>();
		for (String s : baseConfig.keySet()) {
			toRet.put(s, baseConfig.get(s));
		}
		return toRet;
	}
	// --- //
	
	// Optimization Algorithm Configuration //
	
	public GenericConfiguration generateOptimizationAlgorithmConfiguration(String optStrategy, String optAlgorithm) {
		GenericConfiguration toRet = new GenericConfiguration();
		toRet.setProperty(GenericOptimizationProperties.OPTIMIZATION_STRATEGY, optStrategy);
		toRet.setProperty(GenericOptimizationProperties.OPTIMIZATION_ALGORITHM, optAlgorithm);
		return toRet;
	}
	
	public void addMaxSetSizeToOptimizationConfiguration(GenericConfiguration config, Integer maxSetSize) {
		addPropertyToOptimizationConfiguration(config, GenericOptimizationProperties.MAX_SET_SIZE, maxSetSize);
	}
	
	public void addMinSetSizeToOptimizationConfiguration(GenericConfiguration config, Integer minSetSize) {
		addPropertyToOptimizationConfiguration(config, GenericOptimizationProperties.MIN_SET_SIZE, minSetSize);
	}
	
	public void addIsVariableSizeGenomeToOptimizationConfiguration(GenericConfiguration config, boolean isVariableSize) {
		addPropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.IS_VARIABLE_SIZE_GENOME, isVariableSize);
	}
	
	public void addIsGeneOptimizationToOptimizationConfiguration(GenericConfiguration config, boolean isGeneOptimization) {
		addPropertyToOptimizationConfiguration(config, GenericOptimizationProperties.IS_GENE_OPTIMIZATION, isGeneOptimization);
	}
	
	public void addIsOverUnderExpressionToOptimizationConfiguration(GenericConfiguration config, boolean isOverUnderExp) {
		addPropertyToOptimizationConfiguration(config, GenericOptimizationProperties.IS_OVER_UNDER_EXPRESSION, isOverUnderExp);
	}
	
	public void addOverUnderRangeToOptimizationConfiguration(GenericConfiguration config, Integer min, Integer max) {
		addPropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.OU_RANGE, new Pair<Integer, Integer>(min, max));
	}
	
	public void addNumFunctionEvalCriteriaToOptimizationConfiguration(GenericConfiguration config, Integer numFuncEval) throws InvalidTerminationCriteriaParameter {
		addPropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.TERMINATION_CRITERIA, new NumFunctionEvaluationsListenerHybridTerminationCriteria(numFuncEval));
	}
	
	public void addNumIterationCriteriaToOptimizationConfiguration(GenericConfiguration config, Integer numIterations) throws InvalidNumberOfIterationsException {
		addPropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.TERMINATION_CRITERIA, new IterationListenerHybridTerminationCriteria(numIterations));
	}
	
	public void removeMaxSetSizeToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, GenericOptimizationProperties.MAX_SET_SIZE);
	}
	
	public void removeMinSetSizeToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, GenericOptimizationProperties.MIN_SET_SIZE);
	}
	
	public void removeIsVariableSizeGenomeToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.IS_VARIABLE_SIZE_GENOME);
	}
	
	public void removeIsGeneOptimizationToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, GenericOptimizationProperties.IS_GENE_OPTIMIZATION);
	}
	
	public void removeIsOverUnderExpressionToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, GenericOptimizationProperties.IS_OVER_UNDER_EXPRESSION);
	}
	
	public void removedOverUnderRangeToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.OU_RANGE);
	}
	
	public void removeNumFunctionEvalCriteriaToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.TERMINATION_CRITERIA);
	}
	
	public void removeNumIterationCriteriaToOptimizationConfiguration(GenericConfiguration config) {
		removePropertyToOptimizationConfiguration(config, JecoliOptimizationProperties.TERMINATION_CRITERIA);
	}
	
	public GenericConfiguration cloneOptimizationConfiguration(GenericConfiguration baseConfig) {
		GenericConfiguration toRet = new GenericConfiguration(baseConfig.getPropertyMapCopy());
		return toRet;
	}
	
	// --- //
	
	// Optimization Objective Function Configuration //
	
	public Map<String, Object> generateSingleObjectiveFunctionConfiguration(String objFuncID) {
		HashMap<String, Object> toRet = new HashMap<>();
		addPropertyToConfigurationMap(toRet, AbstractObjectiveFunction.OBJECTIVE_FUNCTION_ID, objFuncID);
		return toRet;
	}
	
	// --- //
	
	// Environmental Conditions //
	
	public EnvironmentalConditions generateEnvironmentalCondition(String reactionID, double lowerBound, double upperBound) {
		EnvironmentalConditions toRet = new EnvironmentalConditions();
		toRet.addReactionConstraint(reactionID, new ReactionConstraint(lowerBound, upperBound));
		return toRet;
	}
	
	public EnvironmentalConditions generateEnvironmentalCondition() {
		EnvironmentalConditions toRet = new EnvironmentalConditions();
		return toRet;
	}
	
	public void addReactionConstraintToEnvironmentalCondition(EnvironmentalConditions envCond, String reactionID, double lowerBound, double upperBound) {
		if (envCond == null) {
			envCond = new EnvironmentalConditions();
		}
		envCond.addReactionConstraint(reactionID, new ReactionConstraint(lowerBound, upperBound));
	}
	
	protected String createDirectoryStructure(String baseDirectory, Map<String, String> convert) throws Exception {
		String incrementalPath = baseDirectory;
		
		for (String node : getOrderedValues()) {
			String elem = convert.get(node);
			if (elem != null) {
				incrementalPath += elem + ClusterConstants.DASH;
				Path iopath = new File(incrementalPath).toPath();
				boolean exists = Files.exists(iopath, LinkOption.NOFOLLOW_LINKS);
				if (!exists) {
					logger.debug("Directory [" + incrementalPath + "] inexistent. Creating...");
					Files.createDirectory(iopath);
				}
			}
		}
		
		return incrementalPath;
	}
	
	protected String getBaseName(Map<String, String> convert) {
		
		List<String> tags = new ArrayList<String>();
		for (String node : getOrderedValues()) {
			String elem = convert.get(node);
			if (elem != null)
				tags.add(elem);
		}
		
		return "resultSet#[" + StringUtils.concat(Delimiter.COMMA.toString(), tags) + "]";
	}
	
	// ---------------------------- Info Prints ---------------------------- //
	
	public void printAllRegisteredNodeObjIDs(String classID) {
		Map<String, Object> map = getAllObjects(classID);
		System.out.println(map.keySet());
	}
	
	public void printAllRegisteredContainerIDs() {
		printAllRegisteredNodeObjIDs(CONTAINER);
	}
	
	public void printAllRegisteredPathwayIDs() {
		printAllRegisteredNodeObjIDs(PATHWAYS);
	}
	
	public void printAllRegisteredEnvConditionsIDs() {
		printAllRegisteredNodeObjIDs(ENV_CONDITIONS);
	}
	
	public void printAllRegisteredGenConditionsIDs() {
		printAllRegisteredNodeObjIDs(INIT_GC);
	}
	
	public void printAllRegisteredNonTargetsIDs() {
		printAllRegisteredNodeObjIDs(NON_TARGETS);
	}
	
	public void printAllRegisteredVariableNonTargetsIDs() {
		printAllRegisteredNodeObjIDs(VAR_NON_TARGETS);
	}
	
	public void printAllRegisteredProductionTargetsIDs() {
		printAllRegisteredNodeObjIDs(PRODUCTION_TARGETS);
	}
	
	public void printAllRegisteredAlgorithmsConfigurationIDs() {
		printAllRegisteredNodeObjIDs(OPT_ALGORITHMS);
	}
	
	public void printAllRegisteredSimulationConfigurationIDs() {
		printAllRegisteredNodeObjIDs(OPT_SIMULATION_CONF);
	}
	
	public void printAllRegisteredObjectiveFunctionConfigurationIDs() {
		printAllRegisteredNodeObjIDs(OPT_OBJECTIVE_FUNCTION_CONF);
	}
	
	public void printSimulationConfigurationInfo(String simConfigID) {
		Map<String, Map<String, Object>> simMap = (Map<String, Map<String, Object>>) getObject(OPT_SIMULATION_CONF, simConfigID);
		System.out.print("Simulation Configuration in '" + simConfigID + "':");
		MapUtils.prettyPrint(simMap);
	}
	
	public void printOptimizationObjectiveFunctionConfigurationInfo(String optObjFuncConfigID) {
		Map<Map<String, Object>, String> optOfFuncMap = (Map<Map<String, Object>, String>) getObject(OPT_OBJECTIVE_FUNCTION_CONF, optObjFuncConfigID);
		System.out.print("Optimization Obj Func Configuration in '" + optObjFuncConfigID + "':");
		MapUtils.prettyPrint(optOfFuncMap);
	}
	
	public void printOptimizationAlgorithmConfigurationInfo(String optAlgConfigID) {
		IGenericConfiguration configuration = (IGenericConfiguration) getObject(OPT_ALGORITHMS, optAlgConfigID);
		System.out.print("Optimization Algorithm Configuration in '" + optAlgConfigID + "':");
		MapUtils.prettyPrint(configuration.getPropertyMap());
	}
	
	public void printProductionTarget(String productionTargetID) {
		String productionTarget = (String) getObject(PRODUCTION_TARGETS, productionTargetID);
		System.out.println("Production targets in '" + productionTargetID + "':\n" + productionTarget);
	}
	
	public void printEnvironmentalCondition(String envCondID) {
		EnvironmentalConditions ec = (EnvironmentalConditions) getObject(ENV_CONDITIONS, envCondID);
		System.out.println("Env Cond in '" + envCondID + "':\n" + ec);
	}
	
	public void printGeneticCondition(String initGcID) {
		EnvironmentalConditions initGC = (EnvironmentalConditions) getObject(INIT_GC, initGcID);
		System.out.println("Gen Cond in '" + initGcID + "':\n" + initGC);
	}
	
	public void printNonTargets(String nonTargetsID) {
		Set<String> nonTargets = (Set<String>) getObject(NON_TARGETS, nonTargetsID);
		System.out.println("Non Targets in '" + nonTargetsID + "':\n" + nonTargets);
	}
	
	public void printVariableNoTargets(String varNonTargetsID) {
		Set<String> varNonTargets = (Set<String>) getObject(VAR_NON_TARGETS, varNonTargetsID);
		System.out.println("Variable Non Targets in '" + varNonTargetsID + "':\n" + varNonTargets);
	}
	
	public static void prettyPrint(Map<?, ?> map) {
		logger.info("Map:");
		if (map != null)
			for (Object key : map.keySet()) {
				logger.info("\t" + key + ": " + map.get(key));
			}
		else
			logger.info("\t" + null);
	}
	
	public void printNodeInformationByID(String nodeID) {
		String defaultMsg = "No print information for node '" + nodeID + "'";
		
		INodeTree nodeOrig = getTree().getNode(nodeID);
		
		if (!testClass(RegistEnvNode.class, nodeOrig)) {
			System.out.println(defaultMsg);
		} else {
			RegistEnvNode node = (RegistEnvNode) getTree().getNode(nodeID);
			String type = ordv.get(node.getLevel());
			String objID = node.getObjectId();
			
			switch (type) {
				case ENV_CONDITIONS:
					printEnvironmentalCondition(objID);
					break;
				
				case INIT_GC:
					printGeneticCondition(objID);
					break;
				
				case NON_TARGETS:
					printNonTargets(objID);
					break;
				
				case VAR_NON_TARGETS:
					printVariableNoTargets(objID);
					break;
				
				case PRODUCTION_TARGETS:
					printProductionTarget(objID);
					break;
				
				case OPT_ALGORITHMS:
					printOptimizationAlgorithmConfigurationInfo(objID);
					break;
				
				case OPT_SIMULATION_CONF:
					printSimulationConfigurationInfo(objID);
					break;
				
				case OPT_OBJECTIVE_FUNCTION_CONF:
					printOptimizationObjectiveFunctionConfigurationInfo(objID);
					break;
				
				default:
					System.out.println(defaultMsg);
					break;
			}
		}
	}
	
	// ---------------------------- // ---------------------------- //
	
	// -------------- UTILITIES -------------- //
	
	protected void copyFromTo(String baseDirectoryTo, String directoryFrom, Map<String, String> mapConvert) throws Exception {
		String baseFilename = createDirectoryStructure(baseDirectoryTo + "/", mapConvert);
		MultipleExtensionFileFilter filter = new MultipleExtensionFileFilter(".ss");
		File[] files = new File(directoryFrom).listFiles(filter);
		logger.info("Copying from [" + directoryFrom + "]...");
		for (int i = 0; i < files.length; i++) {
			String finalOutputFile = baseFilename + ClusterConstants.DEFAULT_NAME_CONNECTOR + "run" + i + ".ss";
			FileUtils.copy(files[i].getAbsolutePath(), finalOutputFile);
			logger.info("\t [" + i + "]: " + finalOutputFile);
		}
	}
	
	public static Set<String> readCriticalIDsFromFile(String... files) throws Exception {
		Set<String> critical = new HashSet<String>();
		
		for (String criticalFile : files) {
			if (criticalFile != null) {
				System.out.print("[" + criticalFile + "]...");
				FileReader fr = new FileReader(criticalFile);
				BufferedReader br = new BufferedReader(fr);
				
				while (br.ready()) {
					String str = br.readLine().trim();
					critical.add(str);
				}
				
				br.close();
				fr.close();
			}
		}
		
		logger.info("Done reading [" + critical.size() + "] critical IDs");
		
		return critical;
	}
	
	protected static String generateNowDateString() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		String year = String.valueOf(cal.get(Calendar.YEAR));
		String month = String.format("%02d", cal.get(Calendar.MONTH) + 1); // Note: zero based!
		String day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
		String hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
		String minute = String.format("%02d", cal.get(Calendar.MINUTE));
		String second = String.format("%02d", cal.get(Calendar.SECOND));
		
		return year + month + day + "_" + hour + minute + second;
	}
	
	protected static Map<String, IStrainOptimizationResultSet<?, ?>> convertMapToStrainOptResSet(Map<String, Object> objMap) {
		Map<String, IStrainOptimizationResultSet<?, ?>> toRet = new HashMap<>();
		
		for (String key : objMap.keySet()) {
			toRet.put(key, (IStrainOptimizationResultSet<?, ?>) objMap.get(key));
		}
		
		return toRet;
	}
	
	
	public static Map<String, List<String>> getSwapsMap(String swapsFile) throws Exception {
		return getSwapsMap(swapsFile, Delimiter.COMMA.toString());
	}
	
	public static Map<String, List<String>> getSwapsMap(String swapsFile, String delimiter) throws Exception {
		HashMap<String, List<String>> toret = null;
		logger.info("Loading swaps map ");
		if (swapsFile != null && !swapsFile.isEmpty()) {
			toret = new HashMap<String, List<String>>();
			logger.info("[" + swapsFile + "]...");
			BufferedReader br = new BufferedReader(new FileReader(swapsFile));
			
			int i = 0;
			while (br.ready()) {
				String str = br.readLine().trim();
				String tokens[] = str.split(delimiter);
				if (tokens.length < 2) {
					br.close();
					throw new Exception("\nLoading swaps map file [" + swapsFile + "] at line " + i + ". Lines must always contain at least two elements separated by [" + delimiter + "].");
				} else {
					String original = tokens[0].trim();
					List<String> swapList = new ArrayList<String>();
					for (int j = 1; j < tokens.length; j++)
						swapList.add(tokens[j].trim());
					
					toret.put(original, swapList);
				}
				i++;
			}
			
			br.close();
			logger.info("done with " + i + " possible swaps!");
		} else
			logger.info("... not found!");
		
		return toret;
	}
	
	protected Map<String, IRunTreeStatus> convertOriginalResultToState() {
		Map<String, IRunTreeStatus> toRet = new HashMap<>();
		for (String resultID : results.keySet()) {
			toRet.put(resultID, (IRunTreeStatus) results.get(resultID));
		}
		return toRet;
	}
	
	public String generateBranchID(Map<String, String> convert) {
		return generateBranchBase(convert, Delimiter.HASHTAG.toString());
	}
	
	public String generateBranchBase(Map<String, String> convert, String delimiter) {
		List<String> branchIDs = new ArrayList<>();
		for (String ord : getOrderedValues()) {
			String val = convert.get(ord);
			if (val != null) {
				branchIDs.add(val);
			}
		}
		String branchID = StringUtils.concat(delimiter, branchIDs);
		
		return branchID;
	}
	
	public String generateBranchBaseUntil(Map<String, String> convert, String delimiter, int until) {
		List<String> branchIDs = new ArrayList<>();
		int current = 0;
		for (int i = 0; i < getOrderedValues().size() && current < until - 1; i++) {
			String ord = getOrderedValues().get(i);
			String val = convert.get(ord);
			if (val != null) {
				branchIDs.add(val);
				current++;
			}
		}
		String branchID = StringUtils.concat(delimiter, branchIDs);
		
		return branchID;
	}
	
	public String generateBranchBaseIntersect(List<Map<String, String>> converts, String delimiter) {
		
		Map<String, String> ids = null;
		for (Map<String, String> conv : converts) {
			if (ids == null) {
				ids = new HashMap<>(conv);
			} else {
				ids = MapUtils.getMapIntersectionValues(ids, conv);
			}
		}
		
		return generateBranchBase(ids, delimiter);
	}
	
	
	public Map<Integer, Map<String, String>> createConverts() {
		TreeMap<Integer, Pair<String, Map<Integer, String>>> toRun = getTree().getValidatedNodes();
		Map<Integer, Map<String, String>> convertMaps = new HashMap<>();
		
		for (Integer order : toRun.keySet()) {
			Pair<String, Map<Integer, String>> pair = toRun.get(order);
			Map<Integer, String> info = pair.getB();
			Map<String, String> convert = convert(info);
			convertMaps.put(order, convert);
		}
		
		return convertMaps;
	}
	
	public void createFromConfigurationAndLoadResults(String configurationFile, String resultsFromDir, String resultsToDir) throws Exception {
		OptimizationConfiguration conf = new OptimizationConfiguration(configurationFile);
		createFromConfiguration(conf);
		Map<Integer, Map<String, String>> convertMaps = createConverts();
		
		Map<Integer, String> paths = new HashMap<>();
		for (int i = 0; i < conf.getNumberOfStates(); i++) {
			conf.setCurrentState(i);
			List<TreeNode<Pair<String, String>>> path1 = conf.getPossibleStatesPaths().get(i);
			String path1string = convertPathToString(resultsFromDir.endsWith(ClusterConstants.DASH) ? resultsFromDir : resultsFromDir + ClusterConstants.DASH, path1);
			paths.put(i + 1, path1string);
		}
		
		String date = getFormattedCurrentDate();
		
		resultsToDir = resultsToDir + "/" + date + "#[EXT_IMPORT]";
		Path baseDirectoryPath = new File(resultsToDir).toPath();
		if (!Files.exists(baseDirectoryPath, LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.createDirectory(baseDirectoryPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (Integer state : paths.keySet()) {
			copyFromTo(resultsToDir, paths.get(state), convertMaps.get(state));
		}
	}
	
	public static String getFormattedCurrentDate() {
		Calendar cal = Calendar.getInstance();
		String year = String.valueOf(cal.get(Calendar.YEAR));
		String month = String.format("%02d", cal.get(Calendar.MONTH) + 1); // Note: zero based!
		String day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
		String hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
		String minute = String.format("%02d", cal.get(Calendar.MINUTE));
		
		return year + month + day + "_" + hour + minute;
	}
	
	public static String convertPathToString(String baseDir, List<TreeNode<Pair<String, String>>> path) {
		String incrementalPath = baseDir;
		for (TreeNode<Pair<String, String>> node : path) {
			Pair<String, String> elem = node.getElement();
			if (elem != null) {
				incrementalPath += elem.getB() + ClusterConstants.DASH;
			}
		}
		return incrementalPath;
	}
	
	public void createFromConfiguration(String configurationFile) throws Exception {
		OptimizationConfiguration conf = new OptimizationConfiguration(configurationFile);
		createFromConfiguration(conf);
	}
	
	public void createFromConfiguration(OptimizationConfiguration conf) throws Exception {
		
		List<String> containers = new ArrayList<String>();
		List<String> envs = new ArrayList<String>();
		List<String> initGCS = new ArrayList<String>();
		List<String> criticals = new ArrayList<String>();
		List<String> varNonTargets = new ArrayList<String>();
		List<String> products = new ArrayList<String>();
		List<String> algorithms = new ArrayList<String>();
		List<String> simulations = new ArrayList<String>();
		List<String> objectiveFunctions = new ArrayList<String>();
		
		for (int i = 0; i < conf.getNumberOfStates(); i++) {
			conf.setCurrentState(i);
			
			HashMap<String, String> possibleStates = conf.getPossibleStates().get(i);
			
			/**
			 * LOAD CONTAINERS
			 */
			String containerID = possibleStates.get("M_VERSION");
//			containerID = (conf.getAliasesMap().containsKey(containerID)) ? conf.getAliasesMap().get(containerID) : containerID;
			containerID = conf.replaceAliasesMatchesStatewise(containerID, i);
			if (!containers.contains(containerID)) {
				containers.add(containerID);
				Container container = conf.getContainer();
				
				ContainerEnv cEnv = new ContainerEnv(container);
				this.registerContainer(containerID, cEnv);
			}
			
			/**
			 * LOAD ENVIRONMENTAL CONDITIONS
			 */
			String env = possibleStates.get("ENV");
			String sub = possibleStates.get("SUB");
			String envID = sub + "_" + env;
			if (!envs.contains(envID)) {
				envs.add(envID);
				EnvironmentalConditions conditions = conf.getEnvironmentalConditionsOnly();
				String biomass = conf.getModelBiomass();
				String substrate = (conf.getAliasesMap().containsKey(sub)) ? conf.getAliasesMap().get(sub) : sub;
				this.registerEC(envID, substrate, biomass, conditions);
			}
			
			/**
			 * LOAD INITIAL GC
			 */
			String initGCID = possibleStates.get("INIT_GC");
			if (!initGCS.contains(initGCID)) {
				initGCS.add(initGCID);
				EnvironmentalConditions gc = conf.getEnvironmentalConditionsVariation();
				this.registerInitGC(initGCID, gc);
			}
			
			/**
			 * CRITICALS
			 */
			
			String critID = "criticals_" + envID;
			if (!criticals.contains(critID)) {
				criticals.add(critID);
				Set<String> crits = new HashSet<String>();
				crits.addAll(conf.getOptimizationCriticalIDs());
				this.registerNonTargets(critID, crits);
			}
			
			/**
			 * VAR NON TARGETS
			 */
			String varCritID = possibleStates.get("VAR_TARGETS");
			if (!varNonTargets.contains(varCritID)) {
				varNonTargets.add(varCritID);
				List<String> varNT = conf.getOptimizationManualCriticalIDs();
				this.registerVariableNonTargets(varCritID, varNT);
			}
			
			/**
			 * PRODUCTION TARGETS
			 */
			String productID = possibleStates.get("PROD");
			if (!products.contains(productID)) {
				products.add(productID);
				String prod = (conf.getAliasesMap().containsKey(productID)) ? conf.getAliasesMap().get(productID) : productID;
				this.registerProductionTarget(productID, prod);
			}
			
			/**
			 * ALGORITHMS
			 */
			String algorithm = possibleStates.get("ALG");
			String maxmods = possibleStates.get("MAXMODS");
			String strategy = conf.getOptimizationStrategy();
			Integer maxMods = conf.getMaxSize();
			Integer maxSwaps = conf.getMaxSwaps();
			String maxModsString = (maxmods != null) ? "K" + maxmods : null;
			String maxSwapsString = (maxSwaps > 0) ? "S" + maxSwaps : null;
			String algorithmID = StringUtils.concat("_", algorithm + strategy, maxModsString, maxSwapsString);
			if (!algorithms.contains(algorithmID)) {
				algorithms.add(algorithmID);
				boolean varSize = conf.isVariableSize();
				ITerminationCriteria termCriteria = conf.getTerminationCriteria();
				String optimizationStrategy = conf.getOptimizationStrategy();
				String optimizationAlg = conf.getAlgorithm().getShortName();
				boolean isGeneOpt = conf.isGeneBased();
				boolean isOverUnder = conf.isOverUnder();
				Map<String, List<String>> swapsMap = conf.getSwapsMap();
				
				JecoliGenericConfiguration algConf = new JecoliGenericConfiguration();
				algConf.setProperty(GenericOptimizationProperties.MAX_SET_SIZE, maxMods);
				algConf.setProperty(JecoliOptimizationProperties.IS_VARIABLE_SIZE_GENOME, varSize);
				algConf.setProperty(JecoliOptimizationProperties.TERMINATION_CRITERIA, termCriteria);
				algConf.setProperty(GenericOptimizationProperties.OPTIMIZATION_STRATEGY, optimizationStrategy);
				algConf.setProperty(GenericOptimizationProperties.OPTIMIZATION_ALGORITHM, optimizationAlg);
				algConf.setProperty(GenericOptimizationProperties.IS_GENE_OPTIMIZATION, isGeneOpt);
				algConf.setProperty(GenericOptimizationProperties.IS_OVER_UNDER_EXPRESSION, isOverUnder);
				algConf.setProperty(JecoliOptimizationProperties.MAX_ALLOWED_SWAPS, maxSwaps);
				algConf.setProperty(JecoliOptimizationProperties.REACTION_SWAP_MAP, swapsMap);
				
				this.registerOptimizationAlgorithm(algorithmID, algConf);
			}
			
			/**
			 * SIM CONF
			 */
			String simulationID = possibleStates.get("SIM");
			if (!simulations.contains(simulationID)) {
				simulations.add(simulationID);
				Map<String, Map<String, Object>> methodsProperties = new HashMap<String, Map<String, Object>>();
				List<String> simMethods = conf.getSimulationMethod();
				for (String sim : simMethods) {
					Map<String, Object> methodConf = new HashMap<>();
					methodConf.put(SimulationProperties.METHOD_ID, sim);
					methodConf.put(SimulationProperties.IS_MAXIMIZATION, true);
					methodConf.put(SimulationProperties.SOLVER, conf.getSimulationSolver());
					methodConf.put(SimulationProperties.OBJECTIVE_FUNCTION, null);
					methodConf.put(SimulationProperties.MODEL, null);
					methodConf.put(SimulationProperties.ENVIRONMENTAL_CONDITIONS, null);
					methodsProperties.put(sim, methodConf);
				}
				this.registerSimulationConfiguration(simulationID, methodsProperties);
			}
			
			/**
			 * OBJECTIVE FUNCTIONS
			 */
			String objectiveFunctionID = possibleStates.get("OBJFUNC");
			if (!objectiveFunctions.contains(objectiveFunctionID)) {
				objectiveFunctions.add(objectiveFunctionID);
				Map<Map<String, Object>, String> ofConfs = conf.getObjectiveFunctionConfigurations();
				this.registerObjectiveFunctionConf(objectiveFunctionID, ofConfs);
			}
			
		}
		
		for (String cont : containers) {
			addRunContainer(cont);
		}
		
		for (String env : envs) {
			addRunEC(env);
		}
		
		for (String initGC : initGCS) {
			addRunInitGC(initGC);
		}
		
		for (String nonTarg : criticals) {
			addRunNonTargets(nonTarg);
		}
		
		for (String varNonTarg : varNonTargets) {
			addRunVarNonTargets(varNonTarg);
		}
		
		boolean perProduct = false;
		for (String cont : containers) {
			String[] contTokens = cont.split("_", 3);
			if (contTokens.length >= 3) {
				String prodaux = contTokens[2].trim();
				for (String prod : products) {
					if (prod.equalsIgnoreCase(prodaux)) {
						perProduct = true;
						addRunProductionTargetIn(cont, prod);
					}
				}
			}
		}
		
		if (!perProduct) {
			for (String prod : products) {
				addRunProductionTarget(prod);
			}
		}
		
		for (String alg : algorithms) {
			addRunOptimizationAlgorithm(alg);
		}
		
		for (String sim : simulations) {
			addRunSimulationConfiguration(sim);
		}
		
		//PROBLEM ONLY THE FIRST OBJECTIVE FUNCTION IS SET ! SINCE THE OBJECTIVE FUNCTION IS FULLY QUALIFIED (ALL PARAMETERS SET), IT WILL NOT ENTER PROCESS MODE AND OVERRIDE PARAMETERS
		for (String obj : objectiveFunctions) {
			addRunObjectiveFunctionConfiguration(obj);
		}
		
	}
	
	// --------------- // --------------- //
	
	/**
	 * Consider twins if it has same class id and value id.<br>
	 * This means that the tree has the same object in multiple branches.
	 * 
	 * @param classId
	 * @param valueId
	 * @return
	 */
	protected LinkedHashSet<INodeTree> hasTwinNodes(String classId, String valueId) {
		LinkedHashSet<INodeTree> toRet = new LinkedHashSet<>();
		
		LinkedHashSet<INodeTree> allSimilarNodes = getTree().getAllNodeWith(valueId);
		
		if (allSimilarNodes == null || allSimilarNodes.isEmpty()) {
			return null;
		}
		
		int level = getOrderedValues().indexOf(classId);
		
		for (INodeTree similarNode : allSimilarNodes) {
			if (similarNode.getLevel() == level) {
				toRet.add(similarNode);
			}
		}
		
		return toRet;
	}
	
	public boolean areNodesRelated(INodeTree node, INodeTree ancestralNode) {
		ArrayList<INodeTree> allDescendents = new ArrayList<>();
		getDescendents(ancestralNode, allDescendents);
		
		for (INodeTree descendent : allDescendents) {
			if (node.getId().equals(descendent.getId())) {
				return true;
			}
		}
		
		return false;
	}
	
	public ArrayList<INodeTree> getAllDescendentsAtLevel(INodeTree node, int level) {
		ArrayList<INodeTree> toRet = new ArrayList<INodeTree>();
		
		ArrayList<INodeTree> allDescendents = new ArrayList<INodeTree>();
		getDescendents(node, allDescendents);
		
		for (INodeTree desc : allDescendents) {
			if (desc.getLevel() == level) {
				toRet.add(desc);
			}
		}
		
		return toRet;
	}
	
	public ArrayList<INodeTree> getAllDescendentsAtLevel(INodeTree node, String classId) {
		ArrayList<INodeTree> toRet = getAllDescendentsAtLevel(node, getOrderedValues().indexOf(classId));
		
		return toRet;
	}
	
	public ArrayList<INodeTree> getAllEmptyDescendentsAtLevel(INodeTree node, int level) {
		ArrayList<INodeTree> toRet = new ArrayList<INodeTree>();
		
		ArrayList<INodeTree> allDescendentsAtLevel = getAllDescendentsAtLevel(node, level);
		
		for (INodeTree desc : allDescendentsAtLevel) {
			if (desc instanceof BasicNodeTree) {
				toRet.add(desc);
			}
		}
		
		return toRet;
	}
	
	public ArrayList<INodeTree> getAllEmptyDescendentsAtLevel(INodeTree node, String classId) {
		ArrayList<INodeTree> toRet = getAllEmptyDescendentsAtLevel(node, getOrderedValues().indexOf(classId));
		
		return toRet;
	}
	
	public static void getDescendents(INodeTree node, ArrayList<INodeTree> allDescendents) {
		List<INodeTree> children = node.getchildren();
		if (children != null) {
			allDescendents.addAll(children);
			for (INodeTree child : children) {
				getDescendents(child, allDescendents);
			}
		}
	}
	
	public static void getAncestrals(INodeTree node, ArrayList<INodeTree> allAncestrals) {
		INodeTree parent = node.getParent();
		if (parent != null) {
			allAncestrals.add(parent);
			getAncestrals(parent, allAncestrals);
		}
	}

	@Override
	public void loadDefaultConfig() {
		
	}
	
}
