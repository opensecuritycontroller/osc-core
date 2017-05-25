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
package org.osc.core.broker.service.vc;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ListVirtualizationConnectorService
        extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<VirtualizationConnectorDto>>
        implements ListVirtualizationConnectorServiceApi {
    ListResponse<VirtualizationConnectorDto> response = new ListResponse<VirtualizationConnectorDto>();

    @Reference
    private EncryptionApi encryption;

    @Override
    public ListResponse<VirtualizationConnectorDto> exec(BaseRequest<BaseDto> request, EntityManager em) throws EncryptionException {
        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, em, this.txBroadcastUtil);
        // to do mapping
        List<VirtualizationConnectorDto> vcmList = new ArrayList<VirtualizationConnectorDto>();

        // mapping all the VC objects to vc dto objects
        for (VirtualizationConnector vc : emgr.listAll("name")) {
            VirtualizationConnectorDto dto = new VirtualizationConnectorDto();
            VirtualizationConnectorEntityMgr.fromEntity(vc, dto, this.encryption);
            if (request.isApi()) {
                VirtualizationConnectorDto.sanitizeVirtualizationConnector(dto);
            }
            vcmList.add(dto);
        }
        this.response.setList(vcmList);
        return this.response;
    }

}
