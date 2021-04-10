package com.silicolife.yeastchassis.env.components;

import java.io.Serializable;

public interface IOperationNodeTree extends Serializable{

	Object run(INodeTree n, Object... args);
	Object agregateChildReturns(Object... childRets);
	Object[] calculateNextArgs(Object ret, Object... args);
}
