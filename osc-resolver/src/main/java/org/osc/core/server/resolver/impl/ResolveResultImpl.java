package org.osc.core.server.resolver.impl;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.osc.core.server.resolver.ResolveRequest;
import org.osc.core.server.resolver.ResolveResult;
import org.osgi.resource.Resource;

public class ResolveResultImpl implements ResolveResult {
	
	private final ResolveRequest request;
	private final Map<Resource, String> resourceMap = new IdentityHashMap<>();

	public ResolveResultImpl(ResolveRequest request) {
		this.request = request;
	}

	@Override
	public ResolveRequest getRequest() {
		return request;
	}

	@Override
	public Map<Resource, String> getResources() {
		return Collections.unmodifiableMap(resourceMap);
	}

	void addResource(Resource resource, String location) {
		resourceMap.put(resource, location);
	}


}