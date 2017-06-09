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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.UUID;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;

public class PortGroupCheckMetaTaskTestData {
    public static SecurityGroup SG_WITHOUT_NET_ELEMENT_ID = createSG("SG_WITHOUT_NET_ELEMENT_ID", null);
    public static SecurityGroup SG_WITH_NET_ELEMENT_ID_FOR_DELETE = createSG("SG_WITH_NET_ELEMENT_ID_FOR_DELETE", UUID.randomUUID().toString());
    public static SecurityGroup SG_WITH_NET_ELEMENT_ID_FOR_UPDATE = createSG("SG_WITH_NET_ELEMENT_ID_FOR_DELETE", UUID.randomUUID().toString());

    public static TaskGraph updatePortGroupGraph(SecurityGroup sg, String domainId) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new UpdatePortGroupTask().create(sg, createPortGroup(sg.getNetworkElementId(), domainId)));
        return expectedGraph;
    }

    public static TaskGraph createPortGroupGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new CreatePortGroupTask().create(sg));
        return expectedGraph;
    }

    public static TaskGraph deletePortGroupGraph(SecurityGroup sg, String domainId) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeletePortGroupTask().create(sg, createPortGroup(sg.getNetworkElementId(), domainId)));
        return expectedGraph;
    }

    private static SecurityGroup createSG(String name, String netElementId) {
        VirtualizationConnector vc = createVC(name);

        SecurityGroup sg = new SecurityGroup(vc, UUID.randomUUID().toString(), name + "_tenant");
        sg.setName(name + "_SG");
        sg.setNetworkElementId(netElementId);
        return sg;
    }

    private static VirtualizationConnector createVC(String name) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(name + "_vc");
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setProviderIpAddress(name + "_providerIp");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        return vc;
    }

    private static PortGroup createPortGroup(String id, String parentId) {
        PortGroup portGroup = new PortGroup() {
            @Override
            public boolean equals(Object obj) {
                if (obj == null || !(obj instanceof PortGroup)) {
                    return false;
                }

                PortGroup portGroup = (PortGroup) obj;

                return portGroup.getElementId().equals(getElementId()) && portGroup.getParentId().equals(getParentId());
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder().append(getElementId()).append(getParentId()).toHashCode();
            }
        };

        portGroup.setPortGroupId(id);
        portGroup.setParentId(parentId);

        return portGroup;
    }
}
