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
package org.osc.core.broker.service;

import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.request.ListApplianceModelSwVersionComboRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListApplianceModelSwVersionComboService extends
        ServiceDispatcher<ListApplianceModelSwVersionComboRequest, ListResponse<ApplianceModelSoftwareVersionDto>> {

    ListResponse<ApplianceModelSoftwareVersionDto> response = new ListResponse<ApplianceModelSoftwareVersionDto>();

    @Override
    public ListResponse<ApplianceModelSoftwareVersionDto> exec(ListApplianceModelSwVersionComboRequest request,
            Session session) {

        ManagerType mcType = request.getType();

        List<ApplianceModelSoftwareVersionDto> ls = ApplianceSoftwareVersionEntityMgr.findByMcType(session, mcType);
        this.response.setList(ls);

        return this.response;

    }

}
