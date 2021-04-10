package com.silicolife.yeastchassis.env.components;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;

public class GenericRootTree extends BasicNodeTree {

	private static final long serialVersionUID = 1L;

	public GenericRootTree(INodeTreeContoler controler ) {
		super(-1, controler);
		setId("");
		setVisible(false);
	}

	public void printTree() throws IOException {
		
		System.out.println(">>>>");
		NodeTree.printChilds(getchildren(), "", true);
		System.out.println("");
	}
	
	public void saveTreeToFile(String file) throws IOException {
		
		FileWriter fw = new FileWriter(file);
		BufferedWriter bf = new BufferedWriter(fw);
		bf.append(">>>>\n");
		NodeTree.writeChildren(getchildren(), "", true, bf);
		bf.close();
		fw.close();
		
	}

	
	protected void addNode(int level, Object... args){
		INodeTree node = getControler().createNode(level, args);
		getControler().addNode(this, node);
	}
	
	protected void addNodeAt(String nodeId,int level, Object... args){
		INodeTree node = getControler().createNode(level, args);
		INodeTree parent = getNode(nodeId);
		getControler().addNodeNotRecusive(parent, node);
	}
	
	protected void addNode(String nodeId, int level, Object...args){
		INodeTree node = getControler().createNode(level, args);

		INodeTree parent = getNode(nodeId);
		getControler().addNode(parent, node);
	}
	

	public void removeNode(String nodeId) {
		INodeTree node = getNode(nodeId);
		INodeTree parent = node.getParent();
		parent.removeChildNode(node);
		parent.recalculateInfo();
	}

	public LinkedHashSet<INodeTree> getAllNodeWith(Object... args){
		
		LinkedHashSet<INodeTree> node = new LinkedHashSet<INodeTree>(); 
		for(INodeTree n : getchildren()){
			node.addAll(n.getControler().getAllNodeWith(n, args));
		}
		return node;
	}
	

	

}
