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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.NetworkElement;

public class PortGroupCheckTask extends TransactionalTask {

    private SecurityGroup sg;
    private SdnControllerApi controller;
    boolean deleteTg;

    public PortGroupCheckTask(SecurityGroup sg, SdnControllerApi controller, boolean deleteTg) {
        this.sg = sg;
        this.deleteTg = deleteTg;
        this.controller = controller;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sg = em.find(SecurityGroup.class, this.sg.getId());

        Set<SecurityGroupMember> members = this.sg.getSecurityGroupMembers();
        List<NetworkElement> protectedPorts = new ArrayList<>();

        for (SecurityGroupMember sgm : members) {
            if (!sgm.getMarkedForDeletion()) {
                protectedPorts.addAll(OpenstackUtil.getPorts(sgm));
            }
        }
        String domainId = OpenstackUtil.extractDomainId(this.sg.getTenantId(), this.sg.getTenantName(),
                this.sg.getVirtualizationConnector(), protectedPorts);
        String portGroupId = this.sg.getNetworkElementId();
        PortGroup portGroup = new PortGroup();
        portGroup.setPortGroupId(portGroupId);

        if (portGroupId != null) {

            if (this.deleteTg) {
                this.controller.deleteNetworkElement(portGroup);
            } else {
                if (portGroup.getParentId() == null) {
                    portGroup.setParentId(domainId);
                }
                for (NetworkElement elem : protectedPorts) {
                    if (elem.getParentId() == null) {
                        ((VMPort) elem).setParentId(domainId);
                    }
                }
                NetworkElement pGrp = this.controller.updateNetworkElement(portGroup, protectedPorts);
                if (pGrp != null && !pGrp.getElementId().equals(portGroup.getElementId())) {
                    //portGroup was deleted outside OSC, recreated portGroup above
                    this.sg.setNetworkElementId(pGrp.getElementId());
                    em.merge(this.sg);
                }
            }
        } else {
            if (CollectionUtils.isNotEmpty(protectedPorts)) {
                for (NetworkElement vmPort : protectedPorts) {
                    ((VMPort) vmPort).setParentId(domainId);
                }
                NetworkElement portGp = this.controller.registerNetworkElement(protectedPorts);
                if (portGp != null) {
                    this.sg.setNetworkElementId(portGp.getElementId());
                    em.merge(this.sg);
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Port Group '%s' members", this.sg.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }
}
