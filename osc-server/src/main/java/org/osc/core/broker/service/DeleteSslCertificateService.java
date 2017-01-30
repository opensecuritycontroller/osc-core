package org.osc.core.broker.service;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.CommonResponse;

public class DeleteSslCertificateService extends ServiceDispatcher<DeleteSslEntryRequest, CommonResponse> {

    private static final Logger log = Logger.getLogger(DeleteSslCertificateService.class);

    @Override
    protected CommonResponse exec(DeleteSslEntryRequest request, Session session) throws Exception {
        boolean succeed = true;
        try {
            SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
            certificateAttrEntityMgr.removeAlias(request.getAlias());
        } catch (Exception e) {
            log.error("Cannot remove entry from trust store", e);
            succeed = false;
        }
        return new CommonResponse(succeed);
    }
}