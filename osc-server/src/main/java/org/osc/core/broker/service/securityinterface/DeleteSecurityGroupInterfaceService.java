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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public class DeleteSecurityGroupInterfaceService extends ServiceDispatcher<BaseIdRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(DeleteSecurityGroupInterfaceService.class);
    private SecurityGroupInterface sgi = null;

    @Override
    public BaseJobResponse exec(BaseIdRequest request, Session session) throws Exception {
        validate(session, request);

        log.info("Deleting SecurityGroupInterface: " + this.sgi.getName());

        EntityManager.delete(session, this.sgi);

        commitChanges(true);

        Long jobId = ConformService.startDAConformJob(session, this.sgi.getVirtualSystem().getDistributedAppliance());

        BaseJobResponse response = new BaseJobResponse(this.sgi.getId());
        response.setJobId(jobId);
        return response;
    }

    private void validate(Session session, BaseIdRequest request) throws Exception {
        BaseIdRequest.checkForNullIdAndParentNullId(request);

        VirtualSystem vs = VirtualSystemEntityMgr.findById(session, request.getParentId());

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + request.getParentId()
            + "  is not found.");
        }

        this.sgi = (SecurityGroupInterface) session.get(SecurityGroupInterface.class, request.getId());
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
