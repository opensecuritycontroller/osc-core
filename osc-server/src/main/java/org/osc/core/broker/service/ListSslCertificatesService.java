package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.SslCertificateDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;

import javax.xml.bind.DatatypeConverter;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class ListSslCertificatesService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<SslCertificateDto>> {

    @Override
    protected ListResponse<SslCertificateDto> exec(BaseRequest<BaseDto> request, Session session) throws Exception {
        List<CertificateBasicInfoModel> certificateInfoList = X509TrustManagerFactory.getInstance().getCertificateInfoList();

        ArrayList<SslCertificateDto> list = new ArrayList<>();
        for (CertificateBasicInfoModel cim : certificateInfoList) {
            list.add(new SslCertificateDto(cim.getAlias(), certificateToString(cim.getCertificate())));
        }

        return new ListResponse<>(list);
    }

    private String certificateToString(X509Certificate certificate) throws CertificateEncodingException {
        StringBuilder cert = new StringBuilder();
        cert.append("-----BEGIN CERTIFICATE----- ");
        cert.append(DatatypeConverter.printBase64Binary(certificate.getEncoded()));
        cert.append(" -----END CERTIFICATE-----");
        return cert.toString();
    }
}