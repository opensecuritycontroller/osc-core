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
package org.osc.core.broker.service.alert;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListAlertServiceApi;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.AlertEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListAlertService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<AlertDto>>
        implements ListAlertServiceApi {

    @Override
    public ListResponse<AlertDto> exec(BaseRequest<BaseDto> request, EntityManager em) throws Exception {

        // Initializing Entity Manager
        OSCEntityManager<Alert> emgr = new OSCEntityManager<Alert>(Alert.class, em);

        List<AlertDto> alertList = new ArrayList<AlertDto>();

        for (Alert alert : emgr.listAll(false, "createdTimestamp")) {
            AlertDto dto = new AlertDto();
            AlertEntityMgr.fromEntity(alert, dto);
            alertList.add(dto);
        }
        ListResponse<AlertDto> response = new ListResponse<AlertDto>();
        response.setList(alertList);
        return response;
    }
}
