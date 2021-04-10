 package com.silicolife.yeastchassis.env.container;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.silicolife.yeastchassis.env.components.AbstractEnvironment;
import com.silicolife.yeastchassis.env.exception.NonExistingMetaboliteException;
import com.silicolife.yeastchassis.env.exception.NonExistingReactionException;
import com.silicolife.yeastchassis.env.utils.ToStringUtils;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.ContainerUtils;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.GeneCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionConstraintCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionTypeEnum;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.StoichiometryValueCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.exceptions.MetaboliteDoesNotExistsException;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.exceptions.ReactionDoesNotExistsException;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.writers.JSBMLWriter;
import pt.uminho.ceb.biosystems.mew.biocomponents.validation.chemestry.BalanceValidator;
import pt.uminho.ceb.biosystems.mew.core.model.components.EnvironmentalConditions;
import pt.uminho.ceb.biosystems.mew.core.model.components.Reaction;
import pt.uminho.ceb.biosystems.mew.core.model.components.ReactionConstraint;
import pt.uminho.ceb.biosystems.mew.core.model.components.enums.ReactionType;
import pt.uminho.ceb.biosystems.mew.core.model.converters.ContainerConverter;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.collection.CollectionUtils;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.MapUtils;
import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;

public class ContainerEnv extends AbstractEnvironment{
	
	private static final long serialVersionUID = 1L;
	public static final String SBML_FILE 		= "file";
	public static final String REACTIONS_FILE 	= "reactions_file";
	public static final String METABOLITES_FILE = "metabolites_file";
	public static final String GENES_FILE 		= "genes_file";
	public static final String MATRIX_FILE 		= "matrix_file";
	public static final String BIO_OPT_FILE		= "file";
	public static final String METATOOL_FILE	= "file";
	
	public static final String HOST_PARAM = "host";
	public static final String PORT_PARAM = "port";
	public static final String SCHEMA_PARAM = "schema";
	public static final String USER_PARAM = "user";
	public static final String PASSWD_PARAM = "pwd";
	public static final String ID_PARAM = "id";
	public static final String SHORT_NAME_PARM = "short";
	public static final String VERSION_PARAM = "version";
	
	
	protected Container container;
	protected ContainerInput source;
	protected Map<String, Object> sourceInputs;
	protected Collection<String> metabolitesToRemove;
	protected Collection<String> compartmentsToRemove;
	protected Map<String, ReactionCI> removedReactions;
	protected Set<String> addedReactions;
	protected Set<String> addedMetabolites;
	protected Set<String> addedCompartments;
	
	/** Map<Compound, MetaboliteCI> */
	protected Map<String, MetaboliteCI> removedMetabolites;
	
	protected Map<String, String> metaboliteFormulaSources;
	
	protected Map<String, List<String>> addedDrains;
	protected Map<String, Boolean> changedReversibilities;
	protected Set<String> revertedReactions;
	
	protected String name;
	
	protected ISteadyStateModel model;
	
	
	/**
	 * Generates a ContainerEnv from a SBML file.  
	 * 
	 * @param sbmlFilePath
	 * @param name
	 * @return a Container Environment
	 * @throws Exception
	 */
	public static ContainerEnv readSBMLFile(String sbmlFilePath, String name) throws Exception {
		Map<String, Object> files = new HashMap<String, Object>();
		files.put(SBML_FILE, sbmlFilePath);
		return new ContainerEnv(ContainerInput.SBML, files, name);
	}

	/**
	 * Creates a ContainerEnv based on the inputs.
	 * E.g.: to read a container from flat files:
	 * <ul>
	 * <li> Map<String, Double> inputs;
	 * <li> inputs.put(ContainerEnv.REACTIONS_FILE, path/to/file);
	 * <li> inputs.put(ContainerEnv.METABOLITES_FILE, path/to/file);
	 * <li> inputs.put(ContainerEnv.MATRIX_FILE, path/to/file);
	 * <li> inputs.put(ContainerEnv.GENES_FILE, path/to/file);
	 * <li> ContainerEnv c = new ContainerEnv(ContainerSource.SPARSE_FLAT_FILES, inputs, name);
	 * </ul>
	 * 
	 * 
	 * @param source the Container Source type.
	 * @param inputs a map of inputs to generate a container. 
	 * @param name the name of the container.
	 * @throws Exception 
	 */
	public ContainerEnv(ContainerInput source, Map<String, Object> inputs, String name) throws Exception {
		this.name = name;
		this.source = source;
		this.sourceInputs = inputs;
		this.metabolitesToRemove = new HashSet<String>();
		this.compartmentsToRemove = new HashSet<String>();
		this.addedDrains = new HashMap<String, List<String>>();
		this.removedReactions=new HashMap<String, ReactionCI>();
		
		this.container = source.read(name, inputs);
		this.addedReactions = new HashSet<String>();
		generateMetaboliteFormulaSources();
		loadDefaultConfig();
	}


	/**
	 * Loads a ContainerEnv from a serialized object.
	 * 
	 * @param filePath
	 * @return the ContainerEnv deserialized from the file.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static ContainerEnv loadContainer(String filePath) throws IOException, ClassNotFoundException {
		Container container = (Container) FileUtils.loadSerializableObject(filePath);
		return new ContainerEnv(container);
	}
	
	
	/**
	 * Creates a Container from an existing container.
	 * 
	 * @param container
	 */
	public ContainerEnv(Container container) {
		this.name = container.getModelName();
		this.source = null;
		this.sourceInputs = new HashMap<String, Object>();
		this.metabolitesToRemove = new HashSet<String>();
		this.compartmentsToRemove = new HashSet<String>();
		this.addedDrains = new HashMap<String, List<String>>();
		this.container = container;
		this.removedReactions=new HashMap<String, ReactionCI>();
		this.addedReactions = new HashSet<String>();
//		this.container.putDrainsInProductsDirection();
		generateMetaboliteFormulaSources();
	}
	
	public Map<String,Object> getSourceInputs(){
		return sourceInputs;
	}

	public Collection<String> getMetabolitesToRemove() {
		return metabolitesToRemove;
	}
	
	public Collection<String> getCompartmentsToRemove() {
		return compartmentsToRemove;
	}
	
	public ContainerEnv(ContainerEnv container) {
		this(container.getContainer().clone());
		this.source = container.getSource();
		this.metabolitesToRemove.addAll(container.getMetabolitesToRemove());
		this.compartmentsToRemove.addAll(getCompartmentsToRemove());
		this.addedDrains.putAll(getAddedDrains());
		this.removedReactions.putAll(getRemovedReactions());
		this.addedReactions.addAll(getAddedReactions());
		metaboliteFormulaSources.putAll(container.metaboliteFormulaSources);

	}

	protected void generateMetaboliteFormulaSources(){
		metaboliteFormulaSources = new HashMap<String, String>();
		for(String mId : container.getMetabolites().keySet()){
			String sourceTAG = (source==null)?"CONTAINER":source.toString();
			metaboliteFormulaSources.put(mId, sourceTAG);
		}
	}
	
	@Override
	public void loadDefaultConfig() {
		
	}
	
	/**
	 * Removes duplicated metabolites using a given pattern.
	 * E.g.: Metabolites identified by Pallson notation (_c - Cytoplasm,
	 *  _e - External, _p - Periplasm)
	 * <p>
	 * 		containerEnv.removeDuplicatedMetabolites(Pattern.compile("(.*)_[a-z]"));<br>
	 * 		#this will use the identifiers on the capture.
	 * </p>
	 * @param pattern
	 * @return Map<String, String> where the key is the old metabolite id and the value is the new metabolite id.
	 * @throws Exception
	 */
	public Map<String, String> removeDuplicatedMetabolites(Pattern pattern) throws Exception {
		model = null;
		Map<String, String> removedMetabolites = container.stripDuplicateMetabolitesInfoById(pattern);
		return removedMetabolites;
	}
	
	
	/**
	 * Removes metabolites that match a given String (compiled to a Pattern)
	 * 
	 * @param patternString
	 * @return the Container Environment
	 */
	public Set<String> removeMetaboliteByIdPattern(String patternString) {
		
		Set<String> toRemove =  identifyMetaboliteByIdPattern(patternString);
		container.removeMetabolites(toRemove);
		metabolitesToRemove.addAll(toRemove);
		model = null;
		return toRemove;
	}
	
	public Set<String> identifyMetaboliteByIdPattern(String patternString) {
		Pattern pattern = Pattern.compile(patternString);
		Set<String> toRemove =  container.identifyMetabolitesIdByPattern(pattern);
		return toRemove;
	}
	
	
	public void removeReactions(Collection<String> reactionsToRemove){
		
		model = null;
		Map<String, ReactionCI> removedReactions = container.removeReactions(convertToHashSet(reactionsToRemove));
		this.removedReactions.putAll(removedReactions);
	}
	
	public void closeAllDrais() {
	
		Set<String> d = container.getDrains();
		
		for(String id : d){
			container.getDefaultEC().put(id, new ReactionConstraintCI(0, ReactionConstraintCI.INFINITY));
		}
		
	}

	
	/**
	 * Prints the information about this Container Environment.
	 * This information includes the Container name, sources used to import,
	 * removed metabolites and reactions and the number of current metabolites, 
	 * reactions, compartments and genes.
	 */
	@Override
	public void info() {
		System.out.println("Container " + name + ":");
		System.out.println("\tSource " + source + " files:");
		for(String fileTag : sourceInputs.keySet())
			System.out.println("\t\t" + fileTag + ": " + sourceInputs.get(fileTag));
		System.out.println();
		System.out.println("\tNumber of Reactions:\t" + container.getReactions().size());
		System.out.println("\tNumber of Metabolites:\t" + container.getMetabolites().size());
		System.out.println("\tNumber of Compartments:\t" + container.getCompartments().size());
		System.out.println("\tNumber of Genes:\t" + container.getGenes().size());
		if(metabolitesToRemove.size() > 0 || compartmentsToRemove.size() > 0) {
			System.out.println("\tRemoved:");
			if(metabolitesToRemove.size() > 0)
				System.out.println("\t\tMetabolites:\t" + CollectionUtils.join(metabolitesToRemove, ", "));
			if(compartmentsToRemove.size() > 0)
				System.out.println("\t\tCompartments:\t" + CollectionUtils.join(compartmentsToRemove, ", "));
		}

	}
	
	/**
	 * Prints the list of current metabolites.
	 * @return the Container Environment
	 */
	public ContainerEnv printMetabolites() {
		for(String metId : container.getMetabolites().keySet()) {
			MetaboliteCI met = container.getMetabolite(metId);
			System.out.println(metId + "\t " + met.getName() + "\t" + met.getFormula() + "\t"+ met.getCharge());
		}
		return this;
	}
	
	/**
	 * Prints the list of current reactions.
	 */
	public void printReactions() {
		printReactions(new TreeSet<String>(container.getReactions().keySet()));
	}
	
	
	/**
	 * Prints the list of reaction given in reactionIds.
	 * @param reactionIds the reactions to print.
	 * @return the Container Environment
	 */
	public ContainerEnv printReactions(Collection<String> reactionIds) {
		for(String rid : reactionIds)
			printReaction(rid);
		return this;
	}
	
	public void saveReactions(String file, Collection<String> rs) throws IOException{
		if(rs==null) rs=getContainer().getReactions().keySet();
		FileWriter w = new FileWriter(file);
		writeReactions(w, rs);
		w.close();
	}
	
	private void writeReactions(Writer w, Collection<String> rs) throws IOException{
		for(String r : rs)
			writeReaction(w, r);
	}
	
	private void writeReaction(Writer w, String r) throws IOException{
		w.write(toStringReaction(r) + "\n");
	}

	/**
	 * Creates (if not exists) a Drain for a target metabolite in
	 * a compartment. 
	 * 
	 * @param target
	 * @param comp
	 * @return the drain identifier
	 * @throws Exception 
	 */
	public String addDrain(String target, String comp, double lower, double upper) throws Exception {
		if(!addedDrains.containsKey(target))
			addedDrains.put(target, new ArrayList<String>());
		

		String drain = container.constructDrain(target, comp, lower, upper);
		//e este print vazio !?
//		System.out.println();
		if(drain != null)
			addedDrains.get(target).add(comp);
		
		model = null;
		return drain;
	}
	
	public void registerAddedMetabolite(String metaboliteId){
		if(addedMetabolites==null)
			addedMetabolites = new HashSet<String>();
		if(!addedMetabolites.contains(metaboliteId))
			addedMetabolites.add(metaboliteId);
		
		if(removedMetabolites!=null && removedMetabolites.containsKey(metaboliteId))
			removedMetabolites.remove(metaboliteId);
	}
	
	public void registerAddedCompartment(String compartmentId){
		if(addedCompartments==null)
			addedCompartments = new HashSet<String>();
		if(!addedCompartments.contains(compartmentId))
			addedCompartments.add(compartmentId);
	}
	
	public String addDrain(String target, String comp) throws Exception{
		return addDrain(target, comp, -ReactionConstraintCI.INFINITY, ReactionConstraintCI.INFINITY);
	}
	
	public ContainerEnv stripDuplicateMetabolitesInfoById(Pattern pattern) throws Exception {
		container.stripDuplicateMetabolitesInfoById(pattern);
		model = null;
		return this;
	}
	
	public ContainerEnv stripDuplicateMetabolitesInfoById(String patternString, boolean allMatch) throws Exception{
		Pattern pattern = Pattern.compile(patternString);
		container.stripDuplicateMetabolitesInfoById(pattern, allMatch);
		model = null;
		return this;
	}
	
	public ContainerEnv stripDuplicateMetabolitesInfoById(String patternString) throws Exception {
		return stripDuplicateMetabolitesInfoById(patternString, true);
	}
	
	/**
	 * Extracts the EnvironmentalConditions for the container.
	 * 
	 * @return the extracted EnvironmentalConditions
	 * @throws Exception
	 */
	public EnvironmentalConditions extractDefaultEnvironmentalConditions() throws Exception {
		EnvironmentalConditions ec = new EnvironmentalConditions();
		ISteadyStateModel model = getModel();
		for(Reaction r : model.getReactions().values()) {
			if(r.getType() == ReactionType.DRAIN) {
				double lb = r.getConstraints().getLowerLimit();
				double ub = r.getConstraints().getUpperLimit();
				ReactionConstraint constraint = new ReactionConstraint(lb,ub);
				ec.addReactionConstraint(r.getId(), constraint);
			}
		}
		
		return ec;
	}
	
	/**
	 * Returns the container in this Environment.
	 * 
	 * @return the container in the ContainerEnv present state.
	 */
	public Container getContainer() {
		model = null;
		return container;
	}
	
	/**
	 * Prints a reaction.
	 * 
	 * @param reactionId the identifier of the reaction to print.
	 * @return the Container Environment
	 */
	public ContainerEnv printReaction(String reactionId) {
		System.out.println(toStringReaction(reactionId));
		return this;
	}
	
	public String toStringReaction(String reactionId) {
		if(!container.getReactions().containsKey(reactionId))
			return null;
		ReactionCI r = container.getReaction(reactionId);

		Map<String, MetaboliteCI> metabolites = container.getMetabolites();
		String output = reactionId+"\t"+ 
			ToStringUtils.printModelReaction(r, metabolites, " ", true, true) + "\t" + r.getEc_number() + "\t" + r.getGeneRuleString()+ "\t" 
				+ getAllExtraInfo(reactionId, container.getReactionsExtraInfo());
		return output;
	}
	
	public Set<String> getReactionsByMetabolite(String metId){
		Set<String> rIds = getContainer().getMetabolite(metId).getReactionsId();
		return (rIds==null) ? null : new HashSet<String>(rIds);
	}
	
	
	public void printReactionsAssociatedToMetabolite(String metId){
		printReactions(getReactionsByMetabolite(metId));
	}
	/**
	 * Generates a {@link ISteadyStateModel} from this container.
	 * 
	 * @return the metabolic model generated from the container.
	 * @throws Exception
	 */
	public ISteadyStateModel getModel() throws Exception{
		if(model == null){
			model = ContainerConverter.convert(container);
			if(container.getBiomassId()!=null)
				model.setBiomassFlux(container.getBiomassId());
		}
		return model;
	}

	
	public void saveInSBMLFile(String sbmlFile) throws Exception{
		Container cont = (!container.hasUnicIds())? container.standardizeContainerIds():container;
		JSBMLWriter writer = new JSBMLWriter(sbmlFile, cont);
		writer.writeToFile();
	}

	
	public void setFormula(String metId, String formula, int charge, String sourceID) {
		
		MetaboliteCI met = getContainer().getMetabolite(metId);
		if(met!=null){
			met.setFormula(formula);
			met.setCharge(charge);
			metaboliteFormulaSources.put(metId, sourceID);
		}else
			throw new MetaboliteDoesNotExistsException(metId);
		
	}
	
	public void setFormulaAndChargeFromFile(String filePath, String sep) throws NumberFormatException, Exception{
		setFormulasAndChargeContainer(filePath, this, sep);
	}
	
	public void setFormula(String metId, String formula, int charge){
		setFormula(metId, formula, charge, "USER");	
	}
	
	public void saveBalanceInfo(String file, String protonId) throws IOException {
		FileUtils.saveStringInFile(file, toStringBalanceInfo(protonId));
	}
	
	public String toStringBalanceInfo(String protonId){
		return toStringBalanceInfo(protonId, container.getReactions().keySet());
	}
	
	public void printBalanceInfo(String protonId){
		System.out.println(toStringBalanceInfo(protonId));
	}
	
	public void printBalanceInfo(String protonId, Collection<String> reactionsToPrint){
		System.out.println(toStringBalanceInfo(protonId, reactionsToPrint));
	}
	
	public String toStringBalanceInfo(String protonId, Collection<String> reactionsToPrint){
		BalanceValidator bv = ContainerUtils.balanceModelInH(getContainer(), protonId);
		String toPrint = "";
		try {
			toPrint = ContainerUtils.toStringBalanceInfo(bv,reactionsToPrint, BalanceValidator.ALL_TAGS, metaboliteFormulaSources);
		} catch (Exception e) {
			System.out.println("Nunca devia ter passado por aqui!!! Falar com pvilaca");
			e.printStackTrace();
		}
		return toPrint;
	}
	
	public void applyBalance(String protonId) throws Exception{
		BalanceValidator bv = getBalanceValidator(protonId);
		bv.balanceH(protonId);
		container = bv.getBalancedContainer();
		resetModel();
	}
	
	public BalanceValidator getBalanceValidator(String protonId){

		BalanceValidator bv = ContainerUtils.balanceModelInH(getContainer(), protonId);
		return bv;
	}
	
	public Set<String> identyfyReactionWithDeadEnds(/*boolean useComp*/){
		return ContainerUtils.identyfyReactionWithDeadEnds(getContainer()/*, useComp*/);
	}
	
	public Set<String> identifyReactionWithDeadEndsNotIt(/*boolean useComp*/){
		return ContainerUtils.identyfyReactionWithDeadEndsNotIt(getContainer()/*, useComp*/);
	}
	
	public void removeMetabolitesAndItsReactions(Set<String> metabolites){
		
		Set<String> reactions = new HashSet<String>();
		
		for(String mId : metabolites)
			reactions.addAll(container.getMetabolite(mId).getReactionsId());
		this.removeReactions(reactions);
		model = null;
	}
	
	protected Set<String> identifyMetaboliteDeadEnds(boolean useComp){
		return ContainerUtils.remomeMetaboliteDeadEnds(container.clone(), useComp);
	}
	
	public void loadDefaultBounds(String file, String sep) throws IOException{
		addDefaultBoundsToContainer(getContainer(), file, sep);
		resetModel();
	}

	protected Set<String> identifyMetaboliteDeadEndsIt(boolean useComp){
		return ContainerUtils.removeDeadEndsIteratively(container.clone(), useComp);
	}
	
	static private void setFormulasAndChargeContainer(String file, ContainerEnv env, String sep) throws NumberFormatException, Exception{
		Map<String, String[]> data = FileUtils.readTableFileFormat(file, sep, 0);
		System.out.println();
		for(String mId : data.keySet()){
			
			String[] d = data.get(mId);
			String formula = d[1];
			String chargeS = d[2];
			
			String source = "NOT_SPECIFIED_FILE";
			if(d.length > 3)
				source = d[3];
			
			try {
				env.setFormula(mId, formula, Integer.parseInt(chargeS), source);
			} catch (Exception e) {
				System.out.println("WARNING: " + e.getMessage());

			}
			
		}
		
	}
	
	public Set<String> getReactionByType(ReactionTypeEnum type){
		return container.getReactionsByType(type);
	}

	public ReactionCI removeReaction(String modelId) throws Exception{		
		return removeReaction(modelId, true);
	}
	
	public ReactionCI removeReaction(String modelId, boolean verifyDependency) throws Exception{		

		model=null;
		ReactionCI r = getContainer().removeReaction(modelId, verifyDependency);
		if(r==null)
			throw new Exception("Reaction " + modelId + " does not exist!");
		removedReactions.put(r.getId(), r);
		addedReactions.remove(r.getId());
		return r;
	}

	public List<String> reactionsWithGeneRule(ReactionTypeEnum reactionType) {
		List<String> ret = new ArrayList<String>();
		for(String rId : container.getReactions().keySet()) {
			ReactionCI r = container.getReaction(rId);
			if(r.getGeneRule() != null && r.getType().compareTo(reactionType) == 0)
				ret.add(rId);
		}
		return ret;
	}
	
	public ContainerEnv fillReactionLimits() {
		Map<String, ReactionConstraintCI> contraints = new HashMap<String, ReactionConstraintCI>();
		for(String rId : container.getReactions().keySet()) {
			if(container.getDefaultEC().containsKey(rId))
				contraints.put(rId, container.getDefaultEC().get(rId).clone());
			else {
				if(container.getReaction(rId).isReversible())
					contraints.put(rId, new ReactionConstraintCI(-100000, 100000));
				else
					contraints.put(rId, new ReactionConstraintCI(0, 100000));
			}
			
		}
		container.setDefaultEC(contraints);
		return this;
	}
	
	public void setBiomassId(String biomass){
		model = null;
		container.setBiomassId(biomass);
		
	}
	
	public void resetModel(){
		model=null;
	}
	
	public ContainerEnv identifyReactionTypes() {
		Set<String> rIds = container.identifyTransportReactions();
		container.defineReactionsType(rIds, ReactionTypeEnum.Transport);
		rIds = container.getDrains();
		container.defineReactionsType(rIds, ReactionTypeEnum.Drain);
		container.setBiomassId(container.getBiomassFluxFromSizeHeuristic());
		container.getReaction(container.getBiomassId()).setType(ReactionTypeEnum.Biomass);
		rIds = container.getInternalReactions();
		container.defineReactionsType(rIds, ReactionTypeEnum.Internal);
		return this;

	}
	
	public void changeReversibility(String id, boolean isRevesible) throws NonExistingReactionException{
		ReactionCI r = getContainer().getReaction(id);
		if(r==null)
			throw new NonExistingReactionException(id);
		getContainer().getReaction(id).setReversible(isRevesible);
		getContainer().getDefaultEC().remove(id);
		
		registerChangedReversibility(id, isRevesible);
		model=null;
	}
	
	public void revertReaction(String id) throws NonExistingReactionException{
		ReactionCI r = getContainer().getReaction(id);
		if(r==null)
			throw new NonExistingReactionException(id);
		Map<String, StoichiometryValueCI> stoiq = r.getReactants();
		r.setReactants(r.getProducts());
		r.setProducts(stoiq);
		r.setReversible(false);
		getContainer().getDefaultEC().remove(r.getId());
		registerRevertedReaction(id);
		getContainer().getDefaultEC().remove(id);
		model = null;
	}
	
	
	public void changeBoundsUsingMetabliteId(String metaboliteId, double lb, double ub) throws Exception{
		
		if(this.container.getMetabolite(metaboliteId) == null) throw new Exception("Metabolite Id " + metaboliteId + " does not exist");
		String drainId = container.getMetaboliteToDrain().get(metaboliteId);
		if(drainId == null) throw new Exception("Metabolite Id " + metaboliteId + " does not have drain");
		
		container.getDefaultEC().put(drainId, new ReactionConstraintCI(lb, ub));
	}
	
	
	public void createDrainUsingMetaboliteId(String metaboliteId, String comp, double lb, double ub) throws Exception{
		if(this.container.getMetabolite(metaboliteId) == null) throw new Exception("Metabolite Id " + metaboliteId + " does not exist");
		String drainId = container.getMetaboliteToDrain().get(metaboliteId);
		
		if(drainId == null)
			container.constructDrain(metaboliteId, comp, lb, ub);
		else
			container.getDefaultEC().put(drainId, new ReactionConstraintCI(lb, ub));
	}
	
	public String toStringStats(){
		
		
		String ret = "";
		int allReactions = container.getReactions().size();
		int transports = container.identifyTransportReactions().size();
		int drains = container.getDrains().size();
		
		int deadEnds = identifyMetaboliteDeadEnds(false).size();
		int deadEndsIt = identifyMetaboliteDeadEndsIt(false).size();
		
		Set<String> ecs = ContainerUtils.getAllEcNumbers(container);
		Set<String> intRGPR = ContainerUtils.getReactionsWithGPR(container, ContainerUtils.getInternalReactions(container));
		Set<String> tRGPR = ContainerUtils.getReactionsWithGPR(container, container.identifyTransportReactions());
		
		
		System.out.println(ecs);
		
		ret+="Genes\t"+ container.getGenes().size()+"\n";
		ret+="Metabolites\t"+ container.getReactions().size()+"\n";
		ret+="Internal Reactions\t"+ (allReactions - transports - drains)+"\n";
		ret+="Drais\t"+ drains+"\n";
		ret+="Transports\t"+ transports+"\n";
		ret+="Internal reactions with Gene Rule\t"+intRGPR.size()+"\n";
		ret+="Transport reactions with Gene Rule\t"+ tRGPR.size()+"\n";
		ret+="Metabolite Dead Ends\t"+deadEnds+"\n";
		ret+="Metabolite Dead Ends It.\t"+deadEndsIt+"\n";
		
		return ret;
	}
	
	public void printStatsInfo(){
		System.out.println(toStringStats());
	}
	
	public String toStringMetabolitesInfo(Collection<String> metabolites, Collection<String> infoTags) throws NonExistingMetaboliteException {
		Collection<String> mets = (metabolites==null)?getContainer().getMetabolites().keySet():metabolites;
		Map<String, Map<String, String>> info = getAllInformationMetabolite(container, mets);
		
		
		Collection<String> infoT = (infoTags==null)?MapUtils.getSecondMapKeys(info):infoTags; 
		return MapUtils.prettyMAP2LineKeySt(info, infoT, "");
	}
	
	public void printMetaboliteInfo(Collection<String> metabolites, Collection<String> infoTags) throws NonExistingMetaboliteException{
		System.out.println(toStringMetabolitesInfo(metabolites, infoTags));
	}
	
	public void printMetaboliteInfo(Collection<String> metabolites) throws NonExistingMetaboliteException{
		System.out.println(toStringMetabolitesInfo(metabolites, null));
	}

	public void printMetaboliteInfo() throws MetaboliteDoesNotExistsException, NonExistingMetaboliteException{
		System.out.println(toStringMetabolitesInfo(null, null));
	}
	
	public void saveMetaboliteInfo(String path) throws IOException, NonExistingMetaboliteException{
		FileUtils.saveStringInFile(path, toStringMetabolitesInfo(null, null));
	}
	
	public Map<String, ReactionCI> getRemovedReactions(){
		return removedReactions;
	}
	
	public Set<String> getAddedCompartments(){
		return addedCompartments;
	}
	
	public Set<String> getAddedReactions(){
		return addedReactions;
	}
	
	public Set<String> getAddedMetabolites(){
		return addedMetabolites;
	}
	
	public Map<String, List<String>> getAddedDrains(){
		return addedDrains;
	}
	
	public Map<String, MetaboliteCI> getRemovedMetabolites() {
		return removedMetabolites;
	}

	public void changeReactionsMetabolites(Collection<String> reactions, Map<String, String> mets) throws Exception{
		for(String id : reactions)
			changeReactionMetabolites(id, mets, false);
		container.verifyDepBetweenClass();
	}
	
	private void changeReactionMetabolites(String id, Map<String, String> mets, boolean verify) throws Exception{
		
		ReactionCI r = container.getReaction(id);
		if(r==null) throw new Exception("Reaction id" + id +" does not exists!");
		r.changeMetaboliteIds(mets);
		if(verify) container.verifyDepBetweenClass();
		resetModel();
	}
	
	public void addEnvironmentalConditionsToDefault(EnvironmentalConditions ec){
		
		for(String id : ec.keySet()){
			ReactionConstraint rc = ec.get(id);
			getContainer().getDefaultEC().put(id, new ReactionConstraintCI(rc.getLowerLimit(), rc.getUpperLimit()));
		}
	}
	
	
	/**
	 * Searches reaction id using a regular expression 
	 * @param pattern
	 * @return Set of reaction ids
	 */
	public Set<String> searchReactions(Pattern pattern){
		return container.searchReactionById(pattern);
	}
	
	public Set<String> searchReactions(String pattern){
		return searchReactions(Pattern.compile(pattern));
	}
	
	/**
	 * Searches metabolite id using a regular expression 
	 * @param pattern
	 * @return metabolite ids
	 */
	public Set<String> searchMetabolites(Pattern pattern){
		return container.identifyMetabolitesIdByPattern(pattern);
	}
	
	public Set<String> searchMetabolites(String pattern) {
		return searchMetabolites(Pattern.compile(pattern));
		
	}
	
	/**
	 * Changes reaction bounds
	 * 
	 * Attention: to revert the reaction use method revertReaction
	 *            to change the reversibility use the method changeReversibility
	 *            
	 * @param rId reaction id
	 * @param lb  reaction lower bound
	 * @param ub  reaction upper bound
	 * @throws ReactionDoesNotExistsException 
	 */
	
	public void changeReactionBounds(String rId, double lb, double ub) throws ReactionDoesNotExistsException{
		getContainer().changeReactionBound(rId, lb, ub);
		model = null;
	}
	
	
	/**
	 * Changes the bounds of the reactions
	 * 
	 * Attention: to revert the reaction use method revertReaction
	 *            to change the reversibility use the method changeReversibility
	 *            
	 * @param rIds reaction ids
	 * @param lb   reaction lower bound
	 * @param ub   reaction upper bound
	 * @throws ReactionDoesNotExistsException 
	 */
	public void changeReactionBounds(Collection<String> rIds, double lb, double ub) throws ReactionDoesNotExistsException{
		try{
			for(String id : rIds)
				changeReactionBounds(id, lb, ub);
		}catch(ReactionDoesNotExistsException e){
			throw e;
		}
	}
	
	public void changeReactionsBounds(Collection<String> reactions, double lb, double ub) throws ReactionDoesNotExistsException{
		for(String rId:reactions)
			getContainer().changeReactionBound(rId, lb, ub);
	}
	
	/**
	 * Get Biomass id
	 * @return Biomass id
	 */
	public String getBiomassId(){
		return getContainer().getBiomassId();
	}
	
	public Map<String, Boolean> getChangedReversibilities(){
		return changedReversibilities;
	}
	
	public Set<String> getRevertedReactions(){
		return revertedReactions;
	}
	
	public void registerChangedReversibility(String reactionId, Boolean isReversible){
		if(changedReversibilities==null)
			changedReversibilities = new HashMap<String, Boolean>();
		changedReversibilities.put(reactionId, isReversible);
	}
	
	public void registerRevertedReaction(String reactionId){
		if(revertedReactions==null)
			revertedReactions = new TreeSet<String>();
		revertedReactions.add(reactionId);
	}
	
	public Map<String, String> stripInfoReactionIds(String pattern) {
		return stripInfoReactionIds(Pattern.compile(pattern), null);
	}

	public Map<String, String> stripInfoReactionIds(String pattern, Collection<String> reactionToChange) {
		return stripInfoReactionIds(Pattern.compile(pattern), reactionToChange);
	}
	
	public Map<String, String> stripInfoReactionIds(Pattern pattern, Collection<String> reactionToChange) {
		Map<String, String> ret = getContainer().stripInfoReactionIds(pattern, reactionToChange);
		if(!ret.isEmpty()) resetModel();
		return ret;
	}
	
	public Map<String, String> changeReactionIdsByFile(String file) throws Exception {
		Map<String, String> dic = MapUtils.getInfoInFile(file, 0, 1, "\t");
		return changeReactionIds(dic);
	}
	
	public Map<String, String> changeMetaboliteIdsByFile(String file) throws Exception {
		Map<String, String> dic = MapUtils.getInfoInFile(file, 0, 1, "\t");
		return changeMetaboliteIds(dic);
	}

	public Map<String, String> changeReactionIds(Map<String, String> dic) throws Exception {
		Map<String, String> ret = new HashMap<>(dic);
		ret.keySet().retainAll(getContainer().getReactions().keySet());
		getContainer().changeReactionIds(ret);
		resetModel();
		return ret;
	}
	
	public Map<String, String> changeMetaboliteIds(Map<String, String> dic) throws Exception {
		Map<String, String> ret = new HashMap<>(dic);
		ret.keySet().retainAll(getContainer().getMetabolites().keySet());
		getContainer().changeMetaboliteIds(ret);
		resetModel();
		return ret;
	}
	
	public void changeReactionId(String oldId, String newId) throws Exception{
		getContainer().changeReactionId(oldId, newId);
		resetModel();
	}
	
	public Set<String> getReactionsByGeneId(String id){
		return new TreeSet<String>(getContainer().getGene(id).getReactionIds());
	}
	
	public void printReactionsByGeneId(String id){
		printReactions(getReactionsByGeneId(id));
	}
	
	public ContainerInput getSource() {
		return source;
	}
	
	public ContainerEnv copy(){
		return new ContainerEnv(this);
	}
	
	public Map<String, Set<String>> getReactionsByGenes(){
		Map<String, Set<String>> ret = new TreeMap<String, Set<String>>();
		
		for(GeneCI g : getContainer().getGenes().values())
			ret.put(g.getGeneId(), new TreeSet<String>(g.getReactionIds()));
		return ret;
	}
	
	public void printReactionsByGenes() throws IOException{
		writeReactionsByGenes(new OutputStreamWriter(System.out), getReactionsByGenes());
	}
	
	public void saveReactionsByGenes(String file) throws IOException{
		FileWriter f = new FileWriter(file);
		writeReactionsByGenes(f, getReactionsByGenes());
		f.close();
	}
	
	private void writeReactionsByGenes(Writer w , Map<String, Set<String>> data) throws IOException{
		for(String id : data.keySet()){
			for(String rId : data.get(id))
				w.write(id + "\t" + rId + "\n");
		}
		w.flush();
	}	

	public void changeCompartmentIds(Map<String, String> comp) throws IOException {
		getContainer().changeCompartmentIds(comp);
	}

	public Collection<String> getAllGenesByReactionList(Collection<String> reactionsList){
		return container.getAllGenesByReactionList(reactionsList);
	}
	
	public Map<String, Double> getGenesReactionsCountMap(){
		return getContainer().getGenesReactionsCountMap();
	}
	
	public Map<String, Double> getGenesReactionsCountMap(Collection<String> geneList){
		return getContainer().getGenesReactionsCountMap(geneList);
	}
	
	public Map<String, Double> getGenesReactionsCountIntersectionMap(Collection<String> reactionList){
		return getContainer().getGenesReactionsCountIntersectionMap(reactionList);
	}
	
	public Map<String, Double> getGenesReactionsCountIntersectionMap(Collection<String> reactionList, Collection<String> genesList){
		return getContainer().getGenesReactionsCountIntersectionMap(reactionList, genesList);
	}
	
	public Map<String, String> getAllExtraInfo(String id, Map<String, Map<String, String>> allinfo){
		Map<String, String> info = new LinkedHashMap<String, String>();
		if(allinfo!=null)
			for(String infoId : allinfo.keySet()){
				String i = allinfo.get(infoId).get(id);
				if(i != null) info.put(infoId, i);
			}
		return info;
	}
	
	public void addDefaultBoundsToContainer(Container cont, String file, String sep) throws IOException{
		Map<String, ReactionConstraint> def = EnvironmentalConditions.readFromFile(file, sep);
		
		for(String id : def.keySet()){
			ReactionConstraint rc = def.get(id);
			cont.getDefaultEC().put(id, new ReactionConstraintCI(rc.getLowerLimit(), rc.getUpperLimit()));
		}
		
	}
	
	public  Map<String, Map<String, String>> getAllInformationMetabolite(Container cont, Collection<String> metabolites){
		Map<String, Map<String, String>> info = new TreeMap<String, Map<String,String>>();
		for(String id : metabolites){
			info.put(id, getAllInformationMetabolite(cont, id));
		}
		return info;
	}
	
	public  Map<String, String> getAllInformationMetabolite(Container container, String id) {
		
		Map<String, String> info = new LinkedHashMap<String, String>();
		MetaboliteCI mCI = container.getMetabolite(id);
		info.put("id", mCI.getId());
		info.put("Name",mCI.getName());
		info.put("Formula", container.getMetabolite(id).getFormula());
		info.put("Charge",(mCI.getCharge()==null)?null:mCI.getCharge().toString());
		
		for(String infoId : container.getMetabolitesExtraInfo().keySet()){
			String i = container.getMetabolitesExtraInfo().get(infoId).get(id);
			if(i != null) info.put(infoId, i);
		}
		return info;
	}
}