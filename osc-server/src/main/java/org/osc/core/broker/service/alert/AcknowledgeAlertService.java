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

import java.util.Date;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.AcknowledgeAlertServiceApi;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.AlertEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.AlertRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.validator.AlertDtoValidator;
import org.osc.core.broker.service.validator.BaseDtoValidator;
import org.osc.core.common.job.AcknowledgementStatus;
import org.osgi.service.component.annotations.Component;

@Component
public class AcknowledgeAlertService extends ServiceDispatcher<AlertRequest, EmptySuccessResponse>
implements AcknowledgeAlertServiceApi {

    @Override
    public EmptySuccessResponse exec(AlertRequest request, EntityManager em) throws Exception {
        EmptySuccessResponse response = new EmptySuccessResponse();
        OSCEntityManager<Alert> emgr = new OSCEntityManager<Alert>(Alert.class, em, this.txBroadcastUtil);

        if (request.isAcknowledge()) {
            for (AlertDto dto : request.getDtoList()) {
                dto.setStatus(AcknowledgementStatus.ACKNOWLEDGED);
                dto.setAcknowledgedUser(this.userContext.getCurrentUser());
                dto.setTimeAcknowledgedTimestamp(new Date());
            }
        } else {
            //This is unacknowledge request
            for (AlertDto dto : request.getDtoList()) {
                dto.setStatus(AcknowledgementStatus.PENDING_ACKNOWLEDGEMENT);
                dto.setAcknowledgedUser(null);
                dto.setTimeAcknowledgedTimestamp(null);
            }
        }

        for (AlertDto dto : request.getDtoList()) {
            BaseDtoValidator.checkForNullId(dto);
            Alert alert = emgr.findByPrimaryKey(dto.getId());
            // Do not update an alert if the request is to acknowledge an already acknowledged alert
            if (!((alert.getStatus().equals(AcknowledgementStatus.ACKNOWLEDGED) && request.isAcknowledge()))) {
                validate(dto, alert);
                AlertEntityMgr.toEntity(alert, dto);
                emgr.update(alert);
            }
        }

        return response;
    }

    void validate(AlertDto dto, Alert alert) throws Exception {

        // entry must pre-exist in db
        if (alert == null) {
            throw new VmidcBrokerValidationException("Alert entry with id " + dto.getId() + " is not found.");
        }

        // check for null/empty values
        AlertDtoValidator.checkForNullFields(dto);
    }
}
