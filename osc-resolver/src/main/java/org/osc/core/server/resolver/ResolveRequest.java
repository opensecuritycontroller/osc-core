package org.osc.core.server.resolver;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Requirement;

/**
 * Represents a request to resolve certain requirements in the context of the
 * current OSGi Framework.
 */
public class ResolveRequest {

    private final String name;
    private final String symbolicName;
    private final String type;
    private final List<URI> indexes;
    private final Collection<Requirement> requirements;

    /**
     * Construct a request.
     *
     * @param indexes
     *            The list of repository indexes to search for installable
     *            resources.
     * @param requirements
     *            The list of requirements to resolve.
     */
    public ResolveRequest(String name, String symbolicName, String type, List<URI> indexes, Collection<Requirement> requirements) {
        this.name = name;
        this.symbolicName = symbolicName;
        this.type = type;
        this.indexes = new ArrayList<>(indexes);
        this.requirements = new ArrayList<>(requirements);
    }

    public String getName() {
        return this.name;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public String getType() {
        return this.type;
    }

    public List<URI> getIndexes() {
        return Collections.unmodifiableList(this.indexes);
    }

    public Collection<Requirement> getRequirements() {
        return Collections.unmodifiableCollection(this.requirements);
    }
}
