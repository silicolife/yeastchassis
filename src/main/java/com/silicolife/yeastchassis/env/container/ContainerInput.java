package com.silicolife.yeastchassis.env.container;

import java.io.IOException;
import java.util.Map;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.BioOptFileReader;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.FlatFilesReader;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.JSBMLReader;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.MetatoolReader;
import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;

public enum ContainerInput {
	NONE {
		@Override
		public Container read(String name, Map<String, Object> files) {
			return null;
		}

	},
	SBML {
		@Override
		public Container read(String name, Map<String, Object> files) {
			try {
				JSBMLReader reader = new JSBMLReader(files.get(ContainerEnv.SBML_FILE).toString(), name,false);
				return new Container(reader);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	},
	
	SPARSE_FLAT_FILES {
		@Override
		public Container read(String name, Map<String, Object> files) {
			try {
				FlatFilesReader reader = new FlatFilesReader((String) files.get(ContainerEnv.REACTIONS_FILE), 
						(String) files.get(ContainerEnv.MATRIX_FILE),
						(String) files.get(ContainerEnv.METABOLITES_FILE),
						(String) files.get(ContainerEnv.GENES_FILE), name);;
//					
				return new Container(reader);
			}
			catch(Exception e) {
				e.printStackTrace();
				
			}
			return null;
		}
	},
	BIO_OPT {
		@Override
		public Container read(String name, Map<String, Object> files) {
			try {
				BioOptFileReader reader = new BioOptFileReader((String) files.get(ContainerEnv.BIO_OPT_FILE));
				return new Container(reader);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	},
	METATOOL {
		@Override
		public Container read(String name, Map<String, Object> files) {
			try {
				MetatoolReader reader = new MetatoolReader((String) files.get(ContainerEnv.METATOOL_FILE));
				return new Container(reader);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	},
	MARSHAL {
		@Override
		public Container read(String name, Map<String, Object> input) {
			try {
				return (Container) FileUtils.loadSerializableObject((String) input.get("file"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
	};
	
	public abstract Container read(String name, Map<String, Object> files) throws Exception;	

}
