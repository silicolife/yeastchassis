package com.silicolife.yeastchassis.env.tasks.r;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.rcaller.rStuff.RCaller;
import com.github.rcaller.rStuff.RCode;

public class REnvironment {
	
	public static RCaller				_caller;
	public static final String 			_rScriptExecutable = "/usr/bin/Rscript";
	public static final RCode			_code			= new RCode();
	public static final List<String>	_resources		= new ArrayList<String>();
	public static final List<String>	_dependencies	= new ArrayList<String>();
														
	static {
		_resources.add("resources/constants.R");
		_resources.add("resources/data.R");
		_resources.add("resources/clustering.R");
		_resources.add("resources/utils.R");
		_resources.add("resources/myheatmap.R");
		
		_dependencies.add("ggplot2");
		_dependencies.add("cluster");
		_dependencies.add("reshape");
		_dependencies.add("caret");
		_dependencies.add("RWeka");
		_dependencies.add("e1071");
		_dependencies.add("gridExtra");
		_dependencies.add("plyr");
		_dependencies.add("xtable");
		_dependencies.add("pastecs");
		_dependencies.add("stringr");
		_dependencies.add("scales");
		_dependencies.add("plyr");
		_dependencies.add("ggdendro");
		_dependencies.add("combinat");
		_dependencies.add("gplots");
		_dependencies.add("plotmath");
		_dependencies.add("gtools");
		reload();
	}
	
	public static void reload() {
		_caller = new RCaller();
		_caller.setRscriptExecutable(_rScriptExecutable);
		_caller.cleanRCode();
		_caller.redirectROutputToStream(System.out);
		_caller.setRCode(_code);
		_code.clear();
		loadResources();
		loadDependencies();
	}
	
	private static void loadResources() {
		for (String resource : _resources) {
			InputStream inputStream = REnvironment.class.getResourceAsStream(resource);
			File f = null;
			try {
				f = File.createTempFile("temp", "R");
				OutputStream out = new FileOutputStream(f);
				int length = 0;
				byte[] bytes = new byte[1024];
				
				while ((length = inputStream.read(bytes)) != -1) {
					out.write(bytes, 0, length);
				}
				
				inputStream.close();
				out.flush();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			_code.R_source(f.getAbsolutePath());
		}
	};
	
	private static void loadDependencies() {
		for (String dep : _dependencies)
			_code.R_require(dep);
	}
	
}