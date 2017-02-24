package org.osc.core.server.resolver;

public interface PluginResolver {

	ResolveResult resolve(ResolveRequest request) throws Exception;

}
