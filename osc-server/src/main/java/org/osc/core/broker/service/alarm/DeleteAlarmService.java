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
package org.osc.core.broker.service.alarm;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public class DeleteAlarmService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, Session session) throws Exception {

        Alarm alarm = (Alarm) session.get(Alarm.class, request.getId());

        if (alarm == null) {
            throw new VmidcBrokerValidationException("Alarm entry with id " + request.getId() + " is not found.");
        }

        EntityManager.delete(session, alarm);
        return new EmptySuccessResponse();
    }
}
