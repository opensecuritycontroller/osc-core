package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.ForceDeleteDSTask;

public class DeleteDeploymentSpecService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse> {

    private DeploymentSpec ds;

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, Session session) throws Exception {

        BaseJobResponse response = new BaseJobResponse();
        validateAndLoad(session, request);
        Job job = null;
        UnlockObjectMetaTask dsUnlock = null;

        try {
            DistributedAppliance da = this.ds.getVirtualSystem().getDistributedAppliance();
            dsUnlock = LockUtil.tryLockDS(this.ds, da, da.getApplianceManagerConnector(), this.ds.getVirtualSystem()
                    .getVirtualizationConnector());

            if (request.isForceDelete()) {
                TaskGraph tg = new TaskGraph();
                tg.addTask(new ForceDeleteDSTask(this.ds));
                tg.appendTask(dsUnlock, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                job = JobEngine.getEngine().submit("Force Delete Deployment Spec '" + this.ds.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(this.ds));

            } else {
                EntityManager.markDeleted(session, this.ds);
                commitChanges(true);
                job = ConformService.startDsConformanceJob(session, this.ds, dsUnlock);
            }
            response.setJobId(job.getId());
        } catch (Exception e) {
            LockUtil.releaseLocks(dsUnlock);
            throw e;
        }

        return response;

    }

    private void validateAndLoad(Session session, BaseDeleteRequest request) throws Exception {
        BaseDeleteRequest.checkForNullIdAndParentNullId(request);

        VirtualSystem vs = VirtualSystemEntityMgr.findById(session, request.getParentId());

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + request.getParentId()
                    + "  is not found.");
        }

        this.ds = (DeploymentSpec) session.get(DeploymentSpec.class, request.getId());

        // entry must pre-exist in db
        if (this.ds == null) { // note: we cannot use name here in error msg since
            // del req does not have name, only ID

            throw new VmidcBrokerValidationException("Deployment Specification entry with ID " + request.getId()
                    + " is not found.");
        }

        // TODO: Future if DS/DDS is in use send Alert/Warning to the user...

        if (!this.ds.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException(
                    "Deployment Spec '"
                            + this.ds.getName()
                            + "' is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }
    }
}
