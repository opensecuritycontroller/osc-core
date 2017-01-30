package org.osc.core.agent.dpaipc;

import java.io.Serializable;
import java.util.HashMap;

import org.osc.core.rest.client.agent.model.input.EndpointGroupList;

@SuppressWarnings("serial")
public class InterfaceEndpointMap implements Serializable {

    public HashMap<String, InterfaceEntry> interfaceEndpointMap = new HashMap<String, InterfaceEntry>();

    public void updateInterfaceEndpointMap() {
    }

    public void updateProfileServiceContainer(String interfaceTag, EndpointGroupList endpointSet) {
        if (endpointSet == null) {
            interfaceEndpointMap.remove(interfaceTag);
        } else {
            interfaceEndpointMap.put(interfaceTag, new InterfaceEntry(interfaceTag, endpointSet));
        }
    }

    public void updateInterfaceEndpointMap(InterfaceEndpointMap interfaceEndpointMap) {
        this.interfaceEndpointMap = interfaceEndpointMap.interfaceEndpointMap;
    }

    @Override
    public String toString() {
        return "InterfaceEndpointMap [map=" + interfaceEndpointMap + "]";
    }

}
