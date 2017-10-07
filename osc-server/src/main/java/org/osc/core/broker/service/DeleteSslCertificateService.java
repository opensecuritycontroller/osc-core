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

import javax.persistence.EntityManager;

import org.osc.core.broker.service.api.DeleteSslCertificateServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component
public class DeleteSslCertificateService extends ServiceDispatcher<DeleteSslEntryRequest, EmptySuccessResponse>
        implements DeleteSslCertificateServiceApi {

    private static final String INTERNAL_CERTIFICATE_MARKER = "internal";
    private static final Logger log = LoggerFactory.getLogger(DeleteSslCertificateService.class);

    @Override
    protected EmptySuccessResponse exec(DeleteSslEntryRequest request, EntityManager em) throws Exception {
        if (request.getAlias().contains(INTERNAL_CERTIFICATE_MARKER)) {
            throw new VmidcBrokerValidationException("Cannot remove internal certificate");
        }

        try {
            SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
            boolean succeed = certificateAttrEntityMgr.removeAlias(request.getAlias());
            log.info("Deleted alias: " + request.getAlias() + " from trust store status: " + succeed);
            if (!succeed) {
                throw new VmidcBrokerValidationException("Cannot remove entry: " + request.getAlias() + " from trust store");
            }
        } catch (Exception e) {
            throw new VmidcBrokerValidationException("Cannot remove entry: " + request.getAlias() + " from trust store");
        }

        return new EmptySuccessResponse();
    }
}
