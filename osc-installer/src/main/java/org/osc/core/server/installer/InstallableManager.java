package org.osc.core.server.installer;

import java.util.Collection;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface InstallableManager {

	Collection<InstallableUnit> getInstallableUnits();

}
