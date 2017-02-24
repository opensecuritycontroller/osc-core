package org.osc.core.rest.client.agent.model.input.dpaipc;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.agent.model.input.EndpointGroupList;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class InterfaceEndpointMap {

    public HashMap<String, InterfaceEntry> map = new HashMap<String, InterfaceEntry>();

    public void updateInterfaceEndpointMap() {
    }

    public void updateInterfaceEndpointMap(String interfaceTag, EndpointGroupList endpointSet) {
        if (endpointSet == null) {
            map.remove(interfaceTag);
        } else {
            map.put(interfaceTag, new InterfaceEntry(interfaceTag, endpointSet));
        }
    }

    public void updateInterfaceEndpointMap(InterfaceEndpointMap interfaceEndpointMap) {
        this.map = interfaceEndpointMap.map;
    }

    @Override
    public String toString() {
        return "InterfaceEndpointMap [map=" + map + "]";
    }

}
