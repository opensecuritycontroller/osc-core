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

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.DistributedApplianceConformJobFactory;
import org.osc.core.broker.service.api.AddSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AddSecurityGroupInterfaceService
        extends BaseSecurityGroupInterfaceService<BaseRequest<SecurityGroupInterfaceDto>, BaseJobResponse>
        implements AddSecurityGroupInterfaceServiceApi {

    private static final Logger log = LoggerFactory.getLogger(AddSecurityGroupInterfaceService.class);

    @Reference
    private DistributedApplianceConformJobFactory daConformJobFactory;

    @Override
    public BaseJobResponse exec(BaseRequest<SecurityGroupInterfaceDto> request, EntityManager em) throws Exception {
        SecurityGroupInterfaceDto dto = request.getDto();
        VirtualSystem vs = validateAndLoad(em, dto);

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, null,
                FailurePolicyType.valueOf(dto.getFailurePolicyType().name()), 0L);

		SecurityGroupInterfaceEntityMgr.toEntity(sgi, dto,
				PolicyEntityMgr.findPoliciesById(em, dto.getPolicyIds(),
						vs.getDistributedAppliance().getApplianceManagerConnector()),
				SecurityGroupInterface.ISC_TAG_PREFIX);

        log.info("Creating SecurityGroupInterface: " + sgi.toString());
        OSCEntityManager.create(em, sgi, this.txBroadcastUtil);

        chain(() -> {
            Long jobId = this.daConformJobFactory.startDAConformJob(em, sgi.getVirtualSystem().getDistributedAppliance());

            BaseJobResponse response = new BaseJobResponse(sgi.getId());
            response.setJobId(jobId);
            return response;
        });

        return null;
    }

    @Override
    protected VirtualSystem validateAndLoad(EntityManager em, SecurityGroupInterfaceDto dto) throws Exception {
        VirtualSystem vs = super.validateAndLoad(em, dto);

		if (!dto.isUserConfigurable()) {
			throw new VmidcBrokerValidationException(
					"Invalid request. Only User configured Traffic Policy Mappings can be Created.");
		} else {
			if (dto.getPolicies().size() > 1) {
				throw new VmidcBrokerValidationException(
						"Invalid request. User configured Traffic Policy Mappings cannot have more than one policy.");
			}
		}

		Long policyId = dto.getPolicies().iterator().next().getId();
        SecurityGroupInterface existingSGI = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsTagAndPolicy(
                em, vs, SecurityGroupInterface.ISC_TAG_PREFIX + dto.getTagValue().toString(), policyId);

        if (existingSGI != null) {
            throw new VmidcBrokerValidationException("A Traffic Policy Mapping: " + existingSGI.getName()
            + " exists for the specified virtual system and tag combination.");
        }
        return vs;
    }
}
