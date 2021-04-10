package com.silicolife.yeastchassis.env.components;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;



public abstract class AbstractEnvironment implements IEnvironment, Serializable{
	

	private static final long serialVersionUID = 1L;
	
	
	public void setNullNotSerializableObjects(){
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T loadSerializedObject(String path) throws IOException, ClassNotFoundException{
		return (T)FileUtils.loadSerializableObject(path);
	}
	
	public void saveSerializedObject(String path) throws IOException{
		this.setNullNotSerializableObjects();
		FileUtils.saveSerializableObject(this, path);
	}
	
	public static <T> HashSet<T> convertToHashSet(Collection<T> collection){
		return collection == null? null :new HashSet<T>(collection);
	}
	
	public static <T> LinkedHashSet<T> convertToLinkedHashSet(Collection<T> collection){
		return collection == null ? null : new LinkedHashSet<T>(collection);
	}
	
	public static <T> ArrayList<T> convetToArrayList(Collection<T> collection){
		return collection == null ? null : new ArrayList<T>(collection);
	}
	
	protected Writer getScreenWriter(){
		return new OutputStreamWriter(System.out);
	}
	
	protected Writer getFileWriter(String file) throws IOException{
		return new BufferedWriter(new FileWriter(file));
	}
}
