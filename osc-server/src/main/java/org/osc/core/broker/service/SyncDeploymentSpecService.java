package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.util.ValidateUtil;

public class SyncDeploymentSpecService extends
        BaseDeploymentSpecService<BaseRequest<DeploymentSpecDto>, BaseJobResponse> {

    private DeploymentSpec ds;

    @Override
    public BaseJobResponse exec(BaseRequest<DeploymentSpecDto> request, Session session) throws Exception {

        BaseJobResponse response = new BaseJobResponse();

        UnlockObjectMetaTask unlockTask = null;
        validate(session, request.getDto());

        try {

            DistributedAppliance da = this.ds.getVirtualSystem().getDistributedAppliance();
            unlockTask = LockUtil.tryReadLockDA(da, da.getApplianceManagerConnector());
            unlockTask.addUnlockTask(LockUtil.tryLockVCObject(this.ds.getVirtualSystem().getVirtualizationConnector(),
                    LockType.READ_LOCK));

            // Lock the DS with a write lock and allow it to be unlocked at the end of the job
            unlockTask.addUnlockTask(LockUtil.tryLockDSOnly(this.ds));
            Job job = ConformService.startDsConformanceJob(session, this.ds, unlockTask);

            response.setJobId(job.getId());

            return response;
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }
    }

    @Override
    protected void validate(Session session, DeploymentSpecDto dto) throws Exception {
        this.ds = (DeploymentSpec) session.get(DeploymentSpec.class, dto.getId());
        if (this.ds == null) {
            throw new VmidcBrokerValidationException("Deployment Specification with Id: " + dto.getId()
                    + "  is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(this.ds, this.ds.getName());

    };

}
