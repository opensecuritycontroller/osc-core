package org.osc.core.broker.service;

import javax.persistence.EntityManager;

public interface ChainedDispatch<T> {

	T dispatch(T input, EntityManager em) throws Exception;
	
}
