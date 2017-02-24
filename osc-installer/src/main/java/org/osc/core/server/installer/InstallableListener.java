package org.osc.core.server.installer;

import java.util.Collection;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A whiteboard listener for installation units that can be installed into the
 * present OSGi Framework.
 */
@ConsumerType
public interface InstallableListener {

	/**
	 * Notifies that one or more installable units have changed their states.
	 */
	void installableUnitsChanged(Collection<InstallableUnitEvent> events);

}
