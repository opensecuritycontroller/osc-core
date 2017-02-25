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
package org.osc.core.broker.service.vc;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddVirtualizationConnectorServiceRequestValidator;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.RequestValidator;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.core.rest.client.exception.RestClientException;

public class AddVirtualizationConnectorService extends ServiceDispatcher<DryRunRequest<VirtualizationConnectorDto>, BaseResponse> {

    private static final Logger log = Logger.getLogger(AddVirtualizationConnectorService.class);
    private RequestValidator<DryRunRequest<VirtualizationConnectorDto>, VirtualizationConnector> validator;

    private boolean forceAddSSLCertificates = false;

    public AddVirtualizationConnectorService() {
    }

    public AddVirtualizationConnectorService(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    void setForceAddSSLCertificates(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }
    
    @Override
    public BaseResponse exec(DryRunRequest<VirtualizationConnectorDto> request, Session session)
            throws VmidcException, RestClientException, RemoteException, Exception {
        if (this.validator == null) {
            this.validator = new AddVirtualizationConnectorServiceRequestValidator(session);
        }
        try {
            this.validator.validate(request);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && this.forceAddSSLCertificates) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                this.validator.validate(request);
            } else {
                throw e;
            }
        }

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
        EntityManager<VirtualizationConnector> vcEntityMgr = new EntityManager<>(VirtualizationConnector.class, session);
        vc = vcEntityMgr.create(vc);

        SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
        vc.setSslCertificateAttrSet(certificateAttrEntityMgr.storeSSLEntries(vc.getSslCertificateAttrSet(), vc.getId()));

        vcEntityMgr.update(vc);

        return new BaseResponse(vc.getId());
    }

    private DryRunRequest<VirtualizationConnectorDto> internalSSLCertificatesFetch(
            DryRunRequest<VirtualizationConnectorDto> request, SslCertificatesExtendedException sslCertificatesException) throws Exception {
        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();

        if (trustManagerFactory != null) {
            for (CertificateResolverModel certObj : sslCertificatesException.getCertificateResolverModels()) {
                trustManagerFactory.addEntry(certObj.getCertificate(), certObj.getAlias());
                request.getDto().getSslCertificateAttrSet().add(new SslCertificateAttr(certObj.getAlias(), certObj.getSha1()));
            }
        }

        return request;
    }

   }
