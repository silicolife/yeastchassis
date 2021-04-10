package com.silicolife.yeastchassis.env.components;

import java.io.Serializable;
import java.util.Map;

public interface IEnvStateController<E extends AbstractMultRegistTreeEnv> extends Serializable{
	
	public IRunTreeStatus initialEnvState(Map<String, String> objsMap, E env, Map<String, Object> extraParams);
	
	public IRunTreeStatus updateEnvState(IRunTreeStatus oldState, E env, Map<String, Object> extraParams);
	
}
