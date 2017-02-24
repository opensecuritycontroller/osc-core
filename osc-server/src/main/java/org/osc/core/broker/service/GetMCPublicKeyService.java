package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.GetMCPublicKeyRequest;
import org.osc.core.broker.service.response.GetMCPublicKeyResponse;


public class GetMCPublicKeyService extends ServiceDispatcher<GetMCPublicKeyRequest, GetMCPublicKeyResponse> {

    @Override
    public GetMCPublicKeyResponse exec(GetMCPublicKeyRequest request, Session session) throws Exception {

        GetMCPublicKeyResponse response = new GetMCPublicKeyResponse();

        ApplianceManagerConnector mc = validate(session, request);

        byte[] publicKey = mc.getPublicKey();

        response.setPublicKey(publicKey);

        return response;
    }

    ApplianceManagerConnector validate(Session session, GetMCPublicKeyRequest request) throws Exception {

        Long mcId = request.getMcId();

        if (mcId == null || mcId < 0) {

            throw new VmidcBrokerValidationException("Invalid Appliance Manager.");
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
