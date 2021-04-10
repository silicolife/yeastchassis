package com.silicolife.yeastchassis.env.components;

import java.io.Serializable;
import java.util.Collection;


public interface INodeTreeContoler extends Serializable{

	INodeTree createNode(int level, Object... args );
	INodeTree createDefault(int level);
	String getInformationTreeNode(INodeTree node);
	void addNode(INodeTree parent, INodeTree newNode);
	INodeTree clone(INodeTree node, boolean recursive);
	Collection<? extends INodeTree> getAllNodeWith(INodeTree n, Object... args);
	Object iterateAllNodes(IOperationNodeTree op, INodeTree node,
			boolean iterateFirst, Object[] args);
	void addNodeNotRecusive(INodeTree parent, INodeTree node);
	
}
