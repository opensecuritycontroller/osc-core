package org.osc.core.broker.model.plugin.manager;

public class ServiceUnavailableException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServiceUnavailableException(String className) {
		super("Service unavailable: " + className);
	}
	
}
