package com.silicolife.yeastchassis.env.components;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;

public class RegistEnvTree extends GenericRootTree{

	private static final long serialVersionUID = 1L;
	private List<String> levels;
	
	public RegistEnvTree(List<String> classesIds, Collection<String> mandatory) {
		super(new RegistEnvNodeControler(generateMandatoryLeves(classesIds, mandatory), new TreeMap<Integer, INodeTree>()));
		this.levels = classesIds;
	}

	public void addNode(String classId, String objectId){
		addNode(getLevel(classId), objectId);
	}

	public void addNodeAt(String nodeId, String classId, String objectId){
		addNodeAt(nodeId,  getLevel(classId), objectId);
	}

	public void addNodeInObjectId(String objectToSearch, String classId, String objectId){
		int level= getLevel(classId);
		Collection<INodeTree> allNodeToInsert = getAllNodeWith(objectToSearch);
		for(INodeTree n : allNodeToInsert){
			n.getControler().addNode(n, getControler().createNode(level, objectId));
		}
	}
	
	@Override
	public INodeTree getNode(String id) {
		INodeTree n = super.getNode(id);
		if(n == null) throw new InvalidParameterException("The node id [" + id + "] does not exist!" );
		return n;
	}
	
	public Integer changeOrder(int order, String nodeId){
		RegistEnvNode node = (RegistEnvNode) getNode(nodeId);
		if(node == null) throw new InvalidParameterException("The node id [" + nodeId + "] does not exist!" );
		return node.getControler().setOrder(node, order);
		
	}
	
	public Integer changetOrderObjectId(int order,  String... objectId){
		int orderAux = order;
		for(String o : objectId){
			System.out.println(o + "\t" + orderAux);
			orderAux = changetOrderObjectId(orderAux, o);
		}
		return orderAux;
	}
	
	public Integer changetOrderObjectId(int order,  String objectId){
		int orderToIterate = order;
		Collection<INodeTree> allnodesToOrder = getAllNodeWith(objectId);
		for(INodeTree n : allnodesToOrder){
			orderToIterate = ((RegistEnvNode)n).getControler().setOrder(n, orderToIterate);
		}
		return orderToIterate;
	}
	
	public Integer changeOrderReverse(int order, String nodeId){
		RegistEnvNode node = (RegistEnvNode) getNode(nodeId);
		return node.getControler().setOrderReverse(node, order);
		
	}
	
	public Integer changeOrder(int order, String... nodeId){
		
		for(String n : nodeId){
			order = changeOrder(order, n);
		}
		return order;
		
	}
	
	public Integer changeOrderReverse(int order, String... nodeId){
		
		for(String n : nodeId){
			order = changeOrderReverse(order, n);
		}
		return order;
		
	}
	
	public TreeMap<Integer, INodeTree> getOrdered() {
		return ((RegistEnvNodeControler)getControler()).getOrder();
	}
	
	
	private int getLevel(String classId){
		int i = levels.indexOf(classId);
		if(i==-1) throw new InvalidParameterException("Class id ["+ classId+"] was not especified! Options = "  +levels);
		return i;
	}
	

	private static Set<Integer> generateMandatoryLeves(List<String> classesIds, Collection<String> mandatory){
		HashSet<Integer> ret = new HashSet<Integer>();
		if(mandatory == null) return ret;

		for(String mad : mandatory){
			int idx = classesIds.indexOf(mad);
			ret.add(idx);
		}
		return ret;
	}
	
	public TreeMap<Integer, Pair<String, Map<Integer, String>>> getValidatedNodes(){
		
		TreeMap<Integer, Pair<String, Map<Integer, String>>> ret = new TreeMap<Integer, Pair<String,Map<Integer,String>>>();
		Map<Integer, INodeTree> ordered = getControler().getOrder();
		
		GetValidatedNodes valNodel = new GetValidatedNodes();
		for(Integer i : ordered.keySet()){
			INodeTree n = ordered.get(i);
			String id = n.getId();
			Map<Integer, String> levelMap = valNodel.run(n);
			ret.put(i, new Pair<String, Map<Integer,String>>(id, levelMap));
		}
		
		return ret;
	}
	
	public TreeMap<Integer, Pair<String, Map<Integer, String>>> getValidatedNodes(String... nodeIds){
		
		TreeMap<Integer, Pair<String, Map<Integer, String>>> ret = new TreeMap<Integer, Pair<String,Map<Integer,String>>>();
		
		int i = 0;
		GetValidatedNodes valNodel = new GetValidatedNodes();
		for(String id : nodeIds){
			INodeTree n = getNode(id);
			Map<Integer, String> levelMap = valNodel.run(n);
			if(levelMap!=null){
				ret.put(i, new Pair<String, Map<Integer,String>>(id, levelMap));
				i++;
			}
		}
		
		return ret;
	}


	@Override
	public RegistEnvNodeControler getControler() {
		return (RegistEnvNodeControler)super.getControler();
	}
}
