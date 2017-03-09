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
package org.osc.core.broker.service.request;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;

public class UpdateDaiConsolePasswordRequestValidator implements ListRequestValidator<UpdateDaiConsolePasswordRequest, DistributedApplianceInstance> {

    private EntityManager em;

    public UpdateDaiConsolePasswordRequestValidator(EntityManager em) {
        this.em = em;
    }

    @Override
    public void validate(UpdateDaiConsolePasswordRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedApplianceInstance validateAndLoad(UpdateDaiConsolePasswordRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DistributedApplianceInstance> validateAndLoadList(UpdateDaiConsolePasswordRequest request) throws Exception {
        String vsName = request.getVsName();

        if (vsName == null || vsName.isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid Virtual System Name.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid password.");
        }

        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<>(VirtualSystem.class, this.em);

        // This code used to use VirtualSystem.getVsIdFromName(vsName), but the name is already unique!
        //Long vsId = VirtualSystem.getVsIdFromName(vsName);
        VirtualSystem vs = emgr.findByFieldName("name", vsName);

        if (vs == null) {
            // Avoid changing external behaviour
            Long vsId = VirtualSystem.getVsIdFromName(vsName);
            throw new VmidcBrokerValidationException("Virtual System with ID: " + vsId + " not found.");
        }

        List<DistributedApplianceInstance> daiList = new ArrayList<>();
        if (request.getDaiList() != null && !request.getDaiList().isEmpty()) {

            OSCEntityManager<DistributedApplianceInstance> daiEmgr = new OSCEntityManager<>(
                    DistributedApplianceInstance.class, this.em);

            for (String daiName : request.getDaiList()) {
                DistributedApplianceInstance dai = daiEmgr.findByFieldName("name", daiName);

                if (dai != null) {
                    if (!dai.getVirtualSystem().equals(vs)) {
                        throw new VmidcException("DAI '" + daiName + "' is not a member of VSS '" + vs.getName() + "'.");
                    }
                    daiList.add(dai);
                } else {
                    throw new VmidcException("DAI '" + daiName + "' not found.");
                }
            }

        } else {

            daiList = DistributedApplianceInstanceEntityMgr.listByVsId(this.em, vs.getId());
            if (daiList == null || daiList.isEmpty()) {
                throw new VmidcException("VSS '" + vs.getName() + "' does not have members.");
            }

        }

        return daiList;
    }
}
