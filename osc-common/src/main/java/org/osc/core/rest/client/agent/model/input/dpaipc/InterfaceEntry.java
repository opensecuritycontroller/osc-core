package org.osc.core.rest.client.agent.model.input.dpaipc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.agent.model.input.EndpointGroupList;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class InterfaceEntry implements Comparable<InterfaceEntry> {

    private String interfaceTag;
    private EndpointGroupList endpointSet;

    public String getInterfaceTag() {
        return interfaceTag;
    }

    public EndpointGroupList getEndpointSet() {
        return endpointSet;
    }

    public InterfaceEntry() {

    }

    public InterfaceEntry(String interfaceTag, EndpointGroupList containerSet) {
        this.interfaceTag = interfaceTag;
        this.endpointSet = containerSet;
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
        return "InterfaceEntry [interfaceTag=" + interfaceTag + ", EndpointSet=" + endpointSet + "]";
    }

}
