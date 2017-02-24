package org.osc.core.server.installer;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.util.promise.Promise;

@ProviderType
public interface InstallableUnit {

    /**
     * Get the path to the original archive file from which this installable unit arises.
     */
    File getOrigin();

    String getName();

    String getSymbolicName();

    String getType();

    /**
     * Get the physical artifacts that must be installed into the OSGi Framework
     * in order to fully install this unit. May return {@code null} or empty if
     * the state is {@link State#ERROR}.
     */
    Collection<Artifact> getArtifacts();

    /**
     * Get the current state of this installable unit.
     */
    State getState();

    /**
     * Get any error associated with this installable unit. Likely to be
     * {@code null} unless {@link #getState()} returns {@link State#ERROR}.
     */
    String getErrorMessage();

    /**
     * Install the unit.
     *
     * @return A promise of the list of bundles that were actually installed
     *         into the OSGi Framework as a result of this operation.
     */
    Promise<List<Bundle>> install();

    /**
     * Uninstall the unit.
     *
     * @return A promise of the list of bundles that were actually uninstalled
     *         from the OSGi Framework as a result of this operation.
     */
    Promise<List<Bundle>> uninstall();

}
