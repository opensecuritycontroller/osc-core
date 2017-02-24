package org.osc.core.broker.service.vc;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public class DeleteVirtualizationConnectorService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse> {
    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, Session session) throws VmidcException, Exception {
        validate(session, request);

        EntityManager<VirtualizationConnector> vcEntityMgr = new EntityManager<>(VirtualizationConnector.class, session);
        VirtualizationConnector connector = vcEntityMgr.findByPrimaryKey(request.getId());

        SslCertificateAttrEntityMgr sslCertificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
        sslCertificateAttrEntityMgr.removeCertificateList(connector.getSslCertificateAttrSet());
        vcEntityMgr.delete(request.getId());

        return new EmptySuccessResponse();
    }

    void validate(Session session, BaseIdRequest request) throws VmidcException, Exception {
        VirtualizationConnector vc = (VirtualizationConnector) session.get(VirtualizationConnector.class, request.getId());

        // entry must pre-exist in db
        if (vc == null) { // note: we cannot use name here in error msg since del req does not have name, only ID
            throw new VmidcBrokerValidationException("Virtualization Connector entry with ID " + request.getId() + " is not found.");
        }

        VirtualizationConnectorEntityMgr.validateCanBeDeleted(session, vc);
    }

}
