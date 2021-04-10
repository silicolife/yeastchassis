package com.silicolife.yeastchassis.env.components;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.collection.CollectionUtils;

public abstract class AbstractMultRegistTreeEnv extends AbstractMultiRegistEnv {
	
	private static final long serialVersionUID = 1L;
	
	protected abstract List<String> createOrderValues();
	
	protected abstract Set<String> createMandatoryValues();	
	
	protected abstract String infoTreeString(Object result);
	
	protected abstract String infoConsoleToRun(Object result);
	
	protected RegistEnvTree	tree;
	protected List<String>	ordv;
	
	protected Map<String, Object> results;
	protected Map<String, RegistEnvNode> nodesAssociatedToResults;
	
	public AbstractMultRegistTreeEnv() {
		super();
		ordv = createOrderValues();
		Set<String> mandatory = createMandatoryValues();
		Set<String> ids = CollectionUtils.getSetDiferenceValues(ordv, getPossibleClassIds());
		Set<String> testMandatory = CollectionUtils.getSetDiferenceValues(mandatory, getPossibleClassIds());
		if (ids.size() > 0) throw new RuntimeException("Undefined Ids " + ids);
		if (testMandatory.size() > 0) throw new RuntimeException("Undefined madatoryIds Ids " + testMandatory);
		this.tree = new RegistEnvTree(ordv, mandatory);
		results = new HashMap<String, Object>();
		nodesAssociatedToResults = new HashMap<>();
	}
	
	public void printTree() {
		try {
			tree.printTree();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveTreeToFile(String file) {
		try {
			tree.saveTreeToFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public RegistEnvTree getTree() {
		return tree;
	}
	
	public void add(String classId, String objectId) {
		tree.addNode(classId, objectId);
	}
	
	public void add(String parentObjectId, String classId, String objectId) {
		tree.addNodeInObjectId(parentObjectId, classId, objectId);	
	}
	
	public void addNodeAt(String nodeId, String classId, String objectId) {
		tree.addNodeAt(nodeId, classId, objectId);
	}
	
	public void removeNode(String nodeId) {
		tree.removeNode(nodeId);
	}
	
	public List<String> getOrderedValues() {
		return ordv;
	}
		
	
	public void changeResult(String key, Object result){
		results.put(key, result);
		nodesAssociatedToResults.get(key).setResult(infoTreeString(result));
	}
	

	public String generateKeyResults(Map<Integer, String> info) {
		return info.toString();
	}
	
	public Map<String, String> convert(Map<Integer, String> info) {
		LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
		for (int level : info.keySet()) {
			String classId = getOrderedValues().get(level);
			String objectId = info.get(level);
			ret.put(classId, objectId);
		}
		
		return ret;
	}
	
	protected Set<Integer> convertToIdx(Collection<String> classIds) {
		
		Set<String> test = CollectionUtils.getSetDiferenceValues(classIds, ordv);
		if (test.size() > 0) throw new RuntimeException("Undefined Class Ids " + test);
		
		Set<Integer> ret = new TreeSet<>();
		for (String c : classIds) {
			ret.add(getOrderedValues().indexOf(c));
		}
		
		return ret;
	}
	
	public Object getResult(String key) {
		return results.get(key);
	}
	
	protected String generateKeyResultByNode(INodeTree node){
		GetValidatedNodes valNodes = new GetValidatedNodes();
		Map<Integer, String> map = valNodes.run(node);
		if(map == null){
			throw new RuntimeException("["+node.getId()+"] is not a result node");
		}
		return generateKeyResults(map);
	}
	
	protected String generateKeyResultByNodeID(String nodeID){
		return generateKeyResultByNode(getTree().getNode(nodeID));
	}
	
	public void printDetailedInfoResult(String nodeID) throws IOException{
		writeDetailedInfoResult(new OutputStreamWriter(System.out), nodeID);
	}
	
	public void saveDetailedInfoResult(String file, String nodeID) throws IOException{
		FileWriter f = new FileWriter(file);
		writeDetailedInfoResult(f, nodeID);
		f.close();
	}
	
	public void writeDetailedInfoResult(Writer w, String nodeID) throws IOException{
		String keyResult = generateKeyResultByNodeID(nodeID);
		Object resultObj = getResult(keyResult);
		if(resultObj == null){
			w.write("["+nodeID+"] does not have result\n");
		}else{
			w.write(infoConsoleToRun(resultObj)+"\n");
		}
		w.flush();
	}
}
