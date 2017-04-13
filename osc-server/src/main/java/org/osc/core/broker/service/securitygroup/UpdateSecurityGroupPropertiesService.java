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
package org.osc.core.broker.service.securitygroup;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;

public class UpdateSecurityGroupPropertiesService extends UpdateSecurityGroupService {

    private static final Logger log = Logger.getLogger(UpdateSecurityGroupPropertiesService.class);

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, EntityManager em) throws Exception {

        SecurityGroupDto dto = request.getDto();
        validateAndLoad(em, request);
        UnlockObjectMetaTask unlockTask = null;

        try {

            unlockTask = LockUtil.tryLockSecurityGroup(this.securityGroup, this.vc);

            SecurityGroupEntityMgr.toEntity(this.securityGroup, dto);

            log.info("Updating SecurityGroup properties: " + this.securityGroup.toString());
            OSCEntityManager.update(em, this.securityGroup);

            UnlockObjectMetaTask forLambda = unlockTask;
            chain(() -> {
                try {
                    Job job = ConformService.startSecurityGroupConformanceJob(this.securityGroup, forLambda);

                    return new BaseJobResponse(this.securityGroup.getId(), job.getId());
                } catch (Exception e) {
                    LockUtil.releaseLocks(forLambda);
                    throw e;
                }
            });
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        return null;
    }
}
