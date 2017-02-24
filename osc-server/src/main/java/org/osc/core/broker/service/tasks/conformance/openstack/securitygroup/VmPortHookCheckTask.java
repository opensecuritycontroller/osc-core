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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import org.apache.commons.lang.StringUtils;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.InspectionHookElement;

import java.util.Set;

/**
 * This task just adds/update the hooks. If the SGI is marked for deletion, this task does not do anything.
 */
class VmPortHookCheckTask extends TransactionalMetaTask {

    private final Logger log = Logger.getLogger(VmPortHookCheckTask.class);

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private SecurityGroupInterface securityGroupInterface;
    private VMPort vmPort;
    private final String serviceName;
    private final VmDiscoveryCache vdc;

    private VirtualSystem vs;

    public VmPortHookCheckTask(SecurityGroupMember sgm, SecurityGroupInterface bindedSGI, VMPort vmPort,
            VmDiscoveryCache vdc) {
        this.sgm = sgm;
        this.securityGroupInterface = bindedSGI;
        this.vmPort = vmPort;
        this.serviceName = this.securityGroupInterface.getVirtualSystem().getDistributedAppliance().getName();
        this.vdc = vdc;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());

        this.securityGroupInterface = (SecurityGroupInterface) session.get(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());
        this.vmPort = (VMPort) session.get(VMPort.class, this.vmPort.getId());

        this.vs = this.securityGroupInterface.getVirtualSystem();

        DistributedApplianceInstance assignedRedirectedDai = DistributedApplianceInstanceEntityMgr
                .findByVirtualSystemAndPort(session, this.vs, this.vmPort);

        if (!this.securityGroupInterface.getMarkedForDeletion()) {
            if (assignedRedirectedDai == null) {
                this.log.info("No assigned DAI found for port " + this.vmPort);

                String tenantId = this.sgm.getSecurityGroup().getTenantId();

                if (this.sgm.getType().equals(SecurityGroupMemberType.SUBNET) && this.sgm.getSubnet().isProtectExternal()
                        && this.vmPort.getVm() == null) {

                    if (SdnControllerApiFactory.createNetworkControllerApi(this.sgm).isOffboxRedirectionSupported()) {
                        assignedRedirectedDai = OpenstackUtil.findDeployedDAI(session, this.sgm.getMemberRegion(),
                                tenantId, null, this.vs);
                    } else {
                        throw new VmidcBrokerValidationException(
                                "Protecting External Traffic feature is not supported by your SDN controller. Please make sure your SDN controller supports offboxing");
                    }

                } else {
                    assignedRedirectedDai = OpenstackUtil.findDeployedDAI(session, this.sgm.getMemberRegion(),
                            tenantId, this.vmPort.getVm().getHost(), this.vs);
                }

                if (assignedRedirectedDai != null) {
                    // Refresh pessimistically because dai might have been updated by a different transaction
                    session.refresh(assignedRedirectedDai, new LockOptions(LockMode.PESSIMISTIC_WRITE));
                    this.vmPort.addDai(assignedRedirectedDai);
                    assignedRedirectedDai.addProtectedPort(this.vmPort);
                    EntityManager.update(session, this.vmPort);
                } else {
                    throw new VmidcBrokerValidationException(
                            "Couldn't find a relevant Distributed Appliance Instance to protect this Port "
                                    + this.vmPort.getOpenstackId());
                }
            } else {
                this.log.info("Port " + this.vmPort + " assigned to DAI '" + assignedRedirectedDai.getName()
                + "' for service ." + this.serviceName + "'");
            }

            if (StringUtils.isBlank(assignedRedirectedDai.getInspectionOsIngressPortId())
                    || StringUtils.isBlank(assignedRedirectedDai.getInspectionIngressMacAddress())) {
                throw new VmidcBrokerValidationException(
                        "Appliance Instance is not discovered (missing Inspection Port/MAC).");
            }

            this.vdc.discover(assignedRedirectedDai.getDeploymentSpec().getRegion(),
                    assignedRedirectedDai.getOsServerId());

            this.log.info("Checking Inspection Hook for Security group Member: " + this.sgm.getMemberName());

            InspectionHookElement hook;
            try (SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(assignedRedirectedDai)) {
                DefaultNetworkPort ingressPort = new DefaultNetworkPort(
                        assignedRedirectedDai.getInspectionOsIngressPortId(),
                        assignedRedirectedDai.getInspectionIngressMacAddress());
                DefaultNetworkPort egressPort = new DefaultNetworkPort(
                        assignedRedirectedDai.getInspectionOsEgressPortId(),
                        assignedRedirectedDai.getInspectionEgressMacAddress());
                hook = controller.getInspectionHook(this.vmPort, new DefaultInspectionPort(ingressPort, egressPort));
            }

            // Missing tag indicates missing hook
            if (hook == null || (hook.getTag() == null && this.securityGroupInterface.getTag() != null)) {
                this.tg.addTask(new VmPortHookCreateTask(this.vmPort, this.securityGroupInterface,
                        assignedRedirectedDai));
            } else {
                this.log.info("Found Inspection Hook " + hook);

                // Check tag
                if (this.securityGroupInterface.getTagValue() != null && !hook.getTag().equals(this.securityGroupInterface.getTagValue())) {
                    this.tg.appendTask(new VmPortHookTagUpdateTask(this.vmPort, this.securityGroupInterface,
                            assignedRedirectedDai));
                }

                // Check order
                if (!hook.getOrder().equals(this.securityGroupInterface.getOrder())) {
                    this.tg.appendTask(new VmPortHookOrderUpdateTask(this.vmPort, this.securityGroupInterface,
                            assignedRedirectedDai));
                }

                // Check failure policy
                FailurePolicyType failurePolicyType = hook.getFailurePolicyType();
                if (failurePolicyType != null
                        && !failurePolicyType.equals(this.securityGroupInterface.getFailurePolicyType())) {
                    this.tg.appendTask(new VmPortHookFailurePolicyUpdateTask(this.vmPort, this.securityGroupInterface,
                            assignedRedirectedDai));
                }

            }
        }
    }

    @Override
    public String getName() {

        if (this.vmPort.getVm() == null) {
            return String
                    .format("Checking '%s' Service Inspection hooks for SUBNET '%s' with MAC '%s' belonging to Security Group '%s'",
                            this.serviceName, this.vmPort.getSubnet().getName(), this.vmPort.getMacAddresses(), this.sgm
                            .getSecurityGroup().getName());
        } else {
            return String
                    .format("Checking '%s' Service Inspection hooks for VM '%s' with MAC '%s' belonging to Security Group '%s'",
                            this.serviceName, this.vmPort.getVm().getName(), this.vmPort.getMacAddresses(), this.sgm
                            .getSecurityGroup().getName());
        }

    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgm.getSecurityGroup());
    }
}
