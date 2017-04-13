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

import javax.persistence.EntityManager;

import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.ListVirtualSystemRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListVirtualSystemService extends ServiceDispatcher<ListVirtualSystemRequest, ListResponse<VirtualSystemDto>> {

    ListResponse<VirtualSystemDto> response = new ListResponse<VirtualSystemDto>();

    @Override
    public ListResponse<VirtualSystemDto> exec(ListVirtualSystemRequest request, EntityManager em) {

        long mcId = request.getMcId();
        long applianceId = request.getApplianceId();
        String applianceSwVer = request.getApplianceSoftwareVersionName();

        List<VirtualSystemDto> ls = VirtualSystemEntityMgr.findByMcApplianceAndSwVer(em, mcId, applianceId, applianceSwVer);
        this.response.setList(ls);

        return this.response;

    }

}
