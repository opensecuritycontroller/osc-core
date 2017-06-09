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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.DowngradeLockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupInterfaceElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.base.Objects;

@Component(service = MgrSecurityGroupInterfacesCheckMetaTask.class)
public class MgrSecurityGroupInterfacesCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(MgrSecurityGroupInterfacesCheckMetaTask.class);

    @Reference
    ApiFactoryService apiFactoryService;

    @Reference
    CreateMgrSecurityGroupInterfaceTask createMgrSecurityGroupInterfaceTask;

    @Reference
    DeleteMgrSecurityGroupInterfaceTask deleteMgrSecurityGroupInterfaceTask;

    @Reference
    UpdateMgrSecurityGroupInterfaceTask updateMgrSecurityGroupInterfaceTask;

    @Reference
    DowngradeLockObjectTask downgradeLockObjectTask;

    private VirtualSystem vs;
    private DistributedAppliance da;
    private TaskGraph tg;

    private UnlockObjectTask mcUnlockTask;

    /**
     * Start SecurityGroupInterfaces check task. A write lock exists on the mc for the duration of this task and any
     * tasks kicked off by this task.
     * <p>
     * If the unlock task is provided, it automatically UPGRADES the lock to a write lock and will always DOWNGRADE the
     * lock once the tasks are finished. The lock is NOT RELEASED and the provider of the lock should release the lock.
     * </p>
     * <p>
     * If unlock task is not provided(null) then we acquire a write lock and RELEASE it after the tasks are finished.
     * </p>
     */
    public MgrSecurityGroupInterfacesCheckMetaTask create(DistributedAppliance da, UnlockObjectTask mcUnlockTask) {
        MgrSecurityGroupInterfacesCheckMetaTask task = new MgrSecurityGroupInterfacesCheckMetaTask();
        task.apiFactoryService = this.apiFactoryService;
        task.createMgrSecurityGroupInterfaceTask = this.createMgrSecurityGroupInterfaceTask;
        task.deleteMgrSecurityGroupInterfaceTask = this.deleteMgrSecurityGroupInterfaceTask;
        task.updateMgrSecurityGroupInterfaceTask = this.updateMgrSecurityGroupInterfaceTask;
        task.da = da;
        task.mcUnlockTask = mcUnlockTask;
        task.downgradeLockObjectTask = this.downgradeLockObjectTask;
        if (da != null) {
            task.name = task.getName();
        }
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    public MgrSecurityGroupInterfacesCheckMetaTask create(VirtualSystem vs) {
        MgrSecurityGroupInterfacesCheckMetaTask task = create(null, null);
        task.vs = vs;
        task.name = task.getName();
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        boolean isLockProvided = true;

        this.tg = new TaskGraph();

        ApplianceManagerConnector mc = null;
        if (this.da != null) {
            this.da = em.find(DistributedAppliance.class, this.da.getId());
            mc = this.da.getApplianceManagerConnector();
        } else {
            this.vs = em.find(VirtualSystem.class, this.vs.getId());
            mc = this.vs.getDistributedAppliance().getApplianceManagerConnector();
        }

        try {

            if (this.mcUnlockTask == null) {
                isLockProvided = false;
                this.mcUnlockTask = LockUtil.lockMC(mc, LockType.WRITE_LOCK);
            } else {
                // Upgrade to write lock. Will no op if we already have write lock
                boolean upgradeLockSucceeded = LockManager.getLockManager().upgradeLockWithWait(
                        new LockRequest(this.mcUnlockTask));
                if (!upgradeLockSucceeded) {
                    throw new VmidcBrokerInvalidRequestException("Fail to gain write lock for '"
                            + this.mcUnlockTask.getObjectRef().getType() + "' with name '"
                            + this.mcUnlockTask.getObjectRef().getName() + "'.");
                }
            }
            this.tg = new TaskGraph();
            if (this.da != null) {
                for (VirtualSystem vs : this.da.getVirtualSystems()) {
                    this.tg.addTaskGraph(syncSecurityGroupInterfaces(em, vs));
                }
            } else {
                this.tg.addTaskGraph(syncSecurityGroupInterfaces(em, this.vs));
            }

            if (this.tg.isEmpty() && !isLockProvided) {
                LockManager.getLockManager().releaseLock(new LockRequest(this.mcUnlockTask));
            } else {
                if (isLockProvided) {
                    // downgrade lock since we upgraded it at the start
                    this.tg.appendTask(this.downgradeLockObjectTask.create(new LockRequest(this.mcUnlockTask)),
                            TaskGuard.ALL_PREDECESSORS_COMPLETED);
                } else {
                    this.tg.appendTask(this.mcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                }
            }

        } catch (Exception ex) {
            // If we experience any failure, unlock MC.
            if (this.mcUnlockTask != null && !isLockProvided) {
                log.info("Releasing lock for MC '" + mc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(this.mcUnlockTask));
            }
            throw ex;
        }
    }

    public TaskGraph syncSecurityGroupInterfaces(EntityManager em, VirtualSystem vs) throws Exception {

        TaskGraph tg = new TaskGraph();
        if (vs.getId() == null) {
            return tg;
        }

        Set<SecurityGroupInterface> securityGroupInterfaces = vs.getSecurityGroupInterfaces();
        List<? extends ManagerSecurityGroupInterfaceElement> mgrSecurityGroupInterfaces;
        try (ManagerSecurityGroupInterfaceApi mgrApi = this.apiFactoryService.createManagerSecurityGroupInterfaceApi(vs)) {
            mgrSecurityGroupInterfaces = mgrApi.listSecurityGroupInterfaces();
        }

        for (SecurityGroupInterface sgi : securityGroupInterfaces) {
            if (!sgi.getMarkedForDeletion()) {
                // Try to locate by Manager Id
                ManagerSecurityGroupInterfaceElement mgrSgi = findByVmidcSecurityGroupId(mgrSecurityGroupInterfaces,
                        sgi.getMgrSecurityGroupIntefaceId());
                if (mgrSgi == null) {
                    // It is possible it exists but have not been persisted in database.
                    // Search security group by name
                    mgrSgi = findBySecurityGroupByName(mgrSecurityGroupInterfaces, sgi.getName());
                }

                if (mgrSgi == null) {
                    // Add new security group to Manager
                    tg.appendTask(this.createMgrSecurityGroupInterfaceTask.create(sgi));
                } else {
                    if (isInterfaceNeedUpdate(sgi, mgrSgi)) {
                        if (sgi.getMgrSecurityGroupIntefaceId() == null && mgrSgi.getSecurityGroupInterfaceId() != null) {
                            sgi.setMgrSecurityGroupIntefaceId(mgrSgi.getSecurityGroupInterfaceId());
                        }
                        tg.appendTask(this.updateMgrSecurityGroupInterfaceTask.create(sgi));
                    }
                }
            }
        }

        // Remove any security groups which has no policy binding
        for (ManagerSecurityGroupInterfaceElement mgrSgi : mgrSecurityGroupInterfaces) {
            SecurityGroupInterface sgi = findVmidcSecurityGroupByMgrId(securityGroupInterfaces, mgrSgi);
            if (sgi == null || sgi.getMarkedForDeletion()) {
                // Delete security group from Manager
                tg.appendTask(this.deleteMgrSecurityGroupInterfaceTask.create(vs, mgrSgi));
            }
        }
        return tg;
    }

    private static boolean isInterfaceNeedUpdate(SecurityGroupInterface securityGroup,
            ManagerSecurityGroupInterfaceElement mgrSecurityGroup) {
        return !Objects.equal(mgrSecurityGroup.getPolicyId(), securityGroup.getMgrPolicyId())
                || !Objects.equal(mgrSecurityGroup.getTag(), securityGroup.getTag())
                || !Objects.equal(mgrSecurityGroup.getName(), securityGroup.getName());
    }

    private static SecurityGroupInterface findVmidcSecurityGroupByMgrId(Set<SecurityGroupInterface> securityGroups,
            ManagerSecurityGroupInterfaceElement mgrSecurityGroup) {
        for (SecurityGroupInterface securityGroup : securityGroups) {
            String sgiId = securityGroup.getMgrSecurityGroupIntefaceId();

            if (sgiId != null) {
                if (sgiId.equals(mgrSecurityGroup.getSecurityGroupInterfaceId())) {
                    return securityGroup;
                }
            }
        }
        return null;
    }

    private static ManagerSecurityGroupInterfaceElement findBySecurityGroupByName(
            List<? extends ManagerSecurityGroupInterfaceElement> mgrSecurityGroups, String securityGroupName) {
        for (ManagerSecurityGroupInterfaceElement mgrSecurityGroup : mgrSecurityGroups) {
            if (mgrSecurityGroup.getName().equals(securityGroupName)) {
                return mgrSecurityGroup;
            }
        }
        return null;
    }

    private static ManagerSecurityGroupInterfaceElement findByVmidcSecurityGroupId(
            List<? extends ManagerSecurityGroupInterfaceElement> mgrSecurityGroups, String securityGroupId) {
        for (ManagerSecurityGroupInterfaceElement mgrSecurityGroup : mgrSecurityGroups) {
            if (mgrSecurityGroup.getSecurityGroupInterfaceId().equals(securityGroupId)) {
                return mgrSecurityGroup;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        if (this.da != null) {
            return "Checking Traffic Policy Mappings of DA '" + this.da.getName() + "' with Appliance Manager '"
                    + this.da.getApplianceManagerConnector().getName() + "'";
        } else {
            return "Checking Traffic Policy Mappings of VS '" + this.vs.getName() + "' with Appliance Manager '"
                    + this.vs.getDistributedAppliance().getApplianceManagerConnector().getName() + "'";
        }
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        if (this.da != null) {
            return LockObjectReference.getObjectReferences(this.da.getApplianceManagerConnector());
        } else {
            return LockObjectReference.getObjectReferences(this.vs.getDistributedAppliance()
                    .getApplianceManagerConnector());
        }
    }

}
