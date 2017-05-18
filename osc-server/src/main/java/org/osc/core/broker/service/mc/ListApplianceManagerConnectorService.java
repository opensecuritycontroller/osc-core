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
package org.osc.core.broker.service.mc;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ListApplianceManagerConnectorService
        extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<ApplianceManagerConnectorDto>>
        implements ListApplianceManagerConnectorServiceApi {

    @Reference
    private EncryptionApi encryption;


    @Override
    public ListResponse<ApplianceManagerConnectorDto> exec(BaseRequest<BaseDto> request, EntityManager em) throws Exception {
        ListResponse<ApplianceManagerConnectorDto> response = new ListResponse<>();
        // Initializing Entity Manager
        OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em, this.txBroadcastUtil);
        // to do mapping
        List<ApplianceManagerConnectorDto> mcmList = new ArrayList<>();

        // mapping all the MC objects to mc dto objects
        for (ApplianceManagerConnector mc : emgr.listAll("name")) {
            ApplianceManagerConnectorDto dto = new ApplianceManagerConnectorDto();
            ApplianceManagerConnectorEntityMgr.fromEntity(mc, dto, this.encryption);
            if (request.isApi()) {
                ApplianceManagerConnectorDto.sanitizeManagerConnector(dto);
            }

            boolean isPolicyMappingSupported = ManagerApiFactory.syncsPolicyMapping(ManagerType.fromText(mc.getManagerType()));

            dto.setPolicyMappingSupported(isPolicyMappingSupported);

            mcmList.add(dto);
        }
        response.setList(mcmList);
        return response;
    }

}
