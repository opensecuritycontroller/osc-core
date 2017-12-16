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
package org.osc.core.broker.service.ssl;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.osc.core.broker.service.response.CertificateBasicInfoModel;

public interface X509TrustManagerApi {

    void addEntry(File file) throws Exception;

    void addEntry(X509Certificate certificate, String newAlias) throws Exception;

    void replaceInternalCertificate(File zipFile, boolean doReboot) throws Exception;

    List<CertificateBasicInfoModel> getCertificateInfoList() throws Exception;

    String getSha1Fingerprint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException, IllegalArgumentException;

    String certificateToString(X509Certificate certificate);
}
