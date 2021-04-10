package com.silicolife.yeastchassis.env.components;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BasicNodeControler implements INodeTreeContoler{

	private static final long serialVersionUID = 1L;

	@Override
	public BasicNodeTree createNode(int level, Object... args) {
		return new BasicNodeTree(level, this);
	}
	
	public INodeTree clone(INodeTree node, boolean resursive){
		BasicNodeTree clonedNode = new BasicNodeTree(node.getLevel(), node.getControler());
		clonedNode.setVisible(node.isVisible());
		if(resursive){
			cloneChilds(node.getchildren(), clonedNode);
		}
		return clonedNode;
	}

	protected void cloneChilds(List<INodeTree> getchildren, INodeTree parent) {
		
		
		for(INodeTree n : getchildren){
			INodeTree newN = n.getControler().clone(n, true);
			associatePaternity(parent, newN);
		}
	}

	@Override
	public BasicNodeTree createDefault(int level) {
		BasicNodeTree defNode = new BasicNodeTree(level, this);
		defNode.setVisible(true);
		return defNode;
	}

	@Override
	public String getInformationTreeNode(INodeTree node) {
		return (node.isVisible())?node.getId():"";
	}

	public void addNodeNotRecusive(INodeTree parent, INodeTree newNode){
		
		if((parent.getLevel()+1) == newNode.getLevel()){
			INodeTree clonedNode = newNode.getControler().clone(newNode, true);
			associatePaternity(parent, clonedNode);
			
		}else{
			INodeTree newDef = createDefault(newNode.getLevel()-1);
			associatePaternity(newDef, newNode);
			addNodeNotRecusive(parent, newDef);
		}
	}
	
	@Override
	public void addNode(INodeTree parent, INodeTree newNode) {
		
		if((parent.getLevel()+1) == newNode.getLevel()){
			testDefault(parent);

			INodeTree clonedNode = newNode.getControler().clone(newNode, true);
			clonedNode.setParent(parent);
			
			parent.getchildren().add(clonedNode);
	
		}else if(parent.getLevel()+1 < newNode.getLevel()){
			if(parent.getChildCount() == 0){
				INodeTree defNode =newNode.getControler().createDefault(parent.getLevel()+1);
				defNode.setParent(parent);
				parent.getchildren().add(defNode);
			}
			
			for(INodeTree n : parent.getchildren()){
				addNode(n, newNode);
			}
		}
		
	}
	
	protected void associatePaternity(INodeTree parent, INodeTree child){
		child.setParent(parent);
		parent.getchildren().add(child);
	}

	private void testDefault(INodeTree parent) {
		
		
		for(int i =0; i < parent.getChildCount(); i++){
			if(!(parent.getChildAt(i).isVisible()) && parent.getChildAt(i).getChildCount() ==0)
				parent.removeChildNode(1);
			
		}
	}

	@Override
	public Collection<? extends INodeTree> getAllNodeWith(INodeTree n,
			Object... args) {
		return getAllNodeChilds(n.getchildren(), args);
	}
	
	protected Collection<? extends INodeTree> getAllNodeChilds(Collection<INodeTree> childs, Object... args){
		Set<INodeTree> set = new LinkedHashSet<INodeTree>();
		for(INodeTree n : childs){
			set.addAll(n.getControler().getAllNodeWith(n, args));
		}
		return set;
	}

	@Override
	public Object iterateAllNodes(IOperationNodeTree op, INodeTree node, boolean isreverse,
			Object... args) {
		
		Object[] nextArgs = args;
		Object r = null;
		
		if(isreverse){
			r = iterateChilds(node.getchildren(), isreverse, op, r, nextArgs);
			r = op.run(node, op.calculateNextArgs(r,nextArgs));
		}else{
			r = op.run(node, op.calculateNextArgs(r,nextArgs));
			r = iterateChilds(node.getchildren(),isreverse, op, r, nextArgs);
		}
				
		
		return r;
	}
	
	public Object iterateChilds(List<INodeTree> nodes, boolean isreverse, IOperationNodeTree op, Object lastRet, Object... args){
		
		if(!isreverse)
			for(INodeTree n : nodes){
				lastRet = iterateAllNodes(op, n, isreverse,op.calculateNextArgs(lastRet, args));
			}
		else
			for(int i = nodes.size()-1 ; i>=0 ; i--)
				lastRet = iterateAllNodes(op, nodes.get(i), isreverse,op.calculateNextArgs(lastRet, args));
		return lastRet;
	}

}
