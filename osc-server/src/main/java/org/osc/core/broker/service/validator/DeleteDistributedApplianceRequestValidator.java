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
package org.osc.core.broker.service.validator;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.BaseDeleteRequest;

public class DeleteDistributedApplianceRequestValidator implements RequestValidator<BaseDeleteRequest,DistributedAppliance> {

    private EntityManager em;

    public DeleteDistributedApplianceRequestValidator(EntityManager em) {
        this.em = em;
    }

    @Override
    public void validate(BaseDeleteRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedAppliance validateAndLoad(BaseDeleteRequest request) throws Exception {
        DistributedAppliance da = this.em.find(DistributedAppliance.class, request.getId());

        // entry must pre-exist in db
        if (da == null) { // note: we cannot use name here in error msg since del req does not have name, only ID
            throw new VmidcBrokerValidationException("Distributed Appliance entry with ID '" + request.getId() + "' is not found.");
        }

        if (DistributedApplianceEntityMgr.isProtectingWorkload(da)) {
            throw new VmidcBrokerValidationException(
                    String.format("The distributed appliance with name '%s' and id '%s' is currently protecting a workload",
                            da.getName(),
                            da.getId()));
        }

        validateDAReferenceToSfcAndSecurityGroup(em, da);

        if (!da.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException(
                    "Distributed Appilance with ID "
                            + request.getId()
                            + " is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }
        return da;
    }

    private void validateDAReferenceToSfcAndSecurityGroup(EntityManager em, DistributedAppliance da) throws Exception {
        for (VirtualSystem vs : da.getVirtualSystems()) {
            try {
                validateVSReferenceToSfcAndSecurityGroup(em, vs);
            } catch (VmidcBrokerValidationException ex) {
                throw new VmidcBrokerValidationException(String.format("Cannot delete the distributed appliance '%s' reason:",da.getName())+" "+ex.getMessage());
            }
        }
    }

    private void validateVSReferenceToSfcAndSecurityGroup(EntityManager em, VirtualSystem vs) throws Exception {
        if (vs.getServiceFunctionChains() == null) {
            return;
        }
        for (ServiceFunctionChain sfc : vs.getServiceFunctionChains()) {
            validateSfcReferenceToSecurityGroup(em, sfc);
        }
    }

    private void validateSfcReferenceToSecurityGroup(EntityManager em, ServiceFunctionChain sfc) throws Exception {
        List<SecurityGroup> sgList = SecurityGroupEntityMgr.listSecurityGroupsBySfcId(em, sfc.getId());
        String sgNames = sgList.stream().filter(sg -> !sg.getMarkedForDeletion()).map(sg -> sg.getName())
                .collect(Collectors.joining(", "));
        if (!sgNames.isEmpty()) {
            throw new VmidcBrokerValidationException(String.format("Distributed appliance is referencing to Service Function Chain '%s' and binded to a Security Group(s) '%s'",
                    sfc.getName(), sgNames));
        } else if (!sfc.getMarkedForDeletion() && sfc != null) {
            throw new VmidcBrokerValidationException(String.format("Distributed appliance is referencing to Service Function Chain '%s'",
                    sfc.getName()));
        }
    }
}
