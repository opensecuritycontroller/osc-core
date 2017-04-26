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
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.api.AddSecurityGroupServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;

public class AddSecurityGroupService extends BaseSecurityGroupService<AddOrUpdateSecurityGroupRequest, BaseJobResponse>
        implements AddSecurityGroupServiceApi {

    private static final Logger LOG = Logger.getLogger(AddSecurityGroupService.class);

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, EntityManager em) throws Exception,
    VmidcBrokerValidationException {

        SecurityGroupDto dto = request.getDto();
        validateAndLoad(em, dto);
        if (dto.isProtectAll()
                && SecurityGroupEntityMgr.isSecurityGroupExistWithProtectAll(em, dto.getTenantId(),
                        this.vc.getId())) {
            throw new VmidcBrokerValidationException(
                    "Security Group exists with the same Tenant and Selection for this Virtualization Connector.");
        }

        if (SecurityGroupEntityMgr.isSecurityGroupExistWithSameNameAndTenant(em, dto.getName(), dto.getTenantId())) {
            throw new VmidcBrokerValidationException("Security Group Name: " + dto.getName() + " already exists on the same Tenant.");
        }

        UnlockObjectMetaTask unlockTask = null;

        try {

            unlockTask = LockUtil.tryLockVC(this.vc, LockType.READ_LOCK);

            SecurityGroup securityGroup = new SecurityGroup(this.vc, dto.getTenantId(), dto.getTenantName());
            SecurityGroupEntityMgr.toEntity(securityGroup, dto);

            LOG.info("Creating security group: " + securityGroup.toString());
            OSCEntityManager.create(em, securityGroup);

            if (!securityGroup.isProtectAll()) {
                for (SecurityGroupMemberItemDto securityGroupMemberDto : request.getMembers()) {
                    validate(securityGroupMemberDto);
                    addSecurityGroupMember(em, securityGroup, securityGroupMemberDto);
                }
            }

            OSCEntityManager.update(em, securityGroup);

            UnlockObjectMetaTask forLambda = unlockTask;
            chain(() -> {
                try {
                    Job job = ConformService.startSecurityGroupConformanceJob(securityGroup, forLambda);

                    return new BaseJobResponse(securityGroup.getId(), job.getId());
                } catch (Exception e) {
                    LockUtil.releaseLocks(forLambda);
                    throw e;
                }
            });

            // Lock the SG with a write lock and allow it to be unlocked at the end of the job
            unlockTask.addUnlockTask(LockUtil.tryLockSecurityGroupOnly(securityGroup));
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        return null;
    }
}
