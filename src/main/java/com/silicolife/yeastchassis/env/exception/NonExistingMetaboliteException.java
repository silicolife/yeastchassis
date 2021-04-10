package com.silicolife.yeastchassis.env.exception;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;

public class NonExistingMetaboliteException extends Exception{
	
	private static final long serialVersionUID = 1L;

	public NonExistingMetaboliteException(){
		super();
	}
		
	public NonExistingMetaboliteException(String metaboliteId){
		super("The metabolite id '" + metaboliteId + "' does not exist!");
	}
	
	public NonExistingMetaboliteException(MetaboliteCI metaboliteCI){
		super("The metabolite id '" + metaboliteCI.getId() + ", " + metaboliteCI.getName() + ", " + metaboliteCI.getFormula() + "' does not exist!");
	}

}
