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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.DeleteAlertServiceApi;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.AlertRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class DeleteAlertService extends ServiceDispatcher<AlertRequest, EmptySuccessResponse>
        implements DeleteAlertServiceApi {

    @Override
    public EmptySuccessResponse exec(AlertRequest request, EntityManager em) throws Exception {
        EmptySuccessResponse response = new EmptySuccessResponse();

        for (AlertDto dto : request.getDtoList()) {
            Alert alert = em.find(Alert.class, dto.getId());
            if (alert == null) {
                throw new VmidcBrokerValidationException("Alarm entry with id " + dto.getId() + " is not found.");
            }
            OSCEntityManager.delete(em, alert, this.txBroadcastUtil);
        }
        return response;
    }
}
