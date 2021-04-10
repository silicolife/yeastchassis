package com.silicolife.yeastchassis.env.components;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import pt.uminho.ceb.biosystems.mew.utilities.java.StringUtils;

public class BasicNodeTree implements INodeTree{


	private static final long serialVersionUID = 1L;
	private INodeTree parent;
	protected List<INodeTree> childs;
	protected INodeTreeContoler controler;
	private String id;
	private int level;
	private boolean isVisible;

	public BasicNodeTree(int level, INodeTreeContoler controler) {
		this.level = level;
		this.controler = controler;
		this.childs = new ArrayList<>();
		this.isVisible = true;
	}

	@Override
	public INodeTree getChildAt(int childIndex) {
		return childs.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return childs.size();
	}

	@Override
	public INodeTree getParent() {
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		return childs.indexOf(node);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	@Override
	public Enumeration<INodeTree> children() {
		return (new Vector<INodeTree>(childs)).elements();
	}

	@Override
	public int getLevel() {
		return level;
	}

	@Override
	public String getId() {
		if(id == null){
			recalculateInfo();
		}
		return id;
	}

	@Override
	public INodeTreeContoler getControler() {
		return controler;
	}

	@Override
	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}


	public void recalculateInfo(){
		int indexParent = 0;
		if(getParent() != null)
			indexParent =getParent().getIndex(this);

		String toIncrementId = null;
		if(getLevel()%2 == 0 )
			toIncrementId = (indexParent+1)+"";
		else
			toIncrementId = StringUtils.generateCharCode(indexParent);


		id = "";
		if(getParent() != null)
			id = getParent().getId() + toIncrementId;
		recalculateChilds();
	}

	public void recalculateChilds(){

		for(INodeTree n : childs){
			n.recalculateInfo();
		}
	}

	public void setParent(INodeTree parent) {
		this.parent = parent;
	}

	public void createChilds(Integer level, Object... args) {
		INodeTree node = getControler().createNode(level);
		getControler().addNode(this, node);
		recalculateChilds();
	}


	public void addChild(INodeTree node){
		getControler().addNode(this, node);
		recalculateChilds();
	}

	@Override
	public List<INodeTree> getchildren() {
		return childs;
	}

	@Override
	public void removeChildNode(int i) {
		childs.remove(i);
	}

	public INodeTree getNode(String id){

		if(getId().equals(id))
			return this;

		if(getId().equals("") || id.startsWith(getId())){
			for(INodeTree f : childs){
				INodeTree n = f.getNode(id);
				if(n != null)
					return n;
			}
		}
		return null;

	}

	public void removeChildNode(INodeTree node){
		boolean t = childs.remove(node);
		if(t) node.setParent(null);
		
		for(INodeTree c : childs)
			c.removeChildNode(node);
	}

	public void setId(String id) {
		this.id = id;
	}

	public void printTest(){
		for(INodeTree n : getchildren()){
			BasicNodeTree b = (BasicNodeTree) n;
			System.out.println(getId() + "\t" +getChildCount()+ "\t" + b.getId() + "\t" + b.getChildCount() + "\t" + n.getClass());
			b.printTest();
		}
	}
	
}
