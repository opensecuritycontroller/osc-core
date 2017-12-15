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

import java.io.File;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.osc.core.broker.service.api.ReplaceInternalKeypairServiceApi;
import org.osc.core.broker.service.request.UploadRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ReplaceInternalKeypairService extends ServiceDispatcher<UploadRequest, EmptySuccessResponse>
implements ReplaceInternalKeypairServiceApi {

    private static final String FILENAME_EXTENSION = ".zip";
    private static final String FILENAME_PREFIX = "repl_keystore";

    @Reference
    private X509TrustManagerApi x509TrustManagerApi;

    @Override
    protected EmptySuccessResponse exec(UploadRequest request, EntityManager em) throws Exception {
        File zipFile = File.createTempFile(FILENAME_PREFIX, FILENAME_EXTENSION);
        FileUtils.copyInputStreamToFile(request.getUploadedInputStream(), zipFile);
        this.x509TrustManagerApi.replaceInternalCertificate(zipFile, true);
        return new EmptySuccessResponse();
    }
}