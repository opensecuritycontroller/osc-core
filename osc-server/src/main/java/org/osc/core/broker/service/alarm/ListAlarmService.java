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
package org.osc.core.broker.service.alarm;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListAlarmServiceApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.AlarmEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class ListAlarmService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<AlarmDto>>
        implements ListAlarmServiceApi {

    @Override
    public ListResponse<AlarmDto> exec(BaseRequest<BaseDto> request, EntityManager em) throws Exception {
        // Initializing Entity Manager
        OSCEntityManager<Alarm> emgr = new OSCEntityManager<Alarm>(Alarm.class, em, this.txBroadcastUtil);

        List<AlarmDto> alarmList = new ArrayList<AlarmDto>();

        // Mapping all the Alarm objects to Alarm dto objects
        for (Alarm alarm : emgr.listAll("name")) {
            AlarmDto dto = new AlarmDto();
            AlarmEntityMgr.fromEntity(alarm, dto);
            alarmList.add(dto);
        }
        ListResponse<AlarmDto> response = new ListResponse<AlarmDto>();
        response.setList(alarmList);
        return response;
    }
}
