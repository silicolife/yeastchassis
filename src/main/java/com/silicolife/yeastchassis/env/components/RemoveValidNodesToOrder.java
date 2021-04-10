package com.silicolife.yeastchassis.env.components;

import java.util.TreeMap;

public class RemoveValidNodesToOrder implements IOperationNodeTree{

	private static final long serialVersionUID = 1L;

	@Override
	public Object run(INodeTree n, Object... args) {
		
		if(RegistEnvNode.class.isAssignableFrom(n.getClass()) && ((RegistEnvNode)n).isValidPath()){
			
			if(((RegistEnvNode)n).isValidPath()){
				TreeMap<Integer, RegistEnvNode> orderedNodes = (TreeMap<Integer, RegistEnvNode>) args[0];
				orderedNodes.values().remove(n);
				CalculateOrder.removeGaps(orderedNodes);
			}
		}

		return null;
	}

	@Override
	public Object agregateChildReturns(Object... childRets) {
		return null;
	}

	@Override
	public Object[] calculateNextArgs(Object ret, Object... args) {
		return args;
	}

}
