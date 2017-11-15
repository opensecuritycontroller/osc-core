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
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.SyncSecurityGroupServiceApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.ExceptionConstants;
import org.osc.core.broker.service.exceptions.OscBadRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class SyncSecurityGroupService extends ServiceDispatcher<BaseIdRequest, BaseJobResponse>
implements SyncSecurityGroupServiceApi {

    @Reference
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Override
    public BaseJobResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        SecurityGroup securityGroup = validateAndLoad(request, em);

        Job job = this.sgConformJobFactory.startSecurityGroupConformanceJob(em, securityGroup, null, false);

        return new BaseJobResponse(job.getId());
    }

    private SecurityGroup validateAndLoad(BaseIdRequest request, EntityManager em) throws Exception,
    VmidcBrokerValidationException {
        BaseIdRequestValidator.checkForNullIdAndParentNullId(request);

        SecurityGroup securityGroup = SecurityGroupEntityMgr.findById(em, request.getId());

        if (securityGroup == null) {
            throw new VmidcBrokerValidationException("Security Group with Id: " + request.getId() + "  is not found.");
        }

        if (securityGroup.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException(
                    "Syncing Security Group which is marked for deletion is not allowed.");
        }

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, request.getParentId());

        if (vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + request.getParentId()
            + "  is not found.");
        }

        // For service calls makes sure the VC's match
        if (!vc.equals(securityGroup.getVirtualizationConnector())) {
            throw createParentChildMismatchException(request.getParentId(), "Security Group");
        }

        return securityGroup;
    }

    private <T extends BaseDto> OscBadRequestException createParentChildMismatchException(Long parentId,
            String objName) {
        return new OscBadRequestException(
                String.format("The Parent ID %d specified in the '%s' data does not match the id specified in the URL",
                        parentId, objName), ExceptionConstants.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE);
    }

}
