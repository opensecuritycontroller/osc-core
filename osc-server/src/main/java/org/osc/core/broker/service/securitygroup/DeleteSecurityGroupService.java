package org.osc.core.broker.service.securitygroup;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.ForceDeleteSecurityGroupTask;

public class DeleteSecurityGroupService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(DeleteSecurityGroupService.class);
    private SecurityGroup securityGroup = null;

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, Session session) throws Exception {
        validate(session, request);

        UnlockObjectMetaTask unlockTask = null;
        Job deleteSecurityGroupJob = null;
        BaseJobResponse response = new BaseJobResponse();
        log.info("Deleting SecurityGroup: " + this.securityGroup.getName());
        try {
            unlockTask = LockUtil.tryLockSecurityGroup(this.securityGroup,
                    this.securityGroup.getVirtualizationConnector());
            if (request.isForceDelete()) {
                TaskGraph tg = new TaskGraph();
                tg.addTask(new ForceDeleteSecurityGroupTask(this.securityGroup));
                tg.appendTask(unlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                deleteSecurityGroupJob = JobEngine.getEngine().submit(
                        "Force Delete Security Group '" + this.securityGroup.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(this.securityGroup));

            } else {

                // Mark this security Group for Deletion and Trigger  Sync Job
                EntityManager.markDeleted(session, this.securityGroup);
                commitChanges(true);
                deleteSecurityGroupJob = ConformService.startSecurityGroupConformanceJob(session, this.securityGroup,
                        unlockTask, false);
            }
            response.setJobId(deleteSecurityGroupJob.getId());
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        return response;
    }

    private void validate(Session session, BaseDeleteRequest request) throws Exception {
        BaseDeleteRequest.checkForNullIdAndParentNullId(request);

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(session, request.getParentId());

        if (vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + request.getParentId()
                    + "  is not found.");
        }

        this.securityGroup = (SecurityGroup) session.get(SecurityGroup.class, request.getId());
        if (this.securityGroup == null) {
            throw new VmidcBrokerValidationException("Security Group with Id: " + request.getId() + "  is not found.");
        }

        if (!this.securityGroup.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException(
                    "Security Group with ID "
                            + request.getId()
                            + " is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }
    }

}
