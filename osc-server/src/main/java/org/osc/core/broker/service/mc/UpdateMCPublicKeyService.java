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
