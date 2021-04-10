package com.silicolife.yeastchassis.env.tasks.r;

import java.io.File;
import java.io.FileFilter;

import com.github.rcaller.rStuff.RCode;
import com.silicolife.yeastchassis.env.AnalysisEnv;
import com.silicolife.yeastchassis.env.tasks.AbstractAnalysisTask;


public abstract class AbstractRTask extends AbstractAnalysisTask{
	
	public boolean reloaded = false;

	public AbstractRTask(String directory, AnalysisEnv env) {
		super(directory, env);
	}
	
	public void run() throws Throwable{
		reloadR();
		super.run();
	}
		
	
	public RCode code(){
		return REnvironment._code;
	}
	
	public void setCode(){
		REnvironment._caller.setRCode(code());
	}
	
	public void runOnly(){
		REnvironment._caller.runOnly();
	}
	
	public void reloadR(){
		REnvironment.reload();
		reloaded = true;
	}
	
	public String treatRfilename(String in){
		in = "\""+in.toString().replace("\\", "/")+"\"";
		in = in.toString().replace("%", "");
		return in;
	}
	
	public abstract FileFilter getInputFileFilter();

	public  String[] loadFiles(){
		return loadFiles(_directory);
	};
	
	public String[] loadFiles(String path) {
		System.out.println("Trying to load ["+getInputFileFilter().toString()+"] from ["+path+"]");
		File[] files = new File(path).listFiles(getInputFileFilter());
		String[] stringFiles = new String[files.length];
		
		for (int i = 0; i < files.length; i++)
			stringFiles[i] = files[i].getAbsolutePath();
		
		return stringFiles;
	}
}
