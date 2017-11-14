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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.VirtualizationConnectorConformJobFactory;
import org.osc.core.broker.service.api.AddVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.service.validator.AddVirtualizationConnectorServiceRequestValidator;
import org.osc.core.broker.service.validator.RequestValidator;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AddVirtualizationConnectorService
        extends ServiceDispatcher<DryRunRequest<VirtualizationConnectorRequest>, BaseJobResponse>
        implements AddVirtualizationConnectorServiceApi {

    private RequestValidator<DryRunRequest<VirtualizationConnectorRequest>, VirtualizationConnector> validator;

    @Reference
    private VirtualizationConnectorConformJobFactory vcConformJobFactory;

    @Reference
    EncryptionApi encryption;

    @Reference
    private VirtualizationConnectorUtil virtualizationConnectorUtil;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Override
    public BaseJobResponse exec(DryRunRequest<VirtualizationConnectorRequest> request, EntityManager em) throws Exception {
        if (this.validator == null) {
            this.validator = new AddVirtualizationConnectorServiceRequestValidator(em, this.txBroadcastUtil,
                    this.virtualizationConnectorUtil, this.apiFactoryService);
        }
        try {
            this.validator.validate(request);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && request.getDto().isForceAddSSLCertificates()) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                this.validator.validate(request);
            } else {
                throw e;
            }
        }

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encryption);
        OSCEntityManager<VirtualizationConnector> vcEntityMgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
        vc = vcEntityMgr.create(vc);

        SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
        vc.setSslCertificateAttrSet(certificateAttrEntityMgr.storeSSLEntries(vc.getSslCertificateAttrSet(), vc.getId()));

        vcEntityMgr.update(vc);

        Job job = this.vcConformJobFactory.startVCSyncJob(vc, em);
        return new BaseJobResponse(vc.getId(), job.getId());
    }

    private DryRunRequest<VirtualizationConnectorRequest> internalSSLCertificatesFetch(
            DryRunRequest<VirtualizationConnectorRequest> request, SslCertificatesExtendedException sslCertificatesException) throws Exception {
        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();

        if (trustManagerFactory != null) {
            for (CertificateResolverModel certObj : sslCertificatesException.getCertificateResolverModels()) {
                trustManagerFactory.addEntry(certObj.getCertificate(), certObj.getAlias());
                request.getDto().getSslCertificateAttrSet().add(new SslCertificateAttrDto(certObj.getAlias(), certObj.getSha1()));
            }
        }

        return request;
    }

}
