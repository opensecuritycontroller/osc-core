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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class PortGroupHookCheckTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(PortGroupHookCheckTask.class);
    private SecurityGroup sg;
    private SecurityGroupInterface sgi;
    private TaskGraph tg;
    private final String serviceName;
    private DistributedApplianceInstance dai;
    private boolean deleteTg;
    private final VmDiscoveryCache vdc;

    public PortGroupHookCheckTask(SecurityGroup sg, SecurityGroupInterface sgi,  boolean isDeleteTg,
            VmDiscoveryCache vdc) {
        this.sg = sg;
        this.sgi = sgi;
        this.deleteTg = isDeleteTg;
        this.serviceName = this.sgi.getVirtualSystem().getDistributedAppliance().getName();
        this.vdc = vdc;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());
        Set<SecurityGroupMember> members = this.sg.getSecurityGroupMembers();
        Set<VMPort> ports = new HashSet<>();
        for (SecurityGroupMember sgm: members){
            if (sgm.getType() == SecurityGroupMemberType.VM) {
                ports = sgm.getVm().getPorts();
            } else if (sgm.getType() == SecurityGroupMemberType.NETWORK) {
                ports = sgm.getNetwork().getPorts();
            } else if (sgm.getType() == SecurityGroupMemberType.SUBNET) {
                ports = sgm.getSubnet().getPorts();
            }
            for (VMPort port : ports) {
                if (port.getMarkedForDeletion()) {
                    LOG.info(String.format("Removing hooks for Stale VM Port with MAC '%s' belonging to %s member '%s'",
                            port.getMacAddresses(), sgm.getType(), sgm.getMemberName()));
                    port.removeAllDais();
                    EntityManager.update(session, port);
                    this.tg.appendTask(new VmPortDeleteFromDbTask(sgm, port));
                } else {
                    if (!this.sgi.getMarkedForDeletion()){
                        LOG.info("SGI(binding) not marked for deletion " + this.sgi + " calling PortGroupExtendedHookCheckTask");
                        this.tg.appendTask(new PortGroupExtendedHookCheckTask(sgm, this.sgi, port, this.vdc));
                    }
                }
            }
        }
    }


    @Override
    public String getName() {
        LOG.info("Checking dai object : " + this.dai);
        return String.format(
                "Creating Inspection Hooks for Port Group Member '%s' to Service '%s'", this.sg.getName(), this.serviceName);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }
}