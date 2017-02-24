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
package org.osc.core.broker.service.mc;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.UpdateMCPublicKeyRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;


public class UpdateMCPublicKeyService extends ServiceDispatcher<UpdateMCPublicKeyRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(UpdateMCPublicKeyRequest request, Session session) throws Exception {

        EmptySuccessResponse response = new EmptySuccessResponse();

        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);

        ApplianceManagerConnector mc = validate(session, request);
        mc.setPublicKey(request.getPublicKey());
        emgr.update(mc);

        return response;
    }

    ApplianceManagerConnector validate(Session session, UpdateMCPublicKeyRequest request) throws Exception {

        Long mcId = request.getMcId();
        byte[] publicKey = request.getPublicKey();

        if (mcId == null || mcId < 0) {

            throw new VmidcBrokerValidationException("Invalid Appliance Manager.");
        }

        // note: publicKey == null is a valid case key since it indicates that
        // the key is removed
        if (publicKey != null && publicKey.length == 0) {

            throw new VmidcBrokerValidationException("Invalid Appliance Manager Public Key.");
        }

        // retrieve existing entry from db
        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);
        ApplianceManagerConnector mc = emgr.findByPrimaryKey(mcId);

        if (mc == null) {

            throw new VmidcBrokerValidationException("Appliance Manager not found.");
        }

        return mc;

    }

}
