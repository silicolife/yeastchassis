package com.silicolife.yeastchassis.env.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.StoichiometryValueCI;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.collection.CollectionUtils;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.collection.IMapper;

public class ToStringUtils {
	
	public static String printmodelreactionsimple(ReactionCI reaction,
			Map<String, MetaboliteCI> metaboliteList, String sep) {
		return printModelReaction(reaction, metaboliteList, sep, false, false);
	}
	
	public static String printModelReaction(ReactionCI reaction,
			Map<String, MetaboliteCI> metaboliteList, String sep,
			boolean printNames, boolean printCompartments) {

		
		TreeMap<String, StoichiometryValueCI> reactants = 
				new TreeMap<String, StoichiometryValueCI>(new StringComparator());
		
		for(String key : reaction.getReactants().keySet())		
			reactants.put(key, reaction.getReactants().get(key));

		
		TreeMap<String, StoichiometryValueCI> products = 
				new TreeMap<String, StoichiometryValueCI>(new StringComparator());
		
		for(String key : reaction.getProducts().keySet())
			products.put(key, reaction.getProducts().get(key));
		
		IMapper<StoichiometryValueCI, String> compoundInfoMapper = 
				new CompoundInfoMapper(printNames, printCompartments, metaboliteList);
		List<StoichiometryValueCI> reactantsList = new ArrayList<StoichiometryValueCI>();
		
		for(String key : reactants.keySet()) 
			reactantsList.add(reactants.get(key));
		
		List<StoichiometryValueCI> productsList = new ArrayList<StoichiometryValueCI>();
		
		for(String key : products.keySet()) {
			productsList.add(products.get(key));
		}
		
		
		Collection<String> reactList = CollectionUtils.map(reactantsList, compoundInfoMapper);
		Collection<String> prodList = CollectionUtils.map(productsList, compoundInfoMapper);
		
		
		String spliter = reaction.isReversible() ? "<=>" : "==>";
		return CollectionUtils.join(reactList, " + ") + sep + spliter + sep + CollectionUtils.join(prodList, " + ");
	}
	
	public static String reactionFluxToString(Map<String, Double> fluxes, Container container, String sep){
		
		String ret = "Reaction Id"+sep+"Flux"+ sep+ "Convertion"+sep+"reactants"+sep+"rev"+sep+"products\n";
		
		for(String id : fluxes.keySet()){
			ReactionCI reaction = container.getReaction(id);
			double value = fluxes.get(id);
			ret+= id +sep + value + sep +printModelReaction(reaction, container.getMetabolites(), sep, false, true) + "\n";
		}
		return ret;
	}
	
	public static String netConvertionToString(Map<String, Double>  netconvertion){
		String consuming = "Consuming:\n";
		String producing = "Producing:\n";
		
		for(String id : netconvertion.keySet()){
			double value = netconvertion.get(id);
			
			if(value > 0)
				producing+="\t"+id + "\t" + value+"\n";
			else
				consuming+="\t"+id + "\t" + value+"\n";
		}
		
		return consuming + "\n"+producing;
	}
	
	public static String toStringContainer(Container trnsCont){
		
		String ret = "";
		ret += "Number Of Metabolites: " + trnsCont.getMetabolites().size() +"\n";
		ret += "Number Of Reactions:   " + trnsCont.getReactions().size()+"\n";
		ret += "Number Of Genes:       " + trnsCont.getGenes().size()+"\n";
		
		return ret;
	}
	
	
	

}

class CompoundInfoMapper implements IMapper<StoichiometryValueCI, String> {
	
	boolean printNames;
	boolean printCompartments;
	Map<String, MetaboliteCI> metabolites;

	public CompoundInfoMapper(boolean printNames,
			boolean printCompartments, Map<String, MetaboliteCI> metabolites) {
		this.printNames = printNames;
		this.printCompartments = printCompartments;
		this.metabolites = metabolites;
	}

	@Override
	public String map(StoichiometryValueCI stoi) {
		String ret = "";
		double coef = stoi.getStoichiometryValue();
		
		if(coef != 1)
			ret += coef + " * ";
		
		String id = stoi.getMetaboliteId();
		if(printNames) {
			ret += metabolites.get(id).getName();				
			ret += " (" + id + ")"; 
		}
		else
			ret += id;
		
		if(printCompartments)
			ret += "[" + stoi.getCompartmentId() + "]";
		
		return ret;
	}
	
}

class StringComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		int res = 0;
		int i = 0;
		boolean stop = false;
		while(!stop) {
			if(i < o1.length() && i < o2.length()) {
				Character a = o1.charAt(i);
				Character b = o2.charAt(i);
				res = a.compareTo(b);
				if(res!=0)
					stop = true;
			}
			else{
				if(o1.length() == o2.length())
					res=0;
				else
					res = o1.length() > o2.length() ? 1 : -1;
				stop=true;
			}
				
			i++;
		}
		
		return res;
	}
}
