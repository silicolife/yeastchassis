package com.silicolife.yeastchassis.analysis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.silicolife.yeastchassis.env.AnalysisEnv;
import com.silicolife.yeastchassis.env.tasks.AggregateSimplifyTask;
import com.silicolife.yeastchassis.env.tasks.CompileResultsTask;
import com.silicolife.yeastchassis.env.tasks.ChassisTask;

import pt.uminho.ceb.biosystems.mew.core.cmd.searchtools.configuration.OptimizationConfiguration;
import pt.uminho.ceb.biosystems.mew.core.simplification.solutions.SimplifierOptions;
import pt.uminho.ceb.biosystems.mew.core.simulation.components.SimulationProperties;
import pt.uminho.ceb.biosystems.mew.core.simulation.formulations.abstractions.AbstractObjTerm;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ObjectiveFunctionParameterType;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.BPCYObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.CYIELDObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.FVAObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.FluxValueObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.objectivefunctions.ofs.NumKnockoutsObjectiveFunction;
import pt.uminho.ceb.biosystems.mew.solvers.builders.CPLEX3SolverBuilder;
//import pt.uminho.ceb.biosystems.mew.solvers.SolverType;
import pt.uminho.ceb.biosystems.mew.solvers.lp.CplexParamConfiguration;

public class Analyze_20210323_acids {
	
	public static final String  baseDir         = "data/20170313_acids_GK_IMPORT";
	public static final String	baseDirTargets	= baseDir + "/targets/";
	public static final String	envDir			= baseDir + "/env";
	
	public static final String	envSerialized	= envDir + "/import_20170313_acids_GK_IMPORT.env";

	
	/**
	 * 1st step
	 * 
	 * Import the optimization results, create a new analysis environment and
	 * serialize it.
	 */
//	@Test //uncomment if you wish to run this step
	public void importAndSaveEnv() throws Exception {
		
		AnalysisEnv env = new AnalysisEnv();
//		env.createFromConfigurationAndLoadResults(baseDir+"/configuration/20170313_iMM904_aerobic_IMPORT_current.conf", baseDirTargets, baseDir);
		env.createFromConfiguration(new OptimizationConfiguration(baseDir+"/configuration/20170313_iMM904_aerobic_IMPORT_current.conf"));
		env.saveSerializedObject(envSerialized);
	}
	
	/**
	 * 2nd step
	 * 
	 * Aggregate the results (for each state, aggregate the results from multiple
	 * runs in a single file), remove repetitions and simplify the solutions
	 * according to some parameters.
	 * 
	 */
//	@Test //uncomment if you wish to run this step
	public void aggregateSimplifyTest() throws Throwable {

		/**
		 * load the previously serialized environment
		 */
		AnalysisEnv env = AnalysisEnv.loadSerializedObject(envSerialized);
		env.printTree();

		/**
		 * create a new override objective function: this will be used as the objective
		 * function for simplification purposes. Multiple objective functions can be
		 * specified in these map
		 */
		Map<Map<String,Object>,String> overrideObjectiveFunctions = new HashMap<>();
		Map<String,Object> cyield_configuration = new HashMap<>();
		cyield_configuration.put(CYIELDObjectiveFunction.OBJECTIVE_FUNCTION_ID, CYIELDObjectiveFunction.ID);		
		overrideObjectiveFunctions.put(cyield_configuration,"FBA"); //carbon yield will be calculated based on the results of the FBA simulation

		/**
		 * specify simplifier options: in this example, we are setting the minimum
		 * percentage of the simplified solution value to be 90% that of the original
		 * (meaning we are allowing the solutions to lose 10% of their value for
		 * simplification purposes)
		 */
		Map<String,Object> simplifierOptions = new HashMap<>();
		simplifierOptions.put(SimplifierOptions.MIN_PERCENT_PER_OBJFUNC, new Double[]{0.9});
		
		/**
		 * Create our simplification and aggregation task and execute it specify the
		 * location of the previously imported results (folder structure generate in the
		 * previous step)
		 */
		AggregateSimplifyTask aggregateSimplify = new AggregateSimplifyTask(baseDir+"/20170313_1609#[EXT_IMPORT]", env, false);
		aggregateSimplify.setOverrideObjectiveFunctionsConfiguration(overrideObjectiveFunctions);
//		aggregateSimplify.setSimplifierOptions(simplifierOptions); // not actually using the simplifier options set before (not allowing to lose any cyield during simplification)
		aggregateSimplify.run();
		
//		env.saveSerializedObject(envSerialized);
	}
	
	/**
	 * 3rd step
	 * 
	 * Compile the results of the aggregate and simplified results in a single file.
	 * In here we can specify multiple objective functions (metrics) to be
	 * calculated that can be used further.
	 */
//	@Test //uncomment if you wish to run this step
	public void compileResultsTest() throws Throwable {
		AnalysisEnv env = AnalysisEnv.loadSerializedObject(envSerialized);
		env.printTree();
		
		/**
		 * specify one or more simulation methods to use during this stage. Each
		 * solution will be simulated for each method. In this example we are only
		 * setting up pFBA (parsimonious flux balance analysis) note: null values will
		 * be automatically filled by the task, depending on the state.
		 */
		Map<String,Map<String,Object>> overrideSimulationConfiguration = getOverrideSimulationMap();
		
		/**
		 * specify objective functions (metrics) to calculate for each solution,
		 * and specify which simulation method's result should be used during that calculation
		 */
		Map<Map<String, Object>, String> overrideObjectiveFunctions = getOverrideObjectiveFunctionMap();
						
		/**
		 * Create the compile task, and execute it
		 * specify the location of the previously imported results (folder structure generate in the previous step)
		 */
		CompileResultsTask compileTask = new CompileResultsTask(baseDir+"/20170313_1609#[EXT_IMPORT]", env, null);
		compileTask.setMapping(AnalysisEnv.OPT_ALGORITHMS); 						//set mapping variable (in this case, by setting to OPT_ALGORITHMS, results from multiple algorithms will be compiled together in the same file) - not required because we only used SPEA2
		compileTask.setOverrideIfExistent(true); 											//override previous files 
		compileTask.setIgnoreOFifZero(new int[]{4}); 										// discard solutions where of 4 (BPCY) is zero.
		compileTask.setOverrideSimulationConfigurations(overrideSimulationConfiguration); 	//load pFBA
		compileTask.setOverrideObjectiveFunctionsConfiguration(overrideObjectiveFunctions); 				//load the 8 ofs (metrics)
		compileTask.run();
//		env.saveSerializedObject(envSerialized);
	}
	
	
	/**
	 * 4th step
	 * 
	 * Analyse results and generate chassis
	 */
	@Test
	public void analyseAndGenerateChassis() throws Throwable{
		
		AnalysisEnv env = AnalysisEnv.loadSerializedObject(envSerialized);
		env.printTree();
				
		/**
		 * specify one or more simulation methods to use during this stage.
		 * Each solution will be simulated for each method.
		 * In this example we are only setting up pFBA (parsimonious flux balance analysis)
		 * note: null values will be automatically filled by the task, depending on the state.
		 */
		Map<String,Map<String,Object>> overrideSimulationConfiguration = getOverrideSimulationMap();
		
		/**
		 * specify objective functions (metrics) to calculate for each solution,
		 * and specify which simulation method's result should be used during that calculation
		 */
		Map<Map<String, Object>, String> overrideObjectiveFunctions = getOverrideObjectiveFunctionMap();

		/**
		 * create the chassis task and execute it
		 * 
		 * note: for heatmap generation, an R environment needs to be available. 
		 * check "com/silicolife/yeastchassis/env/tasks/r/REnvironment" to check 
		 * required libraries and specify the location of the R executable.  
		 */
		ChassisTask mostFrequent = new ChassisTask(baseDir+"/20170313_1609#[EXT_IMPORT]", env ,3, 3); //generate chassis of size 3 to 3. the first number is the minimum size of the chassis and the last is the maximum - larger sizes can results in combinatorial explosion
		mostFrequent.setOverrideSimulationConfigurations(overrideSimulationConfiguration);
		mostFrequent.setOverrideObjectiveFunctionsConfiguration(overrideObjectiveFunctions);
		mostFrequent.setMapping(AnalysisEnv.PRODUCTION_TARGETS); 					//map everything at the PRODUCTION_TARGET variable, i.e., analysis will be centered on the targets 
		mostFrequent.setBaseUrl("https://www.yeastgenome.org/locus/"); 				// base url for SGD database locus (to generate links)
		mostFrequent.setObjectiveFunctionIndexes(new int[] {0, 5, 6}); 				// use these objective functions (0 - nkos, 5 - cyield, 6 - fva min)
		mostFrequent.setObjectiveFunctionForScoring(5); 							// when filtering for top solutions consider of 5 (cyield)
		mostFrequent.considerTopScoreSolutions(0.3); 								// only consider the 30% top solutions (by cyield) - for selection of most frequent genes before chassis generation
		mostFrequent.addFilterAllowSolutions(5, 0.95, true, true); 					// discard solutions with cyield below 95% of the maximum
		mostFrequent.setStandard2systematic(baseDir + "/model/GIDS.csv", false); 	//map for conversion of standard to systematic gene IDs
		mostFrequent.setSaveGeneFrequencyTableAndHeatMap(true); 					//save a gene frequency table and a clustering heatmap 
		mostFrequent.setMaxSolutionsPerVariable(20); 								// maximum number or solutions to present for each target in each chassis
		mostFrequent.setDescription("Original Acids chassis (pFBA)");
		mostFrequent.run();
		
		env.saveSerializedObject(envSerialized);
	}
	
	
	/**
	 * Solver configurations (CPLEX)
	 */
	@BeforeClass
	public static void before() {
		AbstractObjTerm.setMaxValue(Double.MAX_VALUE);
		AbstractObjTerm.setMinValue(-Double.MAX_VALUE);
		CplexParamConfiguration.setDoubleParam("EpRHS", 1e-9);
		CplexParamConfiguration.setDoubleParam("TiLim", 3.0d);
		CplexParamConfiguration.setIntegerParam("MIPEmphasis", 2);
		CplexParamConfiguration.setBooleanParam("NumericalEmphasis", true);
		CplexParamConfiguration.setBooleanParam("PreInd", true);
		CplexParamConfiguration.setIntegerParam("HeurFreq", -1);
	}
	
	/**
	 * override objective functions
	 */
	private static final Map<Map<String, Object>, String> getOverrideObjectiveFunctionMap() {
		
		Map<Map<String, Object>, String> ofMapExtra = new LinkedHashMap<>();
		
		/**
		 * 0 - number of knockouts
		 */
		Map<String, Object> nkConf = new HashMap<>();
		nkConf.put(NumKnockoutsObjectiveFunction.OBJECTIVE_FUNCTION_ID, NumKnockoutsObjectiveFunction.ID);
		nkConf.put(NumKnockoutsObjectiveFunction.NK_PARAM_MAXIMIZATION, false);
		ofMapExtra.put(nkConf, SimulationProperties.PFBA);
		
		/**
		 * 1 - biomass flux value
		 */
		Map<String, Object> fvBioConf = new HashMap<>();
		fvBioConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvBioConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvBioConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		ofMapExtra.put(fvBioConf, SimulationProperties.PFBA);
				
		/**
		 * 2 - target flux value
		 */
		Map<String, Object> fvProdConf = new HashMap<>();
		fvProdConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvProdConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvProdConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		ofMapExtra.put(fvProdConf, SimulationProperties.PFBA);
		
		/**
		 * 3 - substrate flux value
		 */
		Map<String, Object> fvSubConf = new HashMap<>();
		fvSubConf.put(FluxValueObjectiveFunction.OBJECTIVE_FUNCTION_ID, FluxValueObjectiveFunction.ID);
		fvSubConf.put(FluxValueObjectiveFunction.FV_PARAM_MAXIMIZATION, true);
		fvSubConf.put(FluxValueObjectiveFunction.FV_PARAM_REACTION, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		ofMapExtra.put(fvSubConf, SimulationProperties.PFBA);
		
		/**
		 * 4 - biomass-product coupled yield
		 * BPCY= (v_biomass * v_product) / v_substrate (Patil et al, 2005)
		 */
		Map<String, Object> bpcyConf = new HashMap<>();
		bpcyConf.put(BPCYObjectiveFunction.OBJECTIVE_FUNCTION_ID, BPCYObjectiveFunction.ID);
		bpcyConf.put(BPCYObjectiveFunction.BPCY_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		bpcyConf.put(BPCYObjectiveFunction.BPCY_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		bpcyConf.put(BPCYObjectiveFunction.BPCY_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		ofMapExtra.put(bpcyConf, SimulationProperties.PFBA);
		
		/**
		 * 5 - carbon yield
		 * CYield = (C_product * v_product) / (C_substrate * v_substrate)
		 */
		Map<String, Object> cyieldConf = new HashMap<>();
		cyieldConf.put(CYIELDObjectiveFunction.OBJECTIVE_FUNCTION_ID, CYIELDObjectiveFunction.ID);
		cyieldConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		cyieldConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_SUBSTRATE, ObjectiveFunctionParameterType.REACTION_SUBSTRATE);
		cyieldConf.put(CYIELDObjectiveFunction.CYIELD_PARAM_CONTAINER, ObjectiveFunctionParameterType.CONTAINER);
		ofMapExtra.put(cyieldConf, SimulationProperties.PFBA);
		
		/**
		 * 6 - FVA min for the product (minimum flux value, when biomass value is set as a constraint) - measure of robustness
		 */
		Map<String, Object> fvaMinConf = new HashMap<>();
		fvaMinConf.put(FVAObjectiveFunction.OBJECTIVE_FUNCTION_ID, FVAObjectiveFunction.ID);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_MAXIMIZATION, false);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		fvaMinConf.put(FVAObjectiveFunction.FVA_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		ofMapExtra.put(fvaMinConf, SimulationProperties.PFBA);
		
		/**
		 * 7 - FVA max for the product (maximum flux value, when biomass value is set as a constraint)
		 */
		Map<String, Object> fvaMaxConf = new HashMap<>();
		fvaMaxConf.put(FVAObjectiveFunction.OBJECTIVE_FUNCTION_ID, FVAObjectiveFunction.ID);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_MAXIMIZATION, true);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_BIOMASS, ObjectiveFunctionParameterType.REACTION_BIOMASS);
		fvaMaxConf.put(FVAObjectiveFunction.FVA_PARAM_PRODUCT, ObjectiveFunctionParameterType.REACTION_PRODUCT);
		ofMapExtra.put(fvaMaxConf, SimulationProperties.PFBA);
		
		return ofMapExtra;
	}
	
	/**
	 * override simulation methods
	 */
	private static final Map<String, Map<String, Object>> getOverrideSimulationMap() {
		Map<String,Map<String,Object>> overrideSimulationConfiguration = new HashMap<String,Map<String,Object>>();
		
		Map<String,Object> fbaConf = new HashMap<>();
		fbaConf.put(SimulationProperties.METHOD_ID, SimulationProperties.PFBA);
		fbaConf.put(SimulationProperties.IS_MAXIMIZATION, true);
		fbaConf.put(SimulationProperties.SOLVER, CPLEX3SolverBuilder.ID);
		fbaConf.put(SimulationProperties.OBJECTIVE_FUNCTION, null);
		fbaConf.put(SimulationProperties.MODEL, null);
		fbaConf.put(SimulationProperties.ENVIRONMENTAL_CONDITIONS, null);
		overrideSimulationConfiguration.put(SimulationProperties.PFBA, fbaConf);
		
		return overrideSimulationConfiguration;
	}
}
