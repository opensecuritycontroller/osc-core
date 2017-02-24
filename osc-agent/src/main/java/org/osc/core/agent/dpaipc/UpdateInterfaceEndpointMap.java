package org.osc.core.agent.dpaipc;

import java.util.ArrayList;
import java.util.Collection;

import org.osc.core.rest.client.agent.model.input.EndpointGroupList;

public class UpdateInterfaceEndpointMap {
    public final String cmd = "update-interface-endpoint-map";
    public Collection<InterfaceEntry> map = new ArrayList<InterfaceEntry>();

    public UpdateInterfaceEndpointMap(String interfaceTag, EndpointGroupList endpointGroupList) {
        map.add(new InterfaceEntry(interfaceTag, endpointGroupList));
    }

    public UpdateInterfaceEndpointMap(Collection<InterfaceEntry> map) {
        this.map = map;
    }
}
