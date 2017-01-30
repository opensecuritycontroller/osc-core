package org.osc.core.server.resolver.impl;

import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequirementImpl extends CapReqBase implements Requirement {

	public static RequirementImpl copy(Requirement req, Resource resource) {
		return new RequirementImpl(req.getNamespace(), req.getDirectives(), req.getAttributes(), resource);
	}

	public RequirementImpl(String namespace, Map<String, String> directives, Map<String, Object> attribs, Resource resource) {
		super(namespace, directives, attribs, resource);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.namespace);

		String filterStr = this.directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filterStr != null) {
            sb.append(":").append(filterStr);
        }

		for (Entry<String,String> directive : this.directives.entrySet()) {
			if (!Namespace.REQUIREMENT_FILTER_DIRECTIVE.equals(directive.getKey())) {
                sb.append(", ").append(directive.getKey()).append(":=").append(directive.getValue());
            }
		}

		for (Entry<String,Object> attrib : this.attribs.entrySet()) {
			sb.append(", ").append(attrib.getKey()).append("=").append(attrib.getValue());
		}

		return sb.toString();
	}

}
