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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.api.GetMCPublicKeyServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.GetMCPublicKeyRequest;
import org.osc.core.broker.service.response.GetMCPublicKeyResponse;


public class GetMCPublicKeyService extends ServiceDispatcher<GetMCPublicKeyRequest, GetMCPublicKeyResponse>
        implements GetMCPublicKeyServiceApi {

    @Override
    public GetMCPublicKeyResponse exec(GetMCPublicKeyRequest request, EntityManager em) throws Exception {

        GetMCPublicKeyResponse response = new GetMCPublicKeyResponse();

        ApplianceManagerConnector mc = validate(em, request);

        byte[] publicKey = mc.getPublicKey();

        response.setPublicKey(publicKey);

        return response;
    }

    ApplianceManagerConnector validate(EntityManager em, GetMCPublicKeyRequest request) throws Exception {

        Long mcId = request.getMcId();

        if (mcId == null || mcId < 0) {

            throw new VmidcBrokerValidationException("Invalid Appliance Manager.");
        }

        // retrieve existing entry from db
        OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, em, this.txBroadcastUtil);
        ApplianceManagerConnector mc = emgr.findByPrimaryKey(mcId);

        if (mc == null) {

            throw new VmidcBrokerValidationException("Appliance Manager not found.");
        }

        return mc;

    }

}
