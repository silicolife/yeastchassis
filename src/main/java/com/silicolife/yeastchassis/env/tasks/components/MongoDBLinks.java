package com.silicolife.yeastchassis.env.tasks.components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MongoDBLinks {
	
	public static final String dbLinksFile = MongoDBLinks.class.getResource("/extra/sgd.tsv").getPath().toString();
	
	public static Map<String,String> SYSTEMATIC_TO_STANDARD = null;
	
	public static Map<String,String> SYSTEMATIC_TO_SGD = null;
	
	static{
		try {
			loadLinks();
		} catch (IOException e) {		
			e.printStackTrace();
		}
	}
		
	private static void loadLinks() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(dbLinksFile));
		
		SYSTEMATIC_TO_STANDARD = new HashMap<String,String>();
		SYSTEMATIC_TO_SGD = new HashMap<String,String>();
		
		br.readLine(); //skip header
		while(br.ready()){
			String line = br.readLine();
			String[] tokens = line.split("\t");
			if(tokens.length>1){
				String sgd = tokens[0].trim();
				String sys = tokens[1].trim();
				if(tokens.length>2){
					String std = tokens[2].trim();
					SYSTEMATIC_TO_STANDARD.put(sys, std);				
				}
				
				SYSTEMATIC_TO_SGD.put(sys, sgd);				
			}
		}
		
		br.close();
	}

	public static Map<String,String> getSystematicToStandard(){
		if(SYSTEMATIC_TO_STANDARD==null){
			try {
				loadLinks();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return SYSTEMATIC_TO_STANDARD;
	}
	
	public static Map<String,String> getSystematicToSGD(){
		if(SYSTEMATIC_TO_SGD==null){
			try {
				loadLinks();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return SYSTEMATIC_TO_SGD;
	}
	
	/**
	 * Systematic IDs to Saccharomyces Genome Database (SGD) IDs
	 * 
	 * @param systematicIDs
	 * @return
	 */
	public static List<String> getSystematicToSGD(List<String> systematicIDs){
		List<String> sgdIDs = new ArrayList<String>();
		for(String sys : systematicIDs){
			sgdIDs.add(MongoDBLinks.SYSTEMATIC_TO_SGD.get(sys));		
		}
		return sgdIDs;
	}
	
	/**
	 * Systematic IDs to standardIDs conversion
	 * 
	 * @param systematicIDs
	 * @return
	 */
	public static List<String> getSystematicToSTD(List<String> systematicIDs){
		List<String> stdIDs = new ArrayList<String>();
		for(String sys : systematicIDs){				
			String stdID = (MongoDBLinks.SYSTEMATIC_TO_STANDARD.containsKey(sys)) ? MongoDBLinks.SYSTEMATIC_TO_STANDARD.get(sys) : sys;
			stdIDs.add(stdID);
		}
		return stdIDs;
	}
		
	
	public static List<String> getSystematicToSTDWithQualifiers(List<String> systematicIDs){
		List<String> stdIDs = new ArrayList<String>();
		for(String sys : systematicIDs){
			stdIDs.add(getSystematicToSTDWithQualifiers(sys));
		}
		return stdIDs;
	}
	
	public static String getSystematicToSTDWithQualifiers(String systematicID){
		String[] sysTKS = systematicID.split("#");
		if(sysTKS.length>1){
			String qual = sysTKS[0].trim();
			String sysElem = sysTKS[1].trim();
			String stdID = (MongoDBLinks.SYSTEMATIC_TO_STANDARD.containsKey(sysElem)) ? MongoDBLinks.SYSTEMATIC_TO_STANDARD.get(sysElem) : sysElem;				
			return qual+"#"+stdID;
		}else{
			String stdID = (MongoDBLinks.SYSTEMATIC_TO_STANDARD.containsKey(systematicID)) ? MongoDBLinks.SYSTEMATIC_TO_STANDARD.get(systematicID) : systematicID;
			return stdID;
		}
	}
	
	public static List<String> getSystematicToSTDDropQualifiers(List<String> systematicIDs){
		List<String> stdIDs = new ArrayList<String>();
		for(String sys : systematicIDs){
			String[] sysTKS = sys.split("#");
			String sysElem = sysTKS[1].trim();
			String stdID = (MongoDBLinks.SYSTEMATIC_TO_STANDARD.containsKey(sysElem)) ? MongoDBLinks.SYSTEMATIC_TO_STANDARD.get(sysElem) : sysElem;
			stdIDs.add(stdID);
		}
		return stdIDs;
	}
	
	public static List<String> getSystematicToSGDWithQualifiers(List<String> systematicIDs){
		List<String> sgdIDs = new ArrayList<String>();
		for(String sys : systematicIDs){
			String[] sysTKS = sys.split("#");
			String qual = sysTKS[0].trim();
			String sysElem = sysTKS[1].trim();
			sgdIDs.add(qual+"#"+MongoDBLinks.SYSTEMATIC_TO_SGD.get(sysElem));		
		}
		return sgdIDs;
	}
}
