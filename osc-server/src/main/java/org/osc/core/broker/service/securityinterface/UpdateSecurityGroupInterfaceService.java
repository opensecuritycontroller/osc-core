/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service.securityinterface;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public class UpdateSecurityGroupInterfaceService extends
BaseSecurityGroupInterfaceService<BaseRequest<SecurityGroupInterfaceDto>, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(UpdateSecurityGroupInterfaceService.class);

    private SecurityGroupInterface sgi;

    @Override
    public BaseJobResponse exec(BaseRequest<SecurityGroupInterfaceDto> request, Session session) throws Exception {

        SecurityGroupInterfaceDto dto = request.getDto();
        validateAndLoad(session, dto);

        SecurityGroupInterfaceEntityMgr.toEntity(this.sgi, dto, this.policy, SecurityGroupInterface.ISC_TAG_PREFIX);

        log.info("Updating SecurityGroupInterface: " + this.sgi.toString());
        EntityManager.update(session, this.sgi);
        commitChanges(true);

        Long jobId = ConformService.startDAConformJob(session, this.sgi.getVirtualSystem().getDistributedAppliance());
        return new BaseJobResponse(this.sgi.getId(), jobId);
    }

    @Override
    protected void validateAndLoad(Session session, SecurityGroupInterfaceDto dto) throws Exception {
        super.validateAndLoad(session, dto);

        this.sgi = SecurityGroupInterfaceEntityMgr.findById(session, dto.getId());

        if (this.sgi == null) {
            throw new VmidcBrokerValidationException(
                    "Traffic Policy Mapping with Id: " + dto.getId() + "  is not found.");
        }

        if (!this.sgi.isUserConfigurable()) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Only User configured Traffic Policy Mappings can be updated.");
        }

        if (!this.sgi.getVirtualSystem().getId().equals(dto.getParentId())) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Cannot change the Virtual System a Traffic Policy Mapping is associated with.");
        }

        SecurityGroupInterface existingSGI = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsAndTag(
                session, this.vs, SecurityGroupInterface.ISC_TAG_PREFIX + dto.getTagValue().toString());

        if (existingSGI != null && !existingSGI.equals(this.sgi)) {
            throw new VmidcBrokerValidationException("A Traffic Policy Mapping: " + existingSGI.getName()
            + " exists for the specified virtual system and tag combination.");
        }

    }

}
