/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.AddSslCertificateServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.AddSslEntryRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osgi.service.component.annotations.Component;

@Component
public class AddSslCertificateService extends ServiceDispatcher<AddSslEntryRequest, EmptySuccessResponse>
        implements AddSslCertificateServiceApi {

    private static final Logger log = Logger.getLogger(AddSslCertificateService.class);

    @Override
    protected EmptySuccessResponse exec(AddSslEntryRequest request, EntityManager em) throws Exception {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(request.getCertificate().getBytes(StandardCharsets.UTF_8)));
            X509TrustManagerFactory.getInstance().addEntry(certificate, request.getAlias());
        } catch (Exception e) {
            log.error("Cannot add new SSL certificate with alias: " +request.getAlias()+ " to trust store", e);
            throw new VmidcBrokerInvalidEntryException("Failed to add new SSL certificate with alias: " + request.getAlias());
        }
        return new EmptySuccessResponse();
    }
}