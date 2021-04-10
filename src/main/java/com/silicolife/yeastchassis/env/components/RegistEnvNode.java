package com.silicolife.yeastchassis.env.components;

import org.apache.log4j.Logger;

public class RegistEnvNode extends BasicNodeTree{

	private static final long serialVersionUID = 1L;

	static private Logger log = Logger.getLogger(RegistEnvNode.class.getName());
	
	private String objectId;
	private boolean isValidPath;
	private Integer order = null;
	private String result;
	
	public RegistEnvNode(int level, String objectId, INodeTreeContoler controler) {
		super(level, controler);
		this.objectId = objectId;
	}
	
	public RegistEnvNodeControler getControler(){
		return (RegistEnvNodeControler)super.getControler();
	}
	
	public void changeOrder(int order){
		getControler().setOrder(this, order);
	}
	
	@Override
	public void recalculateInfo() {
		super.recalculateInfo();
		isValidPath = isLeaf() && getControler().isValidToRun(this);
		
		if(getOrder() == null && isValidPath()){
			getControler().setOrder(this, null);
		}
	}

	public void setOrder(Integer newOrder) {
		order = newOrder;
//		log.info("set Order: " + newOrder + "\t" );
	}

	public String getObjectId() {
		return objectId;
	}
	
	public boolean isValidPath() {
		return isValidPath;
	}
	
	public String getResult() {
		return result;
	}
	
	public void setResult(String result) {
		this.result = result;
	}
	
	public Integer getOrder() {
		return order;
	}
	
	@Override
	public void removeChildNode(INodeTree node) {
		
		RemoveValidNodesToOrder valNodel = new RemoveValidNodesToOrder();
		getControler().iterateAllNodes(valNodel, node, false, getControler().getOrder());
		
		super.removeChildNode(node);
	}

}
