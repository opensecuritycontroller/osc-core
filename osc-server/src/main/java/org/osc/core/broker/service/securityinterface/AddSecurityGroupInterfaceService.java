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
package org.osc.core.broker.service.securityinterface;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public class AddSecurityGroupInterfaceService extends
BaseSecurityGroupInterfaceService<BaseRequest<SecurityGroupInterfaceDto>, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(AddSecurityGroupInterfaceService.class);
    private ConformService conformService;

    public AddSecurityGroupInterfaceService(ConformService conformService) {
        this.conformService = conformService;
    }

    @Override
    public BaseJobResponse exec(BaseRequest<SecurityGroupInterfaceDto> request, EntityManager em) throws Exception {
        SecurityGroupInterfaceDto dto = request.getDto();
        validateAndLoad(em, dto);

        SecurityGroupInterface sgi = new SecurityGroupInterface(
                this.vs,
                null,
                null,
                FailurePolicyType.valueOf(dto.getFailurePolicyType().name()),
                0L);

        SecurityGroupInterfaceEntityMgr.toEntity(sgi, dto, this.policy, SecurityGroupInterface.ISC_TAG_PREFIX);

        log.info("Creating SecurityGroupInterface: " + sgi.toString());
        OSCEntityManager.create(em, sgi);

        chain(() -> {
            Long jobId = this.conformService.startDAConformJob(em, sgi.getVirtualSystem().getDistributedAppliance());

            BaseJobResponse response = new BaseJobResponse(sgi.getId());
            response.setJobId(jobId);
            return response;
        });

        return null;
    }

    @Override
    protected void validateAndLoad(EntityManager em, SecurityGroupInterfaceDto dto) throws Exception {
        super.validateAndLoad(em, dto);

        if (!dto.isUserConfigurable()) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Only User configured Traffic Policy Mappings can be Created.");
        }

        SecurityGroupInterface existingSGI = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsAndTag(
                em, this.vs, SecurityGroupInterface.ISC_TAG_PREFIX + dto.getTagValue().toString());

        if (existingSGI != null) {
            throw new VmidcBrokerValidationException("A Traffic Policy Mapping: " + existingSGI.getName()
            + " exists for the specified virtual system and tag combination.");
        }

    }

}
