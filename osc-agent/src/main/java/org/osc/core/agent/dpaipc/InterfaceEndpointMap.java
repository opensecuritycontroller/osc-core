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
