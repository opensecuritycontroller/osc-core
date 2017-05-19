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

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.api.GetVSPublicKeyServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.GetVSPublicKeyRequest;
import org.osc.core.broker.service.response.GetVSPublicKeyResponse;
import org.osc.core.util.PKIUtil;


public class GetVSPublicKeyService extends ServiceDispatcher<GetVSPublicKeyRequest, GetVSPublicKeyResponse>
        implements GetVSPublicKeyServiceApi {

    @Override
    public GetVSPublicKeyResponse exec(GetVSPublicKeyRequest request, EntityManager em) throws Exception {

        GetVSPublicKeyResponse response = new GetVSPublicKeyResponse();

        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em, this.txBroadcastUtil);

        VirtualSystem vs = validate(em, request, emgr);

        byte[] keystore = vs.getKeyStore();
        byte[] pubkey = PKIUtil.extractCertificate(keystore);

        response.setPublicKey(pubkey);

        return response;
    }

    VirtualSystem validate(EntityManager em, GetVSPublicKeyRequest request, OSCEntityManager<VirtualSystem> emgr)
            throws Exception {

        Long vsId = request.getVsId();

        if (vsId == null || vsId < 0) {

            throw new VmidcBrokerValidationException("Invalid Virtual System.");
        }

        // retrieve existing entry from db
        VirtualSystem vs = emgr.findByPrimaryKey(vsId);

        if (vs == null) {

            throw new VmidcBrokerValidationException("Virtual System not found.");
        }

        return vs;

    }

}
