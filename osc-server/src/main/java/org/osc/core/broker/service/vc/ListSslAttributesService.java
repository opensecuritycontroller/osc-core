package org.osc.core.broker.service.vc;

import org.hibernate.Session;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListSslAttributesService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<SslCertificateAttrDto>> {
    @Override
    public ListResponse<SslCertificateAttrDto> exec(BaseRequest<BaseDto> request, Session session) {
        SslCertificateAttrEntityMgr sslCertificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
        return new ListResponse<>(sslCertificateAttrEntityMgr.getSslEntriesList());
    }
}