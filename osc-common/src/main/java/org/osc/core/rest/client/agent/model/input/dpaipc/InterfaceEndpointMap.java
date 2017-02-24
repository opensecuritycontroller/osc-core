/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
