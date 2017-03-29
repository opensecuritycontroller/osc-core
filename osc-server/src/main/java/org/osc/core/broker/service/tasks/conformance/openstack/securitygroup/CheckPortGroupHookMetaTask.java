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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionHookElement;

public final class CheckPortGroupHookMetaTask extends TransactionalMetaTask {
    private SecurityGroupInterface sgi;
    private TaskGraph tg;
    private static final Logger LOG = Logger.getLogger(CheckPortGroupHookMetaTask.class);
    private final boolean isDeleteTaskGraph;

    public CheckPortGroupHookMetaTask(SecurityGroupInterface sgi, boolean isDeleteTg) {
        this.sgi = sgi;
        this.isDeleteTaskGraph = isDeleteTg;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());
        VMPort protectedPort = getAnyProtectedPort(this.sgi);
        SecurityGroupMember sgm = this.sgi.getSecurityGroup().getSecurityGroupMembers().iterator().next();

        DistributedApplianceInstance assignedRedirectedDai = DistributedApplianceInstanceEntityMgr
                .findByVirtualSystemAndPort(em, this.sgi.getVirtualSystem(), protectedPort);

        if (assignedRedirectedDai == null) {
            LOG.info("No assigned DAI found for port " + protectedPort.getId());
        }

        InspectionHookElement existingInspHook = null;

        if (this.sgi.getNetworkElementId() != null) {
            try (SdnRedirectionApi redirection = SdnControllerApiFactory.createNetworkRedirectionApi(this.sgi.getVirtualSystem())) {
                existingInspHook = redirection.getInspectionHook(this.sgi.getNetworkElementId());
            }
        }

        // If not a deletion then create or update.
        if (!this.sgi.getMarkedForDeletion() && !this.isDeleteTaskGraph) {
            if (existingInspHook == null)
            {
                assignedRedirectedDai = assignedRedirectedDai == null ? getDeployedDAI(sgm, protectedPort, em) : assignedRedirectedDai;
                this.tg.appendTask(new AllocateDAIWithSGIMembersTask(this.sgi, assignedRedirectedDai));
                this.tg.appendTask(new CreatePortGroupHookTask(this.sgi, assignedRedirectedDai));
            } else {
                if (assignedRedirectedDai == null) {
                    throw new VmidcBrokerValidationException(
                            String.format("An inspection hook was found in the SDN controller but "
                                    + "a DAI was not found assigned to the SGI %s.", this.sgi.getName()));
                }

                // If the existing inspection hook has ingress or egress port IDs
                // different than the found assigned DAI then update the inspection hook.
                if (!existingInspHook.getInspectionPort().getIngressPort().getElementId()
                        .equals(assignedRedirectedDai.getInspectionOsIngressPortId()) ||
                        !existingInspHook.getInspectionPort().getEgressPort().getElementId()
                        .equals(assignedRedirectedDai.getInspectionOsEgressPortId())) {

                    this.tg.appendTask(new UpdatePortGroupHookTask(this.sgi, assignedRedirectedDai));
                }
            }
        } else {
            if (existingInspHook != null) {
                this.tg.appendTask(new RemovePortGroupHookTask(this.sgi));
            }

            if (assignedRedirectedDai != null) {
                this.tg.appendTask(new DeallocateDAIOfSGIMembersTask(this.sgi, assignedRedirectedDai));
            }
        }
    }

    private VMPort getAnyProtectedPort(SecurityGroupInterface sgi) {
        // Retrieving the first security group member
        for (SecurityGroupMember sgm : this.sgi.getSecurityGroup().getSecurityGroupMembers()) {
            // If SGM is marked for deletion, previous tasks should have removed the hooks and deleted the member from D.
            if (!sgm.getMarkedForDeletion()) {
                if (sgm.getType() == SecurityGroupMemberType.VM) {
                    return sgm.getVm().getPorts().iterator().next();
                } else if (sgm.getType() == SecurityGroupMemberType.NETWORK) {
                    return sgm.getNetwork().getPorts().iterator().next();
                } else if (sgm.getType() == SecurityGroupMemberType.SUBNET) {
                    return sgm.getSubnet().getPorts().iterator().next();
                }
            }
        }

        return null;
    }

    private DistributedApplianceInstance getDeployedDAI(SecurityGroupMember sgm, VMPort protectedPort, EntityManager em) throws Exception {
        String sgmDomainId = OpenstackUtil.extractDomainId(
                sgm.getSecurityGroup().getTenantId(),
                sgm.getSecurityGroup().getVirtualizationConnector().getProviderAdminTenantName(),
                sgm.getSecurityGroup().getVirtualizationConnector(),
                OpenstackUtil.getPorts(sgm));

        if (StringUtils.isBlank(sgmDomainId)) {
            throw new VmidcBrokerValidationException(String.format("No router/domain was found attached to any of the networks of "
                    + "the member %s of the security group %s.", sgm.getMemberName(), sgm.getSecurityGroup().getName()));
        }

        String tenantId = this.sgi.getSecurityGroup().getTenantId();

        if (sgm.getType().equals(SecurityGroupMemberType.SUBNET) && sgm.getSubnet().isProtectExternal()
                && protectedPort.getVm() == null) {
            if (SdnControllerApiFactory.supportsOffboxRedirection(sgm.getSecurityGroup())) {
                return OpenstackUtil.findDeployedDAI(
                        em,
                        this.sgi.getVirtualSystem(),
                        this.sgi.getSecurityGroup(),
                        tenantId,
                        getMemberRegion(sgm),
                        sgmDomainId,
                        null);
            } else {
                throw new VmidcBrokerValidationException(
                        "Protecting External Traffic feature is not supported by your SDN controller. Please make sure your SDN controller supports offboxing");
            }
        }

        // TODO emanoel: This is currently returning a DAI deployed on the host
        // of the first found VM. If the SDN controller does not support offbox
        // redirection this may fail if the SG has VMs deployed on different hosts.
        return OpenstackUtil.findDeployedDAI(
                em,
                this.sgi.getVirtualSystem(),
                sgm.getSecurityGroup(),
                tenantId,
                getMemberRegion(sgm),
                sgmDomainId,
                protectedPort.getVm().getHost());
    }

    private String getMemberRegion(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getRegion();
        case NETWORK:
            return sgm.getNetwork().getRegion();
        case SUBNET:
            return sgm.getSubnet().getRegion();
        default:
            throw new VmidcBrokerValidationException("Openstack Id is not applicable for Members of type '" + sgm.getType()
            + "'");
        }
    }

    @Override
    public String getName() {
        return String.format("Create the inspection hook for the security group interface '%s' ", this.sgi.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }
}