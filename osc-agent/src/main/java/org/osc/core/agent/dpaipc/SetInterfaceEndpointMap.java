package org.osc.core.agent.dpaipc;

import java.util.Collection;

public class SetInterfaceEndpointMap {
    public final String cmd = "set-interface-endpoint-map";
    public final Collection<InterfaceEntry> map;

    public SetInterfaceEndpointMap(Collection<InterfaceEntry> map) {
        this.map = map;
    }
}
