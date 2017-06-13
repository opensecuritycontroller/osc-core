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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.SecurityGroupMemberElementImpl;
import org.osc.core.broker.model.plugin.manager.SecurityGroupMemberListElementImpl;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.SecurityGroupMemberElement;
import org.osc.sdk.manager.element.SecurityGroupMemberListElement;

class CreateMgrSecurityGroupTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(CreateMgrEndpointGroupTask.class);

    private SecurityGroup sg;
    private VirtualSystem vs;

    public CreateMgrSecurityGroupTask(VirtualSystem vs, SecurityGroup sg) {
        this.vs = vs;
        this.sg = sg;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sg = em.find(SecurityGroup.class, this.sg.getId());

        ManagerSecurityGroupApi mgrApi = ManagerApiFactory.createManagerSecurityGroupApi(this.vs);
        try {
            String iscId = this.sg.getId().toString();
            String mgrEndpointGroupId = mgrApi.createSecurityGroup(this.sg.getName(), iscId,
                    getSecurityGroupMemberListElement(this.sg));
            this.sg.setMgrId(mgrEndpointGroupId);
            OSCEntityManager.update(em, this.sg);

        } finally {
            mgrApi.close();
        }

    }

    static SecurityGroupMemberListElement getSecurityGroupMemberListElement(SecurityGroup sg) throws VmidcBrokerValidationException {
        List<SecurityGroupMemberElement> sgmElements = new ArrayList<>();
        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            SecurityGroupMemberElementImpl sgmElement = new SecurityGroupMemberElementImpl(sgm.getId().toString(),
                    sgm.getMemberName());
            for (VMPort port : getPorts(sgm)) {
                sgmElement.addMacAddresses(port.getMacAddresses());
                sgmElement.addIpAddress(port.getPortIPs());
            }
            sgmElements.add(sgmElement);
        }
        return new SecurityGroupMemberListElementImpl(sgmElements);
    }

    private static Set<VMPort> getPorts(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getPorts();
        case NETWORK:
            return sgm.getNetwork().getPorts();
        case SUBNET:
            return sgm.getSubnet().getPorts();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + sgm.getType() + "'");
        }
    }

    @Override
    public String getName() {
        return "Create Manager Security Group for Virtualization System '"
                + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
