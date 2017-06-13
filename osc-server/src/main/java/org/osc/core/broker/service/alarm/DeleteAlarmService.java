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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.DeleteAlarmServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class DeleteAlarmService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse>
        implements DeleteAlarmServiceApi {

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, EntityManager em) throws Exception {

        Alarm alarm = em.find(Alarm.class, request.getId());

        if (alarm == null) {
            throw new VmidcBrokerValidationException("Alarm entry with id " + request.getId() + " is not found.");
        }

        OSCEntityManager.delete(em, alarm, this.txBroadcastUtil);
        return new EmptySuccessResponse();
    }
}
