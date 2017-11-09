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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.api.UpdateSecurityGroupServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.validator.SecurityGroupDtoValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class UpdateSecurityGroupService
extends BaseSecurityGroupService<AddOrUpdateSecurityGroupRequest, BaseJobResponse>
implements UpdateSecurityGroupServiceApi {

    private static final Logger log = LoggerFactory.getLogger(UpdateSecurityGroupService.class);

    // this @Ref is used by sub-type UpdateSecurityGroupPropertiesService
    @Reference
    protected SecurityGroupConformJobFactory sgConformJobFactory;

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, EntityManager em) throws Exception {

        SecurityGroupDto dto = request.getDto();
        List<String> regions = validateAndLoad(em, request);

        SecurityGroup securityGroup = SecurityGroupEntityMgr.findById(em, dto.getId());
        UnlockObjectMetaTask unlockTask = null;
        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, dto.getParentId());

        try {
            unlockTask = LockUtil.tryLockSecurityGroup(securityGroup, vc);
            SecurityGroupEntityMgr.toEntity(securityGroup, dto);
            Set<SecurityGroupMemberItemDto> selectedMembers = request.getMembers();

            Set<String> selectedMemberUniqueId = new HashSet<>();
            if (selectedMembers != null) {
                for (SecurityGroupMemberItemDto securityGroupMemberDto : selectedMembers) {

                    if (vc.getVirtualizationType().isOpenstack()) {
                        validate(securityGroupMemberDto, regions);
                    }

                    String externalId =
                            SecurityGroupMemberType.fromText(securityGroupMemberDto.getType()) == SecurityGroupMemberType.LABEL ?
                                    securityGroupMemberDto.getLabel() : securityGroupMemberDto.getOpenstackId();
                                    selectedMemberUniqueId.add(externalId);
                                    addSecurityGroupMember(em, securityGroup, securityGroupMemberDto);
                }
            }

            Set<SecurityGroupMember> securityGroupMembers = securityGroup.getSecurityGroupMembers();
            Iterator<SecurityGroupMember> sgMemberEntityIterator = securityGroupMembers.iterator();
            while (sgMemberEntityIterator.hasNext()) {
                SecurityGroupMember sgMemberEntity = sgMemberEntityIterator.next();
                String entityUniqueId = getMemberUniqueId(sgMemberEntity);
                boolean isMemberSelected = selectedMemberUniqueId.contains(entityUniqueId);
                if (!isMemberSelected) {
                    log.info("Removing Member: " + sgMemberEntity.getMemberName());
                    OSCEntityManager.markDeleted(em, sgMemberEntity, this.txBroadcastUtil);
                }
            }

            log.info("Updating SecurityGroup: " + securityGroup.toString());
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

    protected List<String> validateAndLoad(EntityManager em, AddOrUpdateSecurityGroupRequest request) throws Exception {
        SecurityGroupDto dto = request.getDto();
        SecurityGroup securityGroup = null;
        if(request.isApi() && dto.getName() == null) {
            // If update request is coming from API and name is not specified(its a required field),
            // assumes it a member update request. Load existing values from the DB and pass to service
            SecurityGroupDtoValidator.checkForNullIdFields(dto);
            securityGroup = SecurityGroupEntityMgr.findById(em, dto.getId());

            if (securityGroup == null) {
                throw new VmidcBrokerValidationException("Security Group with Id: " + dto.getId() + "  is not found.");
            }
            SecurityGroupEntityMgr.fromEntity(securityGroup, dto);

        }
        List<String> regions = super.validateAndLoad(em, dto);

        if (securityGroup == null) {
            securityGroup = SecurityGroupEntityMgr.findById(em, dto.getId());

            if (securityGroup == null) {
                throw new VmidcBrokerValidationException("Security Group with Id: " + dto.getId() + "  is not found.");
            }
        }

        ValidateUtil.checkMarkedForDeletion(securityGroup, securityGroup.getName());

        if (!securityGroup.getVirtualizationConnector().getId().equals(dto.getParentId())) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Cannot change the Virtualization Connector a Security Group is associated with.");
        }
        return regions;
    }

    private String getMemberUniqueId(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getOpenstackId();
        case NETWORK:
            return sgm.getNetwork().getOpenstackId();
        case SUBNET:
            return sgm.getSubnet().getOpenstackId();
        case LABEL:
            return sgm.getLabel().getValue();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + sgm.getType() + "'");
        }
    }

}
