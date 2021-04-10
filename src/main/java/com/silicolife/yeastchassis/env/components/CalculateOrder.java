package com.silicolife.yeastchassis.env.components;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class CalculateOrder implements IOperationNodeTree{

	
	private static Logger log = Logger.getLogger(CalculateOrder.class.getName());
	@Override
	public Object run(INodeTree n, Object... args) {
		
		Integer order = (Integer) args[0];
		if(RegistEnvNode.class.isAssignableFrom(n.getClass()) && ((RegistEnvNode)n).isValidPath()){
			
			log.info("order run " + n.getClass() +"\t"+ n + "\t" + ((RegistEnvNode)n).isValidPath());
			TreeMap<Integer, RegistEnvNode> orderedNodes = (TreeMap<Integer, RegistEnvNode>) args[1];
			if(order==null) order = (orderedNodes.isEmpty())?1:orderedNodes.lastKey() +1;
			log.info("order = " + order + "\t" + orderedNodes + "\t" + n + "\t" + orderedNodes.values());
			order = mantainOrder(order, orderedNodes, (RegistEnvNode)n)+ 1;
			log.info("nextOrder" + order);
		}
		
		return order;
	}
	
	
	public static Integer mantainOrder(int order, TreeMap<Integer, RegistEnvNode> regist,
			RegistEnvNode... node){
		
		regist.values().removeAll(Arrays.asList(node));
		
		int lastIndx = ((regist.isEmpty())?1:regist.lastKey());
		
		removeGaps(regist);
		for(int i = lastIndx; i >=order; i--){
			RegistEnvNode temp = regist.remove(i);
			if(temp!=null){
				int nI = i+node.length;
				temp.setOrder(nI);
				regist.put(nI, temp);
			}
		}
		
		order = (order>regist.size())?regist.size()+1:order;
		for(int i=0; i < node.length; i++){
			node[i].setOrder(i+order);
			regist.put(i+order, node[i]);
		}
		return order;
	}
	
	
	public static void removeGaps(TreeMap<Integer, RegistEnvNode> regist) {
		LinkedHashSet<Integer> it = new LinkedHashSet(regist.keySet());
		int i = 1;
		for(Integer value : it){
			if(i<value){
				RegistEnvNode n = regist.remove(value);
				regist.put(i, n);
				n.setOrder(i);
			}
			i++;
		}
		
	}


	@Override
	public Integer agregateChildReturns(Object... childRets) {
		
		log.info(childRets);
		TreeSet<Integer> setRet = new TreeSet<Integer>();
		for(Object o : childRets){
			Integer i = null;
			try {
				i=(Integer) o;
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			if(i!=null) setRet.add(i);
		}
		
		return (setRet.isEmpty()?1:setRet.last()+1);
	}


	@Override
	public Object[] calculateNextArgs(Object ret, Object... args) {
		if(ret ==null)
			return args;
		
		args[0]=ret;
		return args;
	}

}
