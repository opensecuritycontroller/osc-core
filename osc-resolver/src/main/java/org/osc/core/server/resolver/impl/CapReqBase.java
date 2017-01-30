package org.osc.core.server.resolver.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Resource;

public class CapReqBase {

	protected final String namespace;
	protected final Map<String, String> directives;
	protected final Map<String, Object> attribs;
	protected final Resource resource;

	public CapReqBase(String namespace, Map<String,String> directives, Map<String, Object> attribs, Resource resource) {
		this.namespace = namespace;
		this.directives = new HashMap<>(directives);
		this.attribs = new HashMap<>(attribs);
		this.resource = resource;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attribs);
	}

	public Resource getResource() {
		return resource;
	}
	
}
