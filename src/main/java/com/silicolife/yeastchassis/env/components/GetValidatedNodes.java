package com.silicolife.yeastchassis.env.components;

import java.util.Map;
import java.util.TreeMap;

public class GetValidatedNodes implements IOperationNodeTree{

	private static final long serialVersionUID = 1L;

	@Override
	public Map<Integer, String> run(INodeTree n, Object... args) {
		
		Map<Integer, String> maps = null;
		if(RegistEnvNode.class.isAssignableFrom(n.getClass()) && ((RegistEnvNode)n).isValidPath()){
			maps = new TreeMap<Integer, String>();
			getInfo(n, maps);
		}
		
		return maps;
	}

	private void getInfo(INodeTree n, Map<Integer, String> maps) {
		if(n != null){
			if(RegistEnvNode.class.isAssignableFrom(n.getClass())){
				maps.put(n.getLevel(), ((RegistEnvNode)n).getObjectId());
			}
			getInfo(n.getParent(), maps);
		}
	
	}

	@Override
	public Object agregateChildReturns(Object... childRets) {
		return null;
	}

	@Override
	public Object[] calculateNextArgs(Object ret, Object... args) {
		return null;
	}

}
