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
import org.osc.core.broker.service.api.ListVirtualizationConnectorBySwVersionServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.ListVirtualizationConnectorBySwVersionRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component
public class ListVirtualizationConnectorBySwVersionService extends
        ServiceDispatcher<ListVirtualizationConnectorBySwVersionRequest, ListResponse<VirtualizationConnectorDto>>
        implements ListVirtualizationConnectorBySwVersionServiceApi {

    @Reference
    EncryptionApi encryption;

    @Override
    public ListResponse<VirtualizationConnectorDto> exec(ListVirtualizationConnectorBySwVersionRequest request,
            EntityManager em) throws EncryptionException {

        ListResponse<VirtualizationConnectorDto> response = new ListResponse<VirtualizationConnectorDto>();
        // to do mapping
        List<VirtualizationConnectorDto> vcmList = new ArrayList<VirtualizationConnectorDto>();
        String swVersion = request.getSwVersion();

        // mapping all the VC objects to vc dto objects
        for (VirtualizationConnector vc : VirtualizationConnectorEntityMgr.listBySwVersion(em, swVersion, this.txBroadcastUtil)) {
            VirtualizationConnectorDto dto = new VirtualizationConnectorDto();
            VirtualizationConnectorEntityMgr.fromEntity(vc, dto, this.encryption);
            vcmList.add(dto);
        }
        response.setList(vcmList);
        return response;
    }

}
