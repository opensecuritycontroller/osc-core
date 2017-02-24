package org.osc.core.server.installer;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Artifact {

	String getName();

	/**
	 * Returns the hash for the artifact content if available, otherwise
	 * {@code null}.
	 */
	Hash getHash();

	/**
	 * Get the location for the artifact, which may be the location of an
	 * existing installed bundle or a physical URI from which the artifact may
	 * be fetched.
	 */
	String getLocation();

}
