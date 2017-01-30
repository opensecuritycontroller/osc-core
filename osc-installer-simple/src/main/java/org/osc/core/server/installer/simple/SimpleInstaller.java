package org.osc.core.server.installer.simple;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.osc.core.server.installer.InstallableListener;
import org.osc.core.server.installer.InstallableUnit;
import org.osc.core.server.installer.InstallableUnitEvent;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;

/**
 * This is a simple installer that immediately installs any resolved installable unit.
 */
@Component
public class SimpleInstaller implements InstallableListener {

	@Override
	public void installableUnitsChanged(Collection<InstallableUnitEvent> events) {
		for (InstallableUnitEvent event : events) {
			InstallableUnit unit = event.getUnit();
			switch (event.getNewState()) {
			case ERROR:
				System.err.printf("ERROR in installable unit %s: %s%n", unit.getSymbolicName(), unit.getErrorMessage());
				break;

			case RESOLVED:
				System.out.printf("RESOLVED installable unit %s with %d artifacts: %s%n", unit.getSymbolicName(), unit.getArtifacts().size(), unit.getArtifacts());
				System.out.printf("... installing %s...%n", unit.getSymbolicName());

				unit.install().then(p -> {
					List<Bundle> bundles = p.getValue();
					System.out.printf("%d bundles installed for %s: %s%n" ,bundles.size(), unit.getSymbolicName(), bundles.stream().map(Bundle::getSymbolicName).collect(Collectors.toList()));
					return null;
				});
				break;

			case INSTALLING:
				System.out.printf("INSTALLING %s...%n", unit.getSymbolicName());
				break;

			case INSTALLED:
				System.out.printf("INSTALLED %s%n", unit.getSymbolicName());
				break;

			case REMOVED:
				System.out.printf("REMOVED %s%n", unit.getSymbolicName());
				break;
			default:
			    System.err.printf("UNKNOWN STATE %s for %s%n", event.getNewState(), unit.getSymbolicName());
			}
		}
	}

}
