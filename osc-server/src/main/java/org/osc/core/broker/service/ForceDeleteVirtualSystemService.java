package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.ForceDeleteVirtualSystemTask;

public class ForceDeleteVirtualSystemService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse> {

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, Session session) throws Exception {

        VirtualSystem vs = validate(session, request);

        UnlockObjectMetaTask ult = null;

        try {
            DistributedAppliance da = vs.getDistributedAppliance();
            ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());
            TaskGraph tg = new TaskGraph();
            tg.addTask(new ForceDeleteVirtualSystemTask(vs));
            tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            Job job = JobEngine.getEngine().submit("Force Delete Virtual System '" + vs.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(da));

            BaseJobResponse response = new BaseJobResponse();
            response.setJobId(job.getId());
            return response;

        } catch (Exception ex) {
            LockUtil.releaseLocks(ult);
            throw ex;
        }
    }

    private VirtualSystem validate(Session session, BaseDeleteRequest request) throws Exception {

        if (!request.isForceDelete()) {
            throw new VmidcBrokerValidationException("Virtual System can only be force deleted with this request");
        }

        VirtualSystem vs = (VirtualSystem) session.get(VirtualSystem.class, request.getId());

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with ID " + request.getId() + " is not found.");
        }

        if (!vs.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException("Virtual System '" + vs.getName() + "' (" + request.getId()
                    + ") is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }

        if (vs.getDistributedAppliance().getVirtualSystems().size() <= 1) {
            throw new VmidcBrokerValidationException("Virtual System '" + vs.getName() + "' (" + request.getId()
                    + ") is the only one in its DA. Deleting it will leave DA in invalid state. Please delete owning DA instead.");
        }

        return vs;
    }

}
