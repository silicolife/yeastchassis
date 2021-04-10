package com.silicolife.yeastchassis.env.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class RegistEnvNodeControler extends BasicNodeControler{
	
	private TreeMap<Integer, INodeTree> orderedNodes;
	private Collection<Integer> mandatoryObjects;
	private BasicNodeControler defaultC;
	
	public RegistEnvNodeControler(Collection<Integer> mandatoryObjects, TreeMap<Integer, INodeTree> orderedNodes) {
		this.mandatoryObjects = mandatoryObjects;
		defaultC = new BasicNodeControler();
		this.orderedNodes = orderedNodes;
	}
	
	@Override
	public RegistEnvNode createNode(int level, Object... args) {
		return new RegistEnvNode(level,(String)args[0], this);
	}
	
	public INodeTree clone(INodeTree node, boolean resursive){
		RegistEnvNode clonedNode = new RegistEnvNode(node.getLevel(),((RegistEnvNode)node).getObjectId(), this);
		if(resursive){
			cloneChilds(node.getchildren(), clonedNode);
		}
		return clonedNode;
	}


	@Override
	public BasicNodeTree createDefault(int level) {
		BasicNodeTree defNode = new BasicNodeTree(level, defaultC);
		defNode.setVisible(true);
		return defNode;
	}

	@Override
	public String getInformationTreeNode(INodeTree node) {
		
		RegistEnvNode n = testCast(node);
		return n.getObjectId() + " " + ((n.isValidPath()?"\t« " + n.getOrder()+ "\t" +((n.getResult()==null)?"Θ ":" " +n.getResult()):"» ") );

//		return n.getObjectId() + " " + ((n.isValidPath()?"☺ " + n.getOrder()+ "\t" +((n.getResult()==null)?"Θ ":"√ " +n.getResult()):"■ ") );
	}

	private RegistEnvNode testCast(INodeTree node){
		return (RegistEnvNode)node;
	}

	public boolean isValidToRun(INodeTree node) {
		
		Set<Integer> objs = new HashSet<Integer>();
		getObjectIdParent(objs, node);
		boolean b = objs.containsAll(mandatoryObjects);
		return b;
	}

	protected void getObjectIdParent(Set<Integer> objIds, INodeTree node){
		
		if(RegistEnvNode.class.isAssignableFrom(node.getClass())){
			objIds.add(node.getLevel());
		}
		if(node.getParent()!=null)
			getObjectIdParent(objIds, node.getParent());
	}
	
	
	
	@Override
	public void addNode(INodeTree parent, INodeTree newNode) {
		super.addNode(parent, newNode);
		RegistEnvNode n = (RegistEnvNode) newNode;	
	}
	
	@Override
	public Collection<? extends INodeTree> getAllNodeWith(INodeTree n,
			Object... args) {
		Collection<INodeTree> ret = (Collection<INodeTree>) getAllNodeChilds(n.getchildren(), args);
		
		if(((RegistEnvNode)n).getObjectId().equals(args[0]))
			ret.add(n);
		
		return ret;
	}
	
	public Integer setOrder(INodeTree n, Integer order){
		return (Integer) this.iterateAllNodes(new CalculateOrder(), n,false, order, orderedNodes);
	}
	
	public Integer setOrderReverse(INodeTree n, Integer order){
		return (Integer) this.iterateAllNodes(new CalculateOrder(), n,true, order, orderedNodes);
	}

	public TreeMap<Integer, INodeTree> getOrder() {
		return orderedNodes;
	}

}
