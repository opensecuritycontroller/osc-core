package org.osc.core.server.resolver;

import java.util.Map;

import org.osgi.resource.Resource;

public interface ResolveResult {

	/**
	 * Return the request that was satisfied by this result.
	 */
	ResolveRequest getRequest();

	/**
	 * Return the resources to install. Each resource is mapped to a location,
	 * which is either the location field of an existing installed bundle or the
	 * physical URL of a location from which the bundle can be installed.
	 */
	Map<Resource, String> getResources();

}
