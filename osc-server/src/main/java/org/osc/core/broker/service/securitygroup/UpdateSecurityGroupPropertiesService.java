package org.osc.core.broker.service.securitygroup;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;

public class UpdateSecurityGroupPropertiesService extends UpdateSecurityGroupService {

    private static final Logger log = Logger.getLogger(UpdateSecurityGroupPropertiesService.class);

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, Session session) throws Exception {

        SecurityGroupDto dto = request.getDto();
        validateAndLoad(session, request);
        UnlockObjectMetaTask unlockTask = null;

        try {

            unlockTask = LockUtil.tryLockSecurityGroup(this.securityGroup, this.vc);

            SecurityGroupEntityMgr.toEntity(this.securityGroup, dto);

            log.info("Updating SecurityGroup properties: " + this.securityGroup.toString());
            EntityManager.update(session, this.securityGroup);

            commitChanges(true);
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        Job job = ConformService.startSecurityGroupConformanceJob(this.securityGroup, unlockTask);

        return new BaseJobResponse(this.securityGroup.getId(), job.getId());

    }
}
