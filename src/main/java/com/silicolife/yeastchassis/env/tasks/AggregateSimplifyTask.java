package com.silicolife.yeastchassis.env.tasks;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.silicolife.yeastchassis.env.AnalysisEnv;

import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.IStrainOptimizationResult;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.IStrainOptimizationResultSet;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.simplification.IStrainOptimizationResultsSimplifier;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.simplification.StrainOptimizationSimplificationFactory;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.optimizationresult.solutionset.ResultSetFactory;
import pt.uminho.ceb.biosystems.mew.core.strainoptimization.strainoptimizationalgorithms.jecoli.JecoliGenericConfiguration;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.io.MultipleExtensionFileFilter;

public class AggregateSimplifyTask extends AbstractAnalysisTask {
	
	public static final FileFilter	INPUT_FILTER			= new MultipleExtensionFileFilter(".ss");
	public static final String		OUTPUT_NAME				= "aggregated#";
															
	
	protected boolean				mergeOnly				= false;
	protected boolean				mergeOnly_hashed		= false;
	protected Map<String, Object>	simplifierOptions		= null;
															
	public AggregateSimplifyTask(String baseDir, AnalysisEnv env, boolean mergeOnly) {
		super(baseDir, env);
		this.mergeOnly = mergeOnly;
	}
	
	@Override
	public void execute() throws Exception {
		
		TreeMap<Integer, Pair<String, Map<Integer, String>>> toRun = _env.getTree().getValidatedNodes();
		
		for (Integer order : toRun.keySet()) {
			Pair<String, Map<Integer, String>> pair = toRun.get(order);
			Map<Integer, String> info = pair.getB();
			
			Map<String, String> convert = _env.convert(info);

			
			String branchDir = _directory + (_directory.endsWith("/") ? "" : "/") + _env.generateBranchBase(convert, "/");

			JecoliGenericConfiguration configuration = (JecoliGenericConfiguration) generateConfigurationForBranch(convert);
			
			if (mergeOnly) {
				mergeOnly(branchDir, convert, configuration);
			} else {
				mergeAndSimplify(branchDir, convert, configuration);
			}		
		}		
	}
	
	public String getOutputSuffix(JecoliGenericConfiguration configuration) {
		return (configuration.getIsGeneOptimization()) ? ".gsss" : ".rsss";
	}
	
	public void setMergeOnlyHashed(boolean mergeOnlyHashed) {
		this.mergeOnly_hashed = mergeOnlyHashed;
	}
	
	public void setSimplifierOptions(Map<String, Object> simplifierOptions) {
		this.simplifierOptions = simplifierOptions;
	}	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void mergeOnly(String branchDir, Map<String, String> convert, JecoliGenericConfiguration configuration) throws Exception {
		
		String output = branchDir + "/" + OUTPUT_NAME + getBaseName(convert) + (mergeOnly ? "" : "#SIMPLIFIED") + getOutputSuffix(configuration);
		String strategy = configuration.getOptimizationStrategy();
		
		ResultSetFactory resultSet_factory = new ResultSetFactory();
		
		File[] files = listFiles(branchDir, ".ss");
		
		if (files != null && files.length > 0) {
			System.out.println("[Aggregation] beginning with directory [" + branchDir + "]");
			
			for (File file : files)
				System.out.println("\t" + file.getAbsolutePath());
				
			IStrainOptimizationResultSet aggregatedSet = resultSet_factory.getResultSetInstance(strategy, configuration);
			
			for (File file : files) {
				String fullpath = file.getAbsolutePath();
				System.out.print("Reading and simplifying [" + fullpath + "]...");
				
				IStrainOptimizationResultSet resultSet = resultSet_factory.getResultSetInstance(strategy, configuration);
				resultSet.readSolutionsFromFile(fullpath);
				
				aggregatedSet.merge(resultSet);
				System.out.println(" done!");
			}
			
			System.out.print("Done with " + files.length + " files. Saving [" + output + "]...");
			aggregatedSet.writeToFile(output);
			System.out.println(" done!");
		} else {
			System.out.println("No valid files found at location [" + branchDir + "]");
		}
		
	}
	
	@SuppressWarnings({ "unchecked"})
	public <T extends JecoliGenericConfiguration, E extends IStrainOptimizationResult> void mergeAndSimplify(String branchDir, Map<String, String> convert, JecoliGenericConfiguration configuration)
			throws Exception {
			
		String output = branchDir + "/" + OUTPUT_NAME + getBaseName(convert) + (mergeOnly ? "" : "#SIMPLIFIED") + getOutputSuffix(configuration);
		
		boolean fileExists = new File(output).exists();
		
		if(!fileExists || (fileExists && override_if_existent)){
			String strategy = configuration.getOptimizationStrategy();
			
			StrainOptimizationSimplificationFactory simp_factory = new StrainOptimizationSimplificationFactory();
			ResultSetFactory resultSet_factory = new ResultSetFactory();
			
			IStrainOptimizationResultsSimplifier<T, E> simplifier = simp_factory.getSimplifierInstance(strategy, configuration);
			if (simplifierOptions != null && !simplifierOptions.isEmpty()) {
				simplifier.setSimplifierOptions(simplifierOptions);
			}
			
			File[] files = listFiles(branchDir, ".ss");
			
			if (files != null && files.length > 0) {
				System.out.println("[Aggregation] beginning with directory [" + branchDir + "]");
				
				
				IStrainOptimizationResultSet<T, E> aggregatedSet = resultSet_factory.getResultSetInstance(strategy, configuration);
				
				for (int f=0; f<files.length; f++) {
					File file = files[f];
					String fullpath = file.getAbsolutePath();
					System.out.println("["+f+"] Reading and simplifying [" + fullpath + "]...");
					
					IStrainOptimizationResultSet<T, E> resultSet = resultSet_factory.getResultSetInstance(strategy, configuration);
					resultSet.readSolutionsFromFile(fullpath);
					
					IStrainOptimizationResultSet<T, E> simpResultSet = resultSet_factory.getResultSetInstance(strategy, configuration);
					
					System.out.print("Simplifying solution ");
					int i = 0;
					for (E sol : resultSet.getResultList()) {
						if (i % 10 == 0) {
							System.out.print(i + ", ");
						}
						List<E> sols = simplifier.simplifySolution(sol);
						for (E s : sols) {
							simpResultSet.addSolution(s);
						}
						i++;
					}
					
					aggregatedSet.merge(simpResultSet);
					System.out.print(" done!\n");
				}
				
				System.out.print("Done with " + files.length + " files. Saving [" + output + "]...");
				aggregatedSet.writeToFile(output);
				System.out.println(" done!");
			} else {
				System.out.println("No valid files found at location [" + branchDir + "]");
			}			
		}else{
			System.out.println("File ["+output+"] exists... skipping");
		}
	}
	
}
