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

import org.jboss.logging.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.NetworkElementImpl;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.api.SdnRedirectionApi;

class VmPortHookCreateTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(VmPortHookCreateTask.class);

    private final String vmName;
    private final String serviceName;
    private VMPort vmPort;
    private DistributedApplianceInstance dai;
    private SecurityGroupInterface securityGroupInterface;

    public VmPortHookCreateTask(VMPort vmPort, SecurityGroupInterface securityGroupInterface,
            DistributedApplianceInstance daiToRedirectTo) {
        this.vmPort = vmPort;
        this.dai = daiToRedirectTo;
        this.securityGroupInterface = securityGroupInterface;
        this.serviceName = this.securityGroupInterface.getVirtualSystem().getDistributedAppliance().getName();
        this.vmName = vmPort.getVm() != null ? vmPort.getVm().getName() : vmPort.getSubnet().getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vmPort = em.find(VMPort.class, this.vmPort.getId());
        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());
        this.securityGroupInterface = em.find(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());
        VirtualSystem vs = this.dai.getVirtualSystem();

        this.log.info(String.format("Creating Inspection Hooks for Security Group Member VM '%s' for service '%s'",
                this.vmName, this.serviceName));
        SdnRedirectionApi controller = SdnControllerApiFactory.createNetworkRedirectionApi(this.dai);

        try {
            DefaultNetworkPort ingressPort = new DefaultNetworkPort(
                    this.dai.getInspectionOsIngressPortId(),
                    this.dai.getInspectionIngressMacAddress());
            DefaultNetworkPort egressPort = new DefaultNetworkPort(
                    this.dai.getInspectionOsEgressPortId(),
                    this.dai.getInspectionEgressMacAddress());

            TagEncapsulationType encapsulationType = vs.getEncapsulationType() != null
                    ? TagEncapsulationType.valueOf(vs.getEncapsulationType().name()) : null;
            if (SdnControllerApiFactory.supportsPortGroup(this.dai.getVirtualSystem())){
                String portGroupId = this.securityGroupInterface.getSecurityGroup().getNetworkElementId();
                if (portGroupId != null){
                    PortGroup portGroup = new PortGroup();
                    portGroup.setPortGroupId(portGroupId);
                    controller.installInspectionHook(portGroup, new DefaultInspectionPort(ingressPort, egressPort),
                            this.securityGroupInterface.getTagValue(), encapsulationType,
                            this.securityGroupInterface.getOrder(), FailurePolicyType.valueOf(this.securityGroupInterface.getFailurePolicyType().name()));
                }
            } else {
                controller.installInspectionHook(new NetworkElementImpl(this.vmPort),
                        new DefaultInspectionPort(ingressPort, egressPort),
                        this.securityGroupInterface.getTagValue(), encapsulationType,
                        this.securityGroupInterface.getOrder(), FailurePolicyType.valueOf(this.securityGroupInterface.getFailurePolicyType().name()));
            }
        } finally {
            controller.close();
        }
    }

    @Override
    public String getName() {
        return String.format(
                "Creating Inspection Hooks for Security Group Member VM '%s' to Service '%s' (DAI '%s', MAC: %s)",
                this.vmName, this.serviceName, this.dai.getName(), this.dai.getInspectionIngressMacAddress());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
