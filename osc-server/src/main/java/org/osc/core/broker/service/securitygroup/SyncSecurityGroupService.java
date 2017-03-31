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
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.util.api.ApiUtil;
import org.osgi.service.component.annotations.Reference;

public class SyncSecurityGroupService extends ServiceDispatcher<BaseIdRequest, BaseJobResponse> {

    private SecurityGroup securityGroup;
    private VirtualizationConnector vc;

    @Reference
    private ApiUtil apiUtil;

    @Override
    public BaseJobResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        validateAndLoad(request, em);

        Job job = ConformService.startSecurityGroupConformanceJob(em, this.securityGroup, null, false);

        return new BaseJobResponse(job.getId());
    }

    private void validateAndLoad(BaseIdRequest request, EntityManager em) throws Exception,
            VmidcBrokerValidationException {
        BaseIdRequest.checkForNullIdAndParentNullId(request);

        this.securityGroup = SecurityGroupEntityMgr.findById(em, request.getId());

        if (this.securityGroup == null) {
            throw new VmidcBrokerValidationException("Security Group with Id: " + request.getId() + "  is not found.");
        }

        if (this.securityGroup.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException(
                    "Syncing Security Group which is marked for deletion is not allowed.");
        }

        this.vc = VirtualizationConnectorEntityMgr.findById(em, request.getParentId());

        if (this.vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + request.getParentId()
                    + "  is not found.");
        }

        // For service calls makes sure the VC's match
        if (this.securityGroup.getVirtualizationConnector() != this.vc) {
            throw apiUtil.createParentChildMismatchException(request.getParentId(), "Security Group");
        }

        if(this.vc.getVirtualizationType() != VirtualizationType.OPENSTACK) {
            throw new VmidcBrokerValidationException("Syncing of security groups is only applicable for Openstack");
        }
    }

}
