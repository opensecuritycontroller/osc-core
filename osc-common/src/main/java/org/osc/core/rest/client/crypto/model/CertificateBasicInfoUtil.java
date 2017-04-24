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
package org.osc.core.rest.client.crypto.model;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.response.CertificateBasicInfoModel;

public class CertificateBasicInfoUtil {
    private static final Logger LOG = Logger.getLogger(CertificateBasicInfoUtil.class);

    public static CertificateBasicInfoModel create(String alias) {
        CertificateBasicInfoModel cbim = new CertificateBasicInfoModel();
        cbim.setAlias(alias);
        cbim.setCertificate(null);
        cbim.setAlgorithmType("-");
        cbim.setValidFrom(null);
        cbim.setValidTo(null);
        cbim.setSha1Fingerprint(null);
        cbim.setCertificateContent("");
        cbim.setIssuer("");
        return cbim;
    }

    public static CertificateBasicInfoModel create(String alias, String sha1Fingerprint, String issuer, Date validFrom, Date validTo, String algorithmType, X509Certificate certificate) {
        CertificateBasicInfoModel cbim = new CertificateBasicInfoModel();
        cbim.setAlias(alias);
        cbim.setSha1Fingerprint(sha1Fingerprint);
        cbim.setIssuer(issuer);
        cbim.setValidFrom(validFrom);
        cbim.setValidTo(validTo);
        cbim.setAlgorithmType(algorithmType);
        cbim.setCertificate(certificate);
        cbim.setCertificateContent(certificateToString(certificate));
        return cbim;
    }

    private static String certificateToString(X509Certificate certificate) {
        try {
            StringBuilder cert = new StringBuilder();
            cert.append("-----BEGIN CERTIFICATE----- ");
            cert.append(DatatypeConverter.printBase64Binary(certificate.getEncoded()));
            cert.append(" -----END CERTIFICATE-----");
            return cert.toString();
        } catch (CertificateEncodingException e) {
            LOG.error("Cannot encode certificate", e);
            return "";
        }
    }

}