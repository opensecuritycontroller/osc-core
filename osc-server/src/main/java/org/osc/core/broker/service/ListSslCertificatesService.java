package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;

import java.util.List;

public class ListSslCertificatesService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<CertificateBasicInfoModel>> {

    @Override
    protected ListResponse<CertificateBasicInfoModel> exec(BaseRequest<BaseDto> request, Session session) throws Exception {
        List<CertificateBasicInfoModel> certificateInfoList = X509TrustManagerFactory.getInstance().getCertificateInfoList();

        SslCertificateAttrEntityMgr sslCertificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
        List<SslCertificateAttrDto> sslEntriesList = sslCertificateAttrEntityMgr.getSslEntriesList();

        for (CertificateBasicInfoModel cim : certificateInfoList) {
            cim.setConnected(isConnected(sslEntriesList, cim.getAlias()));
        }

        return new ListResponse<>(certificateInfoList);
    }

    /**
     * Checks if given alias exists in list
     * @param sslEntriesList - fetched entries list
     * @param alias - search item
     * @return bool - exists
     */
    private boolean isConnected(List<SslCertificateAttrDto> sslEntriesList, String alias) {
        return sslEntriesList.stream().anyMatch(attribute -> attribute.getAlias() != null && attribute.getAlias().contains(alias));
    }

}