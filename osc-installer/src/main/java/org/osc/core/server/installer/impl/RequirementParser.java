package org.osc.core.server.installer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.jboss.osgi.repository.internal.RequirementBuilderImpl;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

class RequirementParser {

	static List<Requirement> parseRequireBundle(String header) throws IllegalArgumentException {
		if (header == null) {
            return Collections.emptyList();
        }

		Clause[] clauses = Parser.parseHeader(header);
		List<Requirement> requirements = new ArrayList<>(clauses.length);
		for (Clause requireClause : clauses) {
			String bsn = requireClause.getName();
			String versionRangeStr = requireClause.getAttribute(org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE);

			String filter = toBundleFilter(bsn, versionRangeStr);
			Requirement requirement = new RequirementBuilderImpl(BundleNamespace.BUNDLE_NAMESPACE)
				.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter)
				.build();
			requirements.add(requirement);
		}
		return requirements;
	}

	static List<Requirement> parseRequireCapability(String header) throws IllegalArgumentException {
		if (header == null) {
            return Collections.emptyList();
        }

		Clause[] clauses = Parser.parseHeader(header);
		List<Requirement> reqs = new ArrayList<>(clauses.length);
		for (Clause clause : clauses) {
			String namespace = clause.getName();

			RequirementBuilderImpl reqBuilder = new RequirementBuilderImpl(namespace);
			for (Attribute attrib : clause.getAttributes()) {
                reqBuilder.addAttribute(attrib.getName(), attrib.getValue());
            }
			for (Directive directive : clause.getDirectives()) {
                reqBuilder.addDirective(directive.getName(), directive.getValue());
            }

			reqs.add(reqBuilder.build());
		}

		return reqs;
	}

	private static String toBundleFilter(String bsn, String versionRangeStr) {
		final String filterStr;

		String bsnFilter = String.format("(%s=%s)", BundleNamespace.BUNDLE_NAMESPACE, bsn);

		if (versionRangeStr == null) {
			filterStr = bsnFilter;
		} else {
			VersionRange versionRange = new VersionRange(versionRangeStr);
			if (versionRange.isExact()) {
				String exactVersionFilter = String.format("(%s=%s)", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, versionRange.getLeft());
				filterStr = String.format("(&%s%s)", bsnFilter, exactVersionFilter);
			} else if (versionRange.getRight() == null) {
				filterStr = String.format("(&%s%s)", bsnFilter, lowerVersionFilter(versionRange));
			} else {
				filterStr = String.format("(&%s%s%s)", bsnFilter, lowerVersionFilter(versionRange), upperVersionFilter(versionRange));
			}
		}
		return filterStr;
	}

	private static String upperVersionFilter(VersionRange versionRange) {
		String upperVersionFilter;
		if (versionRange.getRightType() == VersionRange.RIGHT_CLOSED) {
            upperVersionFilter = String.format("(%s<=%s)", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, versionRange.getRight());
        } else {
            upperVersionFilter = String.format("(!(%s>=%s))", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, versionRange.getRight());
        }
		return upperVersionFilter;
	}

	private static String lowerVersionFilter(VersionRange versionRange) {
		String lowerVersionFilter;
		if (versionRange.getLeftType() == VersionRange.LEFT_CLOSED) {
            lowerVersionFilter = String.format("(%s>=%s)", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, versionRange.getLeft());
        } else {
            lowerVersionFilter = String.format("(!(%s<=%s))", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, versionRange.getLeft());
        }
		return lowerVersionFilter;
	}

}
