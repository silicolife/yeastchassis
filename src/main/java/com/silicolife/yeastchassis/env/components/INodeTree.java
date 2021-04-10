package com.silicolife.yeastchassis.env.components;

import java.io.Serializable;
import java.util.List;

import javax.swing.tree.TreeNode;


public interface INodeTree extends TreeNode, Serializable{

	int getLevel();
	String getId();
	INodeTreeContoler getControler();
	boolean isVisible();
	List<INodeTree> getchildren();
	void recalculateInfo();
	INodeTree getChildAt(int childIndex);
	void removeChildNode(int i);
	INodeTree getNode(String id);
	void removeChildNode(INodeTree node);
	void setParent(INodeTree newNode);
	INodeTree getParent();
}
