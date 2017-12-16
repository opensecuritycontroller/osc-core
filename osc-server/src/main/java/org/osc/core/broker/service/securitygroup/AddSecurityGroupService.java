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

import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.api.AddSecurityGroupServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation is advertised so that it can be used in
 * SecurityGroupUpdateOrDeleteMetaTask
 */
// TODO this service causes circularity. DS references are optional+dynamic as work around.
@Component(service={AddSecurityGroupService.class, AddSecurityGroupServiceApi.class})
public class AddSecurityGroupService extends BaseSecurityGroupService<AddOrUpdateSecurityGroupRequest, BaseJobResponse>
implements AddSecurityGroupServiceApi {

    private static final Logger LOG = LoggerFactory.getLogger(AddSecurityGroupService.class);

    @Reference
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, EntityManager em) throws Exception,
    VmidcBrokerValidationException {

        SecurityGroupDto dto = request.getDto();
        List<String> regions = validateAndLoad(em, dto);

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, dto.getParentId());

        if (dto.isProtectAll()
                && SecurityGroupEntityMgr.isSecurityGroupExistWithProtectAll(em, dto.getProjectId(),
                        dto.getParentId())) {
            throw new VmidcBrokerValidationException(
                    "Security Group exists with the same Project and Selection for this Virtualization Connector.");
        }

        if (SecurityGroupEntityMgr.isSecurityGroupExistWithSameNameAndProject(em, dto.getName(), dto.getProjectId())) {
            throw new VmidcBrokerValidationException("Security Group Name: " + dto.getName() + " already exists on the same Project.");
        }

        UnlockObjectMetaTask unlockTask = null;

        try {
            unlockTask = LockUtil.tryLockVC(vc, LockType.READ_LOCK);

            SecurityGroup securityGroup = new SecurityGroup(vc, dto.getProjectId(), dto.getProjectName());
            SecurityGroupEntityMgr.toEntity(securityGroup, dto);

            LOG.info("Creating security group: " + securityGroup.toString());
            OSCEntityManager.create(em, securityGroup, this.txBroadcastUtil);

            if (!securityGroup.isProtectAll()) {
                for (SecurityGroupMemberItemDto securityGroupMemberDto : request.getMembers()) {
                    if (vc.getVirtualizationType().isOpenstack()) {
                        validate(securityGroupMemberDto, regions);
                    }

                    addSecurityGroupMember(em, securityGroup, securityGroupMemberDto);
                }
            }

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

            // Lock the SG with a write lock and allow it to be unlocked at the end of the job
            unlockTask.addUnlockTask(LockUtil.tryLockSecurityGroupOnly(securityGroup));
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        return null;
    }
}
