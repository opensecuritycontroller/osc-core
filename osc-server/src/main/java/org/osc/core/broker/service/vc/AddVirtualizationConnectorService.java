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

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddVirtualizationConnectorServiceRequestValidator;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.RequestValidator;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import javax.persistence.EntityManager;

public class AddVirtualizationConnectorService extends ServiceDispatcher<DryRunRequest<VirtualizationConnectorDto>, BaseJobResponse> {

    private RequestValidator<DryRunRequest<VirtualizationConnectorDto>, VirtualizationConnector> validator;

    private boolean forceAddSSLCertificates = false;
    private ConformService conformService;

    public AddVirtualizationConnectorService(ConformService conformService) {
        this.conformService = conformService;
    }

    public AddVirtualizationConnectorService(ConformService conformService, boolean forceAddSSLCertificates) {
        this(conformService);
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    void setForceAddSSLCertificates(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    @Override
    public BaseJobResponse exec(DryRunRequest<VirtualizationConnectorDto> request, EntityManager em) throws Exception {
        if (this.validator == null) {
            this.validator = new AddVirtualizationConnectorServiceRequestValidator(em);
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
        OSCEntityManager<VirtualizationConnector> vcEntityMgr = new OSCEntityManager<>(VirtualizationConnector.class, em);
        vc = vcEntityMgr.create(vc);

        SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em);
        vc.setSslCertificateAttrSet(certificateAttrEntityMgr.storeSSLEntries(vc.getSslCertificateAttrSet(), vc.getId()));

        vcEntityMgr.update(vc);

        // Commit the changes early so that the entity is available for the job engine
        commitChanges(true);
        Job job = this.conformService.startVCSyncJob(vc, em);
        return new BaseJobResponse(vc.getId(), job.getId());
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
