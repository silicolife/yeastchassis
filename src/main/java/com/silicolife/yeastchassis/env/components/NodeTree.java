package com.silicolife.yeastchassis.env.components;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

public class NodeTree {

	static Logger log = Logger.getLogger(NodeTree.class.getName());

	static public void printNode(INodeTree node,  String prefix, boolean isTail) throws IOException{
		
		writeNode(node,  prefix, isTail, new OutputStreamWriter(System.out));
	}
	
	static public void printChilds(List<INodeTree> childs,  String prefix, boolean isTail) throws IOException{
		
		writeChildren(childs,  prefix, isTail, new OutputStreamWriter(System.out));
	}
	
	static public Writer writeNode(INodeTree node, String prefix, boolean isTail, Writer builder) throws IOException{

		if(node.isVisible()) builder.append(prefix + (isTail ? "└── " : "├── ") + "[" + node.getId() + "] "+ node.getControler().getInformationTreeNode(node)+"\n");
		builder.flush();
		return writeChildren(node.getchildren(),  prefix, isTail, builder);
	}

	static public Writer writeChildren(List<INodeTree> childs , String prefix, boolean isTail , Writer builder) throws IOException{

		for (int i = 0; i < childs.size() - 1; i++) {
			writeNode(childs.get(i),  prefix + (isTail ? "    " : "│   "), false, builder);
		}
		if (childs.size() > 0) {
			writeNode(childs.get(childs.size() - 1),  prefix + (isTail ?"    " : "│   "), true, builder);
		}
		builder.flush();
		return builder;
	}
}
