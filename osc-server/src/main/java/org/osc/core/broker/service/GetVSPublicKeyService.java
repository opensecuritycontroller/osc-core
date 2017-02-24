package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.GetVSPublicKeyRequest;
import org.osc.core.broker.service.response.GetVSPublicKeyResponse;
import org.osc.core.util.PKIUtil;


public class GetVSPublicKeyService extends ServiceDispatcher<GetVSPublicKeyRequest, GetVSPublicKeyResponse> {

    @Override
    public GetVSPublicKeyResponse exec(GetVSPublicKeyRequest request, Session session) throws Exception {

        GetVSPublicKeyResponse response = new GetVSPublicKeyResponse();

        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);

        VirtualSystem vs = validate(session, request, emgr);

        byte[] keystore = vs.getKeyStore();
        byte[] pubkey = PKIUtil.extractCertificate(keystore);

        response.setPublicKey(pubkey);

        return response;
    }

    VirtualSystem validate(Session session, GetVSPublicKeyRequest request, EntityManager<VirtualSystem> emgr)
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
