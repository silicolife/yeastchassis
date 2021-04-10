package com.silicolife.yeastchassis.env.components;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.MapUtils;

public abstract class AbstractMultiRegistEnv extends AbstractEnvironment {
	
	private static final long serialVersionUID = 1L;
	
	static Logger log = org.apache.log4j.Logger.getLogger(AbstractMultiRegistEnv.class);
	
	private Map<String, Class<?>> idToClass;
	
	private Map<String, Map<String, Object>> values;
	
	abstract protected Map<String, Class<?>> possibleRegisteredClasses();
	
	abstract public void loadDefaultConfig();
	
	public AbstractMultiRegistEnv() {
		Map<String, Class<?>> posClass = possibleRegisteredClasses();
		this.idToClass = new TreeMap<String, Class<?>>(posClass);
		values = new HashMap<String, Map<String, Object>>();
	}
	
	protected Class<?> getClass(String id) {
		Class<?> klass = idToClass.get(id);
		if (klass == null) throw new RuntimeException(generateNotIdClassError(id));
		return klass;
	}
	
	public Map<String, Object> getAllObjects(String classId) {
		Map<String, Object> objects = values.get(classId);
		if (objects == null) {
			objects = new TreeMap<String, Object>();
			if (idToClass.containsKey(classId)) values.put(classId, objects);
		}
		return objects;
	}
	
	protected void registValue(String classId, String valueId, Object value) {
		if (valueId == null) throw new NullPointerException("The value id connot be null!!");
		if (value == null) throw new NullPointerException("The " + valueId + " object connot be null!!");
		
		Class<?> klass = getClass(classId);
		if (!klass.isAssignableFrom(value.getClass())) throw new RuntimeException(generateClassProblem(value.getClass(), klass));
		
		Map<String, Object> objects = getAllObjects(classId);
		if (objects.containsKey(valueId)) throw new RuntimeException(generateValueAlreadyExistesError(classId, valueId));
		
		objects.put(valueId, value);
	}
	
	protected boolean testClass(Class<?> klass, Object value) {
		return klass.isAssignableFrom(value.getClass());
	}
	

	protected Object getObject(String classId, String objectId){
		if(objectId == null) return null;
		Class<?> klass = getClass(classId);
		Object ret = null;
		
		try {
			ret = getAllObjects(classId).get(objectId);
		} catch (Exception e) {
		}
		
		if (ret == null || !testClass(klass, ret)) throw new RuntimeException(generateNotExistsObjectError(classId, objectId));
		return ret;
	}
	
	protected String toStringClassesNames(String classId) {
		getClass(classId);
		return MapUtils.prettyToString(getAllObjects(classId));
	}
	
	protected String toStringClassesNames(Collection<String> classIds) {
		StringBuilder builder = new StringBuilder();
		
		for (String classId : classIds) {
			Class<?> klass = null;
			try {
				klass = getClass(classId);
			} catch (Exception e) {
			}
			
			if (klass == null)
				builder.append(classId + " does not exist!\n");
			else
				builder.append(classId + " class=" + idToClass.get(classId) + "\n" + "" + toStringClassesNames(classId));
			builder.append("\n");
		}
		return builder.toString();
	}
	
	protected void printClassesNames(Collection<String> classIds) {
		System.out.println(toStringClassesNames(classIds));
	}
	
	protected void printClassesNames(String classId) {
		System.out.println(toStringClassesNames(classId));
	}
	
	private String generateNotExistsObjectError(String classId, String objectId) {
		return "The " + classId + " with id " + objectId + " does not exist!";
	}
	
	private String generateValueAlreadyExistesError(String classId, String valueId) {
		return "Already exists a " + classId + " with name " + valueId;
	}
	
	private String generateNotIdClassError(String id) {
		
		log.error("[" + id + "]\t" + idToClass.keySet());
		String error = "Id [" + id + "] does not exist! Possibilities:\n" + MapUtils.prettyToString(idToClass);
		return error;
	}
	
	private String generateClassProblem(Class<? extends Object> classValue, Class<?> expectedClass) {
		return "Type mismatch!\n" + "\tValue class:    " + classValue + "\n" + "\tExpected class: " + expectedClass;
	}
	
	public Set<String> getPossibleClassIds() {
		return new HashSet<String>(idToClass.keySet());
	}
	
	@Override
	public void info() {
		printClassesNames(idToClass.keySet());
	}
	
}
