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

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.api.UpdateSecurityGroupPropertiesServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UpdateSecurityGroupPropertiesService extends UpdateSecurityGroupService
        implements UpdateSecurityGroupPropertiesServiceApi {

    private static final Logger log = LoggerFactory.getLogger(UpdateSecurityGroupPropertiesService.class);

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, EntityManager em) throws Exception {

        SecurityGroupDto dto = request.getDto();
        validateAndLoad(em, request);

        SecurityGroup securityGroup = SecurityGroupEntityMgr.findById(em, dto.getId());

        UnlockObjectMetaTask unlockTask = null;

        try {

            unlockTask = LockUtil.tryLockSecurityGroup(securityGroup,
                    VirtualizationConnectorEntityMgr.findById(em, dto.getParentId()));

            SecurityGroupEntityMgr.toEntity(securityGroup, dto);

            log.info("Updating SecurityGroup properties: " + securityGroup.toString());
            OSCEntityManager.update(em, securityGroup, this.txBroadcastUtil);

            UnlockObjectMetaTask forLambda = unlockTask;
            chain(() -> {
                try {
                    Job job = this.sgConformJobFactory.startSecurityGroupConformanceJob(securityGroup, forLambda);

                    return new BaseJobResponse(securityGroup.getId(), job.getId());
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
