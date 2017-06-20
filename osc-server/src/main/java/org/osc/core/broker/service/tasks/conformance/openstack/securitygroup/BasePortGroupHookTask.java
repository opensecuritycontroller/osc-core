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

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.DefaultNetworkPort;

/**
 * This abstract class represents the common responsibility of
 * the tasks {@link CreatePortGroupHookTask} and {@link UpdatePortGroupHookTask}.
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
public abstract class BasePortGroupHookTask extends TransactionalTask {
    private SecurityGroupInterface sgi;
    private DistributedApplianceInstance dai;
    private DefaultNetworkPort ingressPort;
    private DefaultNetworkPort egressPort;
    private PortGroup portGroup;

    public BasePortGroupHookTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        this.sgi = sgi;
        this.dai = dai;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());
        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());

        String portGroupId = this.sgi.getSecurityGroup().getNetworkElementId();

        if (portGroupId == null) {
            throw new VmidcBrokerValidationException(
                    String.format("The security group %s does not have a network element set.",
                            this.sgi.getSecurityGroup().getName()));
        }

        this.portGroup = new PortGroup();
        this.portGroup.setPortGroupId(portGroupId);

        this.ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                this.dai.getInspectionIngressMacAddress());
        this.egressPort = new DefaultNetworkPort(this.dai.getInspectionOsEgressPortId(),
                this.dai.getInspectionEgressMacAddress());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }

    protected SecurityGroupInterface getSGI() {
        return this.sgi;
    }

    protected DistributedApplianceInstance getDAI() {
        return this.dai;
    }

    protected DefaultNetworkPort getIngressPort() {
        return this.ingressPort;
    }

    protected DefaultNetworkPort getEgressPort() {
        return this.egressPort;
    }

    protected PortGroup getPortGroup() {
        return this.portGroup;
    }

    protected abstract BasePortGroupHookTask create(SecurityGroupInterface sgi, DistributedApplianceInstance dai);
}
