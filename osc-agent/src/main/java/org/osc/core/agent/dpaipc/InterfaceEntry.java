package org.osc.core.agent.dpaipc;

import java.io.Serializable;

import org.osc.core.rest.client.agent.model.input.EndpointGroupList;

@SuppressWarnings("serial")
public class InterfaceEntry implements Comparable<InterfaceEntry>, Serializable {

    public String interfaceTag;
    public EndpointGroupList endpointGroupList = new EndpointGroupList();

    public InterfaceEntry(String interfaceTag, EndpointGroupList endpointGroupList) {
        this.interfaceTag = interfaceTag;
        if (endpointGroupList != null) {
            this.endpointGroupList = endpointGroupList;
        }
    }

    @Override
    public int compareTo(InterfaceEntry o) {
        return interfaceTag.compareTo(o.interfaceTag);
    }

    @Override
    public int hashCode() {
        return interfaceTag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof InterfaceEntry)
                && interfaceTag.equals(((InterfaceEntry) obj).interfaceTag);
    }

    @Override
    public String toString() {
        return "InterfaceEntry [interfaceTag=" + interfaceTag + ", endpointGroupList=" + endpointGroupList + "]";
    }

}
