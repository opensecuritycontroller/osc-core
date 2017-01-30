package org.osc.core.broker.service;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.service.request.AddSslEntryRequest;
import org.osc.core.broker.service.response.CommonResponse;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class AddSslCertificateService extends ServiceDispatcher<AddSslEntryRequest, CommonResponse> {

    private static final Logger log = Logger.getLogger(AddSslCertificateService.class);

    @Override
    protected CommonResponse exec(AddSslEntryRequest request, Session session) throws Exception {
        boolean succeed = true;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(request.getCertificate().getBytes(StandardCharsets.UTF_8)));
            X509TrustManagerFactory.getInstance().addEntry(certificate, request.getAlias());
        } catch (Exception e) {
        	log.error("Cannot add new SSL certificate to truststore", e);
            succeed = false;
        }
        return new CommonResponse(succeed);
    }
}