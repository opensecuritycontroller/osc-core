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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.DistributedApplianceConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.DeleteSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DeleteSecurityGroupInterfaceService extends ServiceDispatcher<BaseIdRequest, BaseJobResponse>
        implements DeleteSecurityGroupInterfaceServiceApi {

    private static final Logger log = LoggerFactory.getLogger(DeleteSecurityGroupInterfaceService.class);
    private SecurityGroupInterface sgi = null;

    @Reference
    private DistributedApplianceConformJobFactory daConformJobFactory;

    @Override
    public BaseJobResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        validate(em, request);

        log.info("Deleting SecurityGroupInterface: " + this.sgi.getName());

        OSCEntityManager.delete(em, this.sgi, this.txBroadcastUtil);

        chain(() -> {
            Long jobId = this.daConformJobFactory.startDAConformJob(em, this.sgi.getVirtualSystem().getDistributedAppliance());

            BaseJobResponse response = new BaseJobResponse(this.sgi.getId());
            response.setJobId(jobId);
            return response;
        });
        return null;
    }

    private void validate(EntityManager em, BaseIdRequest request) throws Exception {
        BaseIdRequestValidator.checkForNullIdAndParentNullId(request);

        VirtualSystem vs = VirtualSystemEntityMgr.findById(em, request.getParentId());

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + request.getParentId()
            + "  is not found.");
        }

        this.sgi = em.find(SecurityGroupInterface.class, request.getId());
        if (this.sgi == null) {
            throw new VmidcBrokerValidationException("Traffic Policy Mapping with Id: " + request.getId()
            + "  is not found.");
        }

        if (!this.sgi.isUserConfigurable()) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Only User configured Traffic Policy Mappings can be deleted.");
        }
    }

}
