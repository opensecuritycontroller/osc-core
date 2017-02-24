package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import com.google.common.base.Objects;
import org.apache.log4j.Logger;
import org.hibernate.Session;
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
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.DowngradeLockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupInterfaceElement;

import java.util.List;
import java.util.Set;

public class MgrSecurityGroupInterfacesCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(MgrSecurityGroupInterfacesCheckMetaTask.class);

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
    public MgrSecurityGroupInterfacesCheckMetaTask(DistributedAppliance da, UnlockObjectTask mcUnlockTask) {
        this.da = da;
        this.name = getName();
        this.mcUnlockTask = mcUnlockTask;
    }

    public MgrSecurityGroupInterfacesCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        boolean isLockProvided = true;

        this.tg = new TaskGraph();

        ApplianceManagerConnector mc = null;
        if (this.da != null) {
            this.da = (DistributedAppliance) session.get(DistributedAppliance.class, this.da.getId());
            mc = this.da.getApplianceManagerConnector();
        } else {
            this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
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
                    this.tg.addTaskGraph(syncSecurityGroupInterfaces(session, vs));
                }
            } else {
                this.tg.addTaskGraph(syncSecurityGroupInterfaces(session, this.vs));
            }

            if (this.tg.isEmpty() && !isLockProvided) {
                LockManager.getLockManager().releaseLock(new LockRequest(this.mcUnlockTask));
            } else {
                if (isLockProvided) {
                    // downgrade lock since we upgraded it at the start
                    this.tg.appendTask(new DowngradeLockObjectTask(new LockRequest(this.mcUnlockTask)),
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

    public static TaskGraph syncSecurityGroupInterfaces(Session session, VirtualSystem vs) throws Exception {

        TaskGraph tg = new TaskGraph();
        if (vs.getId() == null) {
            return tg;
        }

        Set<SecurityGroupInterface> securityGroupInterfaces = vs.getSecurityGroupInterfaces();
        List<? extends ManagerSecurityGroupInterfaceElement> mgrSecurityGroupInterfaces;
        try (ManagerSecurityGroupInterfaceApi mgrApi = ManagerApiFactory.createManagerSecurityGroupInterfaceApi(vs)) {
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
                    tg.appendTask(new CreateMgrSecurityGroupInterfaceTask(sgi));
                } else {
                    if (isInterfaceNeedUpdate(sgi, mgrSgi)) {
                        if (sgi.getMgrSecurityGroupIntefaceId() == null && mgrSgi.getSecurityGroupInterfaceId() != null) {
                            sgi.setMgrSecurityGroupIntefaceId(mgrSgi.getSecurityGroupInterfaceId());
                        }
                        tg.appendTask(new UpdateMgrSecurityGroupInterfaceTask(sgi));
                    }
                }
            }
        }

        // Remove any security groups which has no policy binding
        for (ManagerSecurityGroupInterfaceElement mgrSgi : mgrSecurityGroupInterfaces) {
            SecurityGroupInterface sgi = findVmidcSecurityGroupByMgrId(securityGroupInterfaces, mgrSgi);
            if (sgi == null || sgi.getMarkedForDeletion()) {
                // Delete security group from Manager
                tg.appendTask(new DeleteMgrSecurityGroupInterfaceTask(vs, mgrSgi));
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
