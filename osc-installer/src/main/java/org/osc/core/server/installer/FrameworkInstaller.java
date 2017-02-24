package org.osc.core.server.installer;

import java.io.IOException;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * A management agent responsible for installing and uninstalling bundles into
 * the OSGi Framework. It keeps a reference count for bundles that it has
 * installed, and is responsible for uninstalling those bundles when the
 * reference count drops to zero.
 * 
 * Bundles that were not installed by the provider (e.g. pre-existing bundles)
 * should never be uninstalled this provider.
 * 
 * If another management agent modifies the installed bundles then the behaviour
 * is undefined.
 */
@ProviderType
public interface FrameworkInstaller {

	/**
	 * Ensure that bundles exist in the OSGi Framework with the specified
	 * locations; they will be installed from those locations if they do not
	 * already exist. Any bundles installed as a result of this method will be
	 * associated with the given sponsor, and if all sponsors for a bundle are
	 * later removed then the bundle shall be uninstalled.
	 * 
	 * NB sponsors should be compared with value equality, i.e.
	 * {@link Object#equals(Object)}.
	 * 
	 * @return The list of bundles actually installed by this operation, i.e.
	 *         not including those that were already present.
	 * 
	 * @throws BundleException
	 * @throws IOException 
	 */
	List<Bundle> addLocations(Object sponsor, List<String> bundleLocations) throws BundleException, IOException;

	/**
	 * Remove bundles associated with the specified sponsor object.
	 *
	 * @return The list of bundles actually uninstalled by this operation.
	 */
	List<Bundle> removeSponsor(Object sponsor);

}
