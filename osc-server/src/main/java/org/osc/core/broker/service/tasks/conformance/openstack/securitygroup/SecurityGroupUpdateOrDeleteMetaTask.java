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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.openstack4j.model.compute.Server;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.securitygroup.AddSecurityGroupService;
import org.osc.core.broker.service.securitygroup.exception.SecurityGroupMemberPartOfAnotherSecurityGroupException;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.IgnoreCompare;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.CheckServiceFunctionChainMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.DeleteMgrSecurityGroupTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.DeleteSecurityGroupInterfaceTask;
import org.osc.core.common.job.TaskGuard;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the Security Group members and syncs them if needed
 */
@Component(service = SecurityGroupUpdateOrDeleteMetaTask.class)
public class SecurityGroupUpdateOrDeleteMetaTask extends TransactionalMetaTask {

    @Reference
    PortGroupCheckMetaTask portGroupCheckMetaTask;

    @Reference
    CheckServiceFunctionChainMetaTask checkServiceFunctionChainMetaTask;

    @Reference
    CheckPortGroupHookMetaTask checkPortGroupHookMetaTask;

    @Reference
    DeleteMgrSecurityGroupTask deleteMgrSecurityGroupTask;

    @Reference
    DeleteSecurityGroupInterfaceTask deleteSecurityGroupInterfaceTask;

    @Reference
    SecurityGroupMemberMapPropagateMetaTask securityGroupMemberMapPropagateMetaTask;

    @Reference
    DeleteSecurityGroupFromDbTask deleteSecurityGroupFromDbTask;

    @Reference
    SecurityGroupMemberVmCheckTask securityGroupMemberVmCheckTask;

    @Reference
    SecurityGroupMemberNetworkCheckTask securityGroupMemberNetworkCheckTask;

    @Reference
    SecurityGroupMemberSubnetCheckTask securityGroupMemberSubnetCheckTask;

    @Reference
    VmPortHookRemoveTask vmPortHookRemoveTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    // optional+dynamic to break circular reference
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ServiceReference<AddSecurityGroupService> addSecurityGroupServiceSR;
    AddSecurityGroupService addSecurityGroupService;

    private final Logger log = LoggerFactory.getLogger(SecurityGroupUpdateOrDeleteMetaTask.class);

    private SecurityGroup sg;
    private TaskGraph tg;

    @IgnoreCompare
    private SecurityGroupUpdateOrDeleteMetaTask factory;
    @IgnoreCompare
    private final AtomicBoolean initDone = new AtomicBoolean();

    private BundleContext context;

    public SecurityGroupUpdateOrDeleteMetaTask create(SecurityGroup sg) {
        SecurityGroupUpdateOrDeleteMetaTask task = new SecurityGroupUpdateOrDeleteMetaTask();
        task.factory = this;
        task.sg = sg;
        this.name = task.getName();
        return task;
    }

    @Override
    protected void delayedInit() {
        if (this.factory.initDone.compareAndSet(false, true)) {
            this.factory.addSecurityGroupService = this.factory.context
                    .getService(this.factory.addSecurityGroupServiceSR);
        }
        this.addSecurityGroupService = this.factory.addSecurityGroupService;
        this.portGroupCheckMetaTask = this.factory.portGroupCheckMetaTask;
        this.checkServiceFunctionChainMetaTask = this.factory.checkServiceFunctionChainMetaTask;
        this.checkPortGroupHookMetaTask = this.factory.checkPortGroupHookMetaTask;
        this.deleteMgrSecurityGroupTask = this.factory.deleteMgrSecurityGroupTask;
        this.deleteSecurityGroupInterfaceTask = this.factory.deleteSecurityGroupInterfaceTask;
        this.securityGroupMemberMapPropagateMetaTask = this.factory.securityGroupMemberMapPropagateMetaTask;
        this.deleteSecurityGroupFromDbTask = this.factory.deleteSecurityGroupFromDbTask;
        this.securityGroupMemberVmCheckTask = this.factory.securityGroupMemberVmCheckTask;
        this.securityGroupMemberNetworkCheckTask = this.factory.securityGroupMemberNetworkCheckTask;
        this.securityGroupMemberSubnetCheckTask = this.factory.securityGroupMemberSubnetCheckTask;
        this.vmPortHookRemoveTask = this.factory.vmPortHookRemoveTask;
        this.apiFactoryService = this.factory.apiFactoryService;
        this.dbConnectionManager = this.factory.dbConnectionManager;
        this.txBroadcastUtil = this.factory.txBroadcastUtil;
    }

    @Activate
    private void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    private void deactivate() {
        if (this.initDone.get()) {
            this.context.getService(this.factory.addSecurityGroupServiceSR);
        }
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        delayedInit();
        this.sg = em.find(SecurityGroup.class, this.sg.getId());

        this.tg = new TaskGraph();

        if (this.sg.getMarkedForDeletion()) {
            this.log.info("Security Group " + this.sg.getName() + " marked for deletion, deleting Endpoint Group");
            // If the SDN supports PG hook we need to retrieve the domainId before
            // the members are deleted.
            String domainId = null;
            if (this.apiFactoryService.supportsPortGroup(this.sg)) {
                VMPort sgMemberPort = OpenstackUtil.getAnyProtectedPort(this.sg);

                domainId = OpenstackUtil.extractDomainId(this.sg.getProjectId(),
                        this.sg.getVirtualizationConnector().getProviderAdminProjectName(),
                        this.sg.getVirtualizationConnector(), Arrays.asList(new NetworkElementImpl(sgMemberPort)));

                if (domainId == null) {
                    throw new VmidcBrokerValidationException(
                            String.format("No domain found for port %s.", sgMemberPort));
                }
            }

            buildTaskGraph(em, true, domainId);
        } else {
            this.log.info("Checking Security Group " + this.sg.getName());

            if (this.sg.isProtectAll()) {
                // Mark all current entities as deleted, as we read them they will get unmarked for deletion.
                for (SecurityGroupMember sgm : this.sg.getSecurityGroupMembers()) {
                    OSCEntityManager.markDeleted(em, sgm, this.txBroadcastUtil);
                }
                List<String> excludedMembers = DistributedApplianceInstanceEntityMgr.listOsServerIdByVcId(em,
                        this.sg.getVirtualizationConnector().getId());

                Endpoint endPoint = new Endpoint(this.sg.getVirtualizationConnector(), this.sg.getProjectName());
                try (Openstack4JNova nova = new Openstack4JNova(endPoint)) {
                    Set<String> regions = nova.listRegions();
                    for (String region : regions) {
                        List<? extends Server> servers = nova.listServers(region);
                        for (Server server : servers) {
                            if (!excludedMembers.contains(server.getId())) {
                                try {
                                    this.addSecurityGroupService.addSecurityGroupMember(em, this.sg,
                                            new SecurityGroupMemberItemDto(region, server.getName(), server.getId(),
                                                    SecurityGroupMemberType.VM.toString(), false));
                                    // Once the VM is part of the security group, dont try to add it again.
                                    excludedMembers.add(server.getId());
                                } catch (SecurityGroupMemberPartOfAnotherSecurityGroupException e) {
                                    this.log.warn(String.format(
                                            "Member '%s' belonging to Security Group '%s' with protect all results in a conflict",
                                            e.getMemberName(), this.sg.getName()), e);
                                    this.tg.addTask(new FailedWithObjectInfoTask(
                                            String.format("Validating Security Group Member '%s'", e.getMemberName()),
                                            e, LockObjectReference.getObjectReferences(this.sg)));
                                }
                            }
                        }
                    }
                }
            }
            buildTaskGraph(em, false, null);
        }

    }

    private void buildTaskGraph(EntityManager em, boolean isDeleteTg, String domainId) throws Exception {
        VmDiscoveryCache vdc = new VmDiscoveryCache(this.sg.getVirtualizationConnector(),
                this.sg.getVirtualizationConnector().getProviderAdminProjectName());

		if (this.apiFactoryService.supportsNeutronSFC(this.sg.getVirtualizationConnector().getControllerType())) {

			// if SFC binded and network element null -> create PC
			// if SFC binded and network element not null -> update PC
			// if SFC not binded and network element not null -> delete PC

			if (this.sg.getServiceFunctionChain() != null
					|| (this.sg.getServiceFunctionChain() == null && this.sg.getNetworkElementId() != null)) {
				this.tg.appendTask(this.checkServiceFunctionChainMetaTask.create(this.sg),
						TaskGuard.ALL_PREDECESSORS_SUCCEEDED);
			}

		}

        // SGM Member sync with no task deferred
        addSGMemberSyncJob(em, isDeleteTg, vdc);

        if (this.sg.getVirtualizationConnector().isControllerDefined()
                && this.apiFactoryService.supportsPortGroup(this.sg)) {
            this.tg.appendTask(this.portGroupCheckMetaTask.create(this.sg, isDeleteTg, domainId),
                    TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }

        for (SecurityGroupInterface sgi : this.sg.getSecurityGroupInterfaces()) {
            List<Task> tasksToSucceedToDeleteSGI = new ArrayList<>();
            if (this.apiFactoryService.supportsPortGroup(this.sg)) {
                CheckPortGroupHookMetaTask checkPortGroupMT = this.checkPortGroupHookMetaTask.create(sgi, isDeleteTg);
                this.tg.appendTask(checkPortGroupMT);
                tasksToSucceedToDeleteSGI.add(checkPortGroupMT);
            }

            if (sgi.getMarkedForDeletion() || isDeleteTg) {
                VirtualSystem vs = sgi.getVirtualSystem();
                if (this.apiFactoryService.syncsSecurityGroup(vs)) {
                    try (ManagerSecurityGroupApi mgrSgApi = this.apiFactoryService.createManagerSecurityGroupApi(vs)) {
                        ManagerSecurityGroupElement mepg = mgrSgApi.getSecurityGroupById(sgi.getMgrSecurityGroupId());
                        if (mepg != null) {
                            DeleteMgrSecurityGroupTask mgrSecurityGroupDelTask = this.deleteMgrSecurityGroupTask
                                    .create(vs, mepg);
                            this.tg.appendTask(mgrSecurityGroupDelTask);
                            tasksToSucceedToDeleteSGI.add(mgrSecurityGroupDelTask);
                        }
                    }
                }

                // Mostly applies only when SGI marked for deletion(not delete tg case) In delete tg case, the members
                // are already deleted and this essentially a no op. The tasks SecurityGroupMemberVmCheckTask etc
                // need to make sure hooks are removed in case of delete tg.
                boolean shouldRemoveHooks = !this.apiFactoryService.supportsPortGroup(this.sg)
                        && !this.apiFactoryService.supportsNeutronSFC(this.sg);
                if (shouldRemoveHooks) {
                    tasksToSucceedToDeleteSGI.addAll(addSGMemberRemoveHooksTask(em, sgi));
                }

                // Ensure removal of mapping for all DAIs before removing SGI.
                this.tg.addTask(this.deleteSecurityGroupInterfaceTask.create(sgi), TaskGuard.ALL_PREDECESSORS_SUCCEEDED,
                        tasksToSucceedToDeleteSGI.toArray(new Task[0]));
            }
        }

        this.tg.appendTask(this.securityGroupMemberMapPropagateMetaTask.create(this.sg),
                TaskGuard.ALL_PREDECESSORS_COMPLETED);

        if (isDeleteTg) {
            this.tg.appendTask(this.deleteSecurityGroupFromDbTask.create(this.sg), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        }

    }

    private void addSGMemberSyncJob(EntityManager em, boolean isDeleteTg, VmDiscoveryCache vdc) {
        // add SG Member Sync task
        for (SecurityGroupMember sgm : this.sg.getSecurityGroupMembers()) {
            if (isDeleteTg) {
                OSCEntityManager.markDeleted(em, sgm, this.txBroadcastUtil);
            }
            if (sgm.getType() == SecurityGroupMemberType.VM) {
                this.tg.appendTask(this.securityGroupMemberVmCheckTask.create(sgm, sgm.getVm(), vdc),
                        TaskGuard.ALL_PREDECESSORS_COMPLETED);
            } else if (sgm.getType() == SecurityGroupMemberType.NETWORK) {
                this.tg.appendTask(this.securityGroupMemberNetworkCheckTask.create(sgm, sgm.getNetwork(), vdc),
                        TaskGuard.ALL_PREDECESSORS_COMPLETED);
            } else if (sgm.getType() == SecurityGroupMemberType.SUBNET) {
                this.tg.appendTask(this.securityGroupMemberSubnetCheckTask.create(sgm, sgm.getSubnet(), vdc),
                        TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }
        }
    }

    private List<Task> addSGMemberRemoveHooksTask(EntityManager em, SecurityGroupInterface sgiMarkedForDeletion) {
        List<Task> tasksAdded = new ArrayList<>();
        // add SG Member hook remove task
        VirtualSystem vs = sgiMarkedForDeletion.getVirtualSystem();
        for (SecurityGroupMember sgm : this.sg.getSecurityGroupMembers()) {
            // If SGM is marked for deletion, previous tasks should have removed the hooks and deleted the member
            // from DB
            if (!sgm.getMarkedForDeletion()) {
                Set<VMPort> ports = sgm.getVmPorts();
                for (VMPort port : ports) {
                    DistributedApplianceInstance assignedRedirectedDai = DistributedApplianceInstanceEntityMgr
                            .findByVirtualSystemAndPort(em, vs, port.getId(), VMPort.class);
                    VmPortHookRemoveTask hookRemoveTask = this.vmPortHookRemoveTask.create(sgm, port,
                            assignedRedirectedDai, vs.getDistributedAppliance().getName());
                    this.tg.appendTask(hookRemoveTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                    tasksAdded.add(hookRemoveTask);
                }
            }
        }

        return tasksAdded;
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group '%s' members", this.sg.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
