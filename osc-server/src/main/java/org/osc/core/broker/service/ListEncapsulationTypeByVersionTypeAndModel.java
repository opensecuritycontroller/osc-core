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
package org.osc.core.broker.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.request.ListEncapsulationTypeByVersionTypeAndModelRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.sdk.controller.TagEncapsulationType;

public class ListEncapsulationTypeByVersionTypeAndModel extends
        ServiceDispatcher<ListEncapsulationTypeByVersionTypeAndModelRequest, ListResponse<TagEncapsulationType>> {

    ListResponse<TagEncapsulationType> response = new ListResponse<>();

    @Override
    public ListResponse<TagEncapsulationType> exec(ListEncapsulationTypeByVersionTypeAndModelRequest request,
            EntityManager em) throws Exception {
        // TODO properly validate the incoming request
        String virtType = request.getVcType().name();
        List<org.osc.core.broker.model.entities.appliance.TagEncapsulationType> list = ApplianceSoftwareVersionEntityMgr.getEncapsulationByApplianceSoftwareVersion(
                em, request.getAppliacneSoftwareVersion(), request.getAppliacneModel(),
                virtType == null ? null : VirtualizationType.valueOf(virtType));

        if(list != null) {
            this.response.setList(list.stream()
                                  .map(t -> TagEncapsulationType.valueOf(t.name()))
                                  .collect(Collectors.toList()));
        }

        return this.response;
    }
}
