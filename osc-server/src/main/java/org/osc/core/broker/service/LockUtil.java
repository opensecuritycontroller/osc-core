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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;

public class LockUtil {

    private static final Logger log = Logger.getLogger(LockUtil.class);
    public static final long DEFAULT_MAX_LOCK_TIMEOUT = 60 * 60 * 1000; // 1 hour

    /**
     * Gets a write lock on the DS only. Throws exception if lock cannot be obtained
     */
    public static UnlockObjectTask tryLockDSOnly(DeploymentSpec ds) throws VmidcBrokerInvalidRequestException,
            InterruptedException {
        return lockObject(new LockObjectReference(ds.getId(), ds.getName(), ObjectType.DEPLOYMENT_SPEC),
                LockType.WRITE_LOCK, true);
    }

    /**
     * Write locks the DS and puts read locks on the DA, MC and VC.
     */
    public static UnlockObjectMetaTask tryLockDS(DeploymentSpec ds, DistributedAppliance da,
            ApplianceManagerConnector mc, VirtualizationConnector vc) throws VmidcBrokerInvalidRequestException,
            InterruptedException {
        return lockDS(ds, da, mc, vc, true);
    }

    /**
     * Write locks the DS and puts read locks on the DA, MC and VC uses the boolean flag to try locking or wait
     */
    public static UnlockObjectMetaTask lockDS(DeploymentSpec ds, DistributedAppliance da, ApplianceManagerConnector mc,
            VirtualizationConnector vc, boolean tryToLock) throws VmidcBrokerInvalidRequestException,
            InterruptedException {
        return lockChildAndParents(tryToLock, new LockObjectReference(ds.getId(), ds.getName(),
                ObjectType.DEPLOYMENT_SPEC), new LockObjectReference(da.getId(), da.getName(),
                ObjectType.DISTRIBUTED_APPLIANCE), new LockObjectReference(vc.getId(), vc.getName(),
                ObjectType.VIRTUALIZATION_CONNECTOR), new LockObjectReference(mc.getId(), mc.getName(),
                ObjectType.APPLIANCE_MANAGER_CONNECTOR));
    }

    /**
     * Read locks the DA and MC its associated with
     */
    public static UnlockObjectMetaTask tryReadLockDA(DistributedAppliance da, ApplianceManagerConnector mc)
            throws Exception {
        return tryLockObjects(LockType.READ_LOCK, new LockObjectReference(da.getId(), da.getName(),
                ObjectType.DISTRIBUTED_APPLIANCE), new LockObjectReference(mc.getId(), mc.getName(),
                ObjectType.APPLIANCE_MANAGER_CONNECTOR));
    }

    /**
     * Write locks the DA and Read locks MC its associated with
     */
    public static UnlockObjectMetaTask tryLockDA(DistributedAppliance da, ApplianceManagerConnector mc)
            throws Exception {
        return tryLockChildAndParents(new LockObjectReference(da.getId(), da.getName(),
                ObjectType.DISTRIBUTED_APPLIANCE), new LockObjectReference(mc.getId(), mc.getName(),
                ObjectType.APPLIANCE_MANAGER_CONNECTOR));
    }

    public static UnlockObjectMetaTask lockDA(DistributedAppliance da, ApplianceManagerConnector mc)
            throws Exception {
        return lockChildAndParents(false, new LockObjectReference(da.getId(), da.getName(),
                ObjectType.DISTRIBUTED_APPLIANCE), new LockObjectReference(mc.getId(), mc.getName(),
                ObjectType.APPLIANCE_MANAGER_CONNECTOR));
    }

    public static UnlockObjectTask lockVC(VirtualizationConnector vc, LockType lockType) throws Exception {
        return lockObject(new LockObjectReference(vc.getId(), vc.getName(), ObjectType.VIRTUALIZATION_CONNECTOR),
                lockType, false);
    }

    public static UnlockObjectMetaTask tryLockVC(VirtualizationConnector vc, LockType lockType) throws Exception {
        return tryLockObjects(lockType, new LockObjectReference(vc.getId(), vc.getName(),
                ObjectType.VIRTUALIZATION_CONNECTOR));
    }

    public static UnlockObjectTask tryLockVCObject(VirtualizationConnector vc, LockType lockType) throws Exception {
        return lockObject(new LockObjectReference(vc.getId(), vc.getName(), ObjectType.VIRTUALIZATION_CONNECTOR),
                lockType, true);
    }

    public static UnlockObjectTask lockMC(ApplianceManagerConnector mc, LockType lockType) throws Exception {
        return lockObject(new LockObjectReference(mc.getId(), mc.getName(), ObjectType.APPLIANCE_MANAGER_CONNECTOR),
                lockType, false);
    }

    public static UnlockObjectTask tryLockMC(ApplianceManagerConnector mc, LockType lockType) throws Exception {
        return lockObject(new LockObjectReference(mc.getId(), mc.getName(), ObjectType.APPLIANCE_MANAGER_CONNECTOR),
                lockType, true);
    }

    public static UnlockObjectTask tryLockSecurityGroupOnly(SecurityGroup sg)
            throws VmidcBrokerInvalidRequestException, InterruptedException {
        return lockObject(new LockObjectReference(sg.getId(), sg.getName(), ObjectType.SECURITY_GROUP),
                LockType.WRITE_LOCK, true);
    }

    public static UnlockObjectMetaTask tryLockSecurityGroup(SecurityGroup sg, VirtualizationConnector vc)
            throws Exception {
        return tryLockChildAndParents(new LockObjectReference(sg.getId(), sg.getName(), ObjectType.SECURITY_GROUP),
                new LockObjectReference(vc.getId(), vc.getName(), ObjectType.VIRTUALIZATION_CONNECTOR));
    }

    public static void releaseLocks(UnlockObjectMetaTask unlockObjectTask) {
        if (unlockObjectTask != null) {
            for (UnlockObjectTask unlockTask : unlockObjectTask.getUnlockTasks()) {
                LockObjectReference objectRef = unlockTask.getObjectRef();
                log.info("Releasing Lock for Object: '" + objectRef.getType() + "' with Id: '" + objectRef.getId()
                        + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(unlockTask));
            }
        }
    }

    /**
     * Tries to Write lock the child and place a read lock on all the parents if specified.
     *
     */
    private static UnlockObjectMetaTask tryLockChildAndParents(LockObjectReference child,
            LockObjectReference... parents) throws VmidcBrokerInvalidRequestException, InterruptedException {
        return lockChildAndParents(true, child, parents);
    }

    private static UnlockObjectMetaTask lockChildAndParents(boolean tryToLock, LockObjectReference child,
            LockObjectReference... parents) throws VmidcBrokerInvalidRequestException, InterruptedException {
        UnlockObjectTask childObjectUnLock = null;
        List<UnlockObjectTask> parentObjectUnLocks = new ArrayList<>();
        try {
            childObjectUnLock = lockObject(child, LockType.WRITE_LOCK, tryToLock);
            if (parents != null) {
                for (LockObjectReference parentRef : parents) {
                    parentObjectUnLocks.add(lockObject(parentRef, LockType.READ_LOCK, tryToLock));
                }
            }
        } catch (Exception e) {
            log.info("Locking of objects failed. Releasing locks already obtained.");
            if (childObjectUnLock != null) {
                log.info("Releasing Write lock for Child: '" + child.getType() + "' with Id: '" + child.getId() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(childObjectUnLock));
            }
            for (UnlockObjectTask parent : parentObjectUnLocks) {
                log.info("Releasing Read lock for Parent: '" + parent.getObjectRef().getType() + "' with Id: '"
                        + parent.getObjectRef().getId() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(parent));
            }
            throw e;
        }
        // Add child unlock to the start of the list
        parentObjectUnLocks.add(0, childObjectUnLock);
        UnlockObjectMetaTask ult = new UnlockObjectMetaTask(parentObjectUnLocks);
        return ult;
    }

    /**
     * Tries to lock the specified objects with the locktype
     *
     */
    private static UnlockObjectMetaTask tryLockObjects(LockType lockType, LockObjectReference... objects)
            throws VmidcBrokerInvalidRequestException, InterruptedException {
        List<UnlockObjectTask> objectUnLocks = new ArrayList<>();
        try {
            for (LockObjectReference objectRef : objects) {
                objectUnLocks.add(lockObject(objectRef, lockType, true));
            }
        } catch (Exception e) {
            log.info("Locking of objects failed. Releasing already obtained locks");
            for (UnlockObjectTask objectUnlockTask : objectUnLocks) {
                LockObjectReference objectRef = objectUnlockTask.getObjectRef();
                log.info("Releasing lock for Object: '" + objectRef.getType() + "' with Id: '" + objectRef.getId()
                        + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(objectUnlockTask));
            }
            throw e;
        }
        UnlockObjectMetaTask ult = new UnlockObjectMetaTask(objectUnLocks);
        return ult;
    }

    private static UnlockObjectTask lockObject(LockObjectReference lockObjectReference, LockType lockType,
            boolean tryLock) throws VmidcBrokerInvalidRequestException, InterruptedException {
        UnlockObjectTask ult = new UnlockObjectTask(lockObjectReference, lockType);
        LockRequest lockRequest = new LockRequest(lockObjectReference, ult);
        if (tryLock) {
            if (!LockManager.getLockManager().tryAcquireLock(lockRequest)) {
                throw new VmidcBrokerInvalidRequestException("Fail to gain lock for " + lockObjectReference.getType()
                        + " '" + lockObjectReference.getName() + "'.");
            }
        } else {
            if (!LockManager.getLockManager().acquireLock(lockRequest, LockUtil.DEFAULT_MAX_LOCK_TIMEOUT)) {
                throw new VmidcBrokerInvalidRequestException("Fail to gain lock for " + lockObjectReference.getType()
                        + " '" + lockObjectReference.getName() + "'.");
            }
        }
        return ult;
    }

}
