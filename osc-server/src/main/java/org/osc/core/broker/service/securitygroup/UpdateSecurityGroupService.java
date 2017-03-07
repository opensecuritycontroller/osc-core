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
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.util.ValidateUtil;

public class UpdateSecurityGroupService extends
        BaseSecurityGroupService<AddOrUpdateSecurityGroupRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(UpdateSecurityGroupService.class);

    protected SecurityGroup securityGroup;

    @Override
    public BaseJobResponse exec(AddOrUpdateSecurityGroupRequest request, Session session) throws Exception {

        SecurityGroupDto dto = request.getDto();
        validateAndLoad(session, request);
        UnlockObjectMetaTask unlockTask = null;

        try {

            unlockTask = LockUtil.tryLockSecurityGroup(this.securityGroup, this.vc);

            SecurityGroupEntityMgr.toEntity(this.securityGroup, dto);

            Set<SecurityGroupMemberItemDto> selectedMembers = request.getMembers();

            Set<String> selectedMemberOsId = new HashSet<>();
            if (selectedMembers != null) {
                for (SecurityGroupMemberItemDto securityGroupMemberDto : selectedMembers) {
                    validate(securityGroupMemberDto);
                    String openstackId = securityGroupMemberDto.getOpenstackId();
                    selectedMemberOsId.add(openstackId);
                    addSecurityGroupMember(session, this.securityGroup, securityGroupMemberDto);
                }
            }

            Set<SecurityGroupMember> securityGroupMembers = this.securityGroup.getSecurityGroupMembers();
            Iterator<SecurityGroupMember> sgMemberEntityIterator = securityGroupMembers.iterator();
            while (sgMemberEntityIterator.hasNext()) {
                SecurityGroupMember sgMemberEntity = sgMemberEntityIterator.next();
                String entityOpenstackId = getMemberOpenstackId(sgMemberEntity);
                boolean isMemberSelected = selectedMemberOsId.contains(entityOpenstackId);
                if (!isMemberSelected) {
                    log.info("Removing Member: " + sgMemberEntity.getMemberName());
                    EntityManager.markDeleted(session, sgMemberEntity);
                }
            }

            log.info("Updating SecurityGroup: " + this.securityGroup.toString());
            EntityManager.update(session, this.securityGroup);

            commitChanges(true);
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        Job job = ConformService.startSecurityGroupConformanceJob(this.securityGroup, unlockTask);

        return new BaseJobResponse(this.securityGroup.getId(), job.getId());

    }

    protected void validateAndLoad(Session session, AddOrUpdateSecurityGroupRequest request) throws Exception {
        SecurityGroupDto dto = request.getDto();
        if(request.isApi() && dto.getName() == null) {
            // If update request is coming from API and name is not specified(its a required field),
            // assumes it a member update request. Load existing values from the DB and pass to service
            SecurityGroupDto.checkForNullIdFields(dto);
            this.securityGroup = SecurityGroupEntityMgr.findById(session, dto.getId());

            if (this.securityGroup == null) {
                throw new VmidcBrokerValidationException("Security Group with Id: " + dto.getId() + "  is not found.");
            }
            SecurityGroupEntityMgr.fromEntity(this.securityGroup, dto);

        }
        super.validateAndLoad(session, dto);

        if (this.securityGroup == null) {
            this.securityGroup = SecurityGroupEntityMgr.findById(session, dto.getId());

            if (this.securityGroup == null) {
                throw new VmidcBrokerValidationException("Security Group with Id: " + dto.getId() + "  is not found.");
            }
        }

        ValidateUtil.checkMarkedForDeletion(this.securityGroup, this.securityGroup.getName());

        if (!this.securityGroup.getVirtualizationConnector().getId().equals(dto.getParentId())) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Cannot change the Virtualization Connector a Security Group is associated with.");
        }

    }

    private String getMemberOpenstackId(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getOpenstackId();
        case NETWORK:
            return sgm.getNetwork().getOpenstackId();
        case SUBNET:
            return sgm.getSubnet().getOpenstackId();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + sgm.getType() + "'");
        }
    }

}
