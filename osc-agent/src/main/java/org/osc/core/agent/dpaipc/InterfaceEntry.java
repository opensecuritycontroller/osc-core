/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
