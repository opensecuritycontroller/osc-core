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
package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.UpdateVSKeyStoreRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;


public class UpdateVSKeyStoreService extends ServiceDispatcher<UpdateVSKeyStoreRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(UpdateVSKeyStoreRequest request, Session session) throws Exception {

        EmptySuccessResponse response = new EmptySuccessResponse();

        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);

        VirtualSystem vs = validate(session, request, emgr);

        vs.setKeyStore(request.getKeyStore());
        emgr.update(vs);

        return response;
    }

    VirtualSystem validate(Session session, UpdateVSKeyStoreRequest request, EntityManager<VirtualSystem> emgr)
            throws Exception {

        Long vsId = request.getVsId();
        byte[] KeyStore = request.getKeyStore();

        if (vsId == null || vsId < 0) {

            throw new VmidcBrokerValidationException("Invalid Virtual System.");
        }

        // note: KeyStore == null is a valid case key since it indicates that
        // KeyStore is removed
        if (KeyStore != null && KeyStore.length == 0) {

            throw new VmidcBrokerValidationException("Invalid KeyStore.");
        }

        // retrieve existing entry from db
        VirtualSystem vs = emgr.findByPrimaryKey(vsId);

        if (vs == null) {

            throw new VmidcBrokerValidationException("Virtual System not found.");
        }

        return vs;

    }

}
