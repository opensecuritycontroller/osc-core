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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

@XmlRootElement(name = "certificateBasicInfoModel")
@XmlAccessorType(XmlAccessType.FIELD)
public class CertificateBasicInfoModel {

    private static final Logger LOG = Logger.getLogger(CertificateBasicInfoModel.class);

    private String alias;
    private String sha1Fingerprint;
    private Date validFrom;
    private Date validTo;
    private String issuer;
    private String algorithmType;

    @JsonIgnore
    private X509Certificate certificate;

    private String certificateContent;

    private boolean isConnected;

    public CertificateBasicInfoModel() {
    }

    public CertificateBasicInfoModel(String alias) {
        this.alias = alias;
        this.certificate = null;
        this.algorithmType = "-";
        this.validFrom = null;
        this.validTo = null;
        this.sha1Fingerprint = null;
        this.certificateContent = "";
        this.issuer = "";
    }

    public CertificateBasicInfoModel(String alias, String sha1Fingerprint, String issuer, Date validFrom, Date validTo, String algorithmType, X509Certificate certificate) {
        this.alias = alias;
        this.sha1Fingerprint = sha1Fingerprint;
        this.issuer = issuer;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.algorithmType = algorithmType;
        this.certificate = certificate;
        this.certificateContent = certificateToString(certificate);
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSha1Fingerprint() {
        return this.sha1Fingerprint;
    }

    public void setSha1Fingerprint(String sha1Fingerprint) {
        this.sha1Fingerprint = sha1Fingerprint;
    }

    public Date getValidFrom() {
        return this.validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return this.validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getAlgorithmType() {
        return this.algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    public X509Certificate getCertificate() {
        return this.certificate;
    }

    public String getCertificateContent() {
        return this.certificateContent;
    }

    public void setCertificateContent(String certificateContent) {
        this.certificateContent = certificateContent;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
  
    public boolean isConnected() {
        return this.isConnected;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;
    }

    private String certificateToString(X509Certificate cert) {
        StringWriter sw = new StringWriter();
        try {
            sw.write("-----BEGIN CERTIFICATE-----\n");
            sw.write(DatatypeConverter.printBase64Binary(cert.getEncoded()).replaceAll("(.{64})", "$1\n"));
            sw.write("\n-----END CERTIFICATE-----");
        } catch (CertificateEncodingException e) {
            LOG.error("Cannot encode certificate", e);
        }
        return sw.toString();
    }
}