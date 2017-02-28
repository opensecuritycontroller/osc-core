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
