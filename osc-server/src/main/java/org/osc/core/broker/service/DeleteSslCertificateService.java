package org.osc.core.broker.service;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.view.maintenance.SslConfigurationLayout;

public class DeleteSslCertificateService extends ServiceDispatcher<DeleteSslEntryRequest, EmptySuccessResponse> {

    private static final Logger log = Logger.getLogger(DeleteSslCertificateService.class);

    @Override
    protected EmptySuccessResponse exec(DeleteSslEntryRequest request, Session session) throws Exception {
        if (request.getAlias().contains(SslConfigurationLayout.INTERNAL_CERTIFICATE_ALIAS)) {
            throw new VmidcBrokerValidationException("Cannot remove internal certificate");
        }

        try {
            SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
            boolean succeed = certificateAttrEntityMgr.removeAlias(request.getAlias());
            log.info("Deleted alias: " + request.getAlias() + " from trust store status: " + succeed);
            if (!succeed) {
                throw new VmidcBrokerValidationException("Cannot remove entry: " + request.getAlias() + " from trust store");
            }
        } catch (Exception e) {
            throw new VmidcBrokerValidationException("Cannot remove entry: " + request.getAlias() + " from trust store");
        }

        return new EmptySuccessResponse();
    }
}