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
package org.osc.core.broker.service.mc;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.EntityManager;
import java.util.ArrayList;

@Component(service = AddApplianceManagerConnectorService.class)
public class AddApplianceManagerConnectorService extends
        ServiceDispatcher<DryRunRequest<ApplianceManagerConnectorDto>, BaseJobResponse> {

    private static final Logger LOG = Logger.getLogger(AddApplianceManagerConnectorService.class);

    private boolean forceAddSSLCertificates = false;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private ConformService conformService;

    public void setForceAddSSLCertificates(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    @Override
    public BaseJobResponse exec(DryRunRequest<ApplianceManagerConnectorDto> request, EntityManager em)
            throws Exception {
        OSCEntityManager<ApplianceManagerConnector> appMgrEntityMgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em);

        try {
            validate(request, appMgrEntityMgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && this.forceAddSSLCertificates) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(request, appMgrEntityMgr);
            } else {
                throw e;
            }
        }

        ApplianceManagerConnector mc =ApplianceManagerConnectorEntityMgr.createEntity(request.getDto());
        mc = appMgrEntityMgr.create(mc);

        SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em);
        mc.setSslCertificateAttrSet(certificateAttrEntityMgr.storeSSLEntries(mc.getSslCertificateAttrSet(), mc.getId()));

        appMgrEntityMgr.update(mc);

        // Commit the changes early so that the entity is available for the job engine
        commitChanges(true);
        UnlockObjectTask mcUnlock = LockUtil.tryLockMC(mc, LockType.WRITE_LOCK);

        Job job = this.conformService.startMCConformJob(mc, mcUnlock, em);
        return new BaseJobResponse(mc.getId(), job.getId());
    }

    private DryRunRequest<ApplianceManagerConnectorDto> internalSSLCertificatesFetch(
            DryRunRequest<ApplianceManagerConnectorDto> request, SslCertificatesExtendedException sslCertificatesException) throws Exception {
        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();
        for (CertificateResolverModel certObj : sslCertificatesException.getCertificateResolverModels()) {
            trustManagerFactory.addEntry(certObj.getCertificate(), certObj.getAlias());
            request.getDto().getSslCertificateAttrSet().add(new SslCertificateAttr(certObj.getAlias(), certObj.getSha1()));
        }
        return request;
    }

    private void validate(DryRunRequest<ApplianceManagerConnectorDto> request,
                          OSCEntityManager<ApplianceManagerConnector> emgr) throws Exception {

        ApplianceManagerConnectorDto.checkForNullFields(request.getDto());
        ApplianceManagerConnectorDto.checkFieldLength(request.getDto());

        // check for uniqueness of mc name
        if (emgr.isExisting("name", request.getDto().getName())) {
            throw new VmidcBrokerValidationException("Appliance Manager Connector Name: " + request.getDto().getName() + " already exists.");
        }

        // check for valid IP address format
        ValidateUtil.checkForValidIpAddressFormat(request.getDto().getIpAddress());

        // check for uniqueness of mc IP
        if (emgr.isExisting("ipAddress", request.getDto().getIpAddress())) {
            throw new VmidcBrokerValidationException("Appliance Manager IP Address: " + request.getDto().getIpAddress() + " already exists.");
        }

        checkManagerConnection(LOG, request, ApplianceManagerConnectorEntityMgr.createEntity(request.getDto()));
    }

    void checkManagerConnection(Logger log, DryRunRequest<ApplianceManagerConnectorDto> request,
                                       ApplianceManagerConnector mc) throws ErrorTypeException {
        if (!request.isSkipAllDryRun() && !request.isIgnoreErrorsAndCommit(ErrorType.MANAGER_CONNECTOR_EXCEPTION)) {

            ArrayList<CertificateResolverModel> resolverModels = new ArrayList<>();

            X509TrustManagerFactory managerFactory = X509TrustManagerFactory.getInstance();
            managerFactory.setListener(model -> {
                model.setAlias("manager_" + model.getAlias());
                resolverModels.add(model);
                managerFactory.clearListener();
            });

            try {
                this.apiFactoryService.checkConnection(mc);
            } catch (Exception e) {
                ErrorTypeException errorTypeException = new ErrorTypeException(e, ErrorType.MANAGER_CONNECTOR_EXCEPTION);
                log.warn("Exception encountered when trying to add Manager Connector, allowing user to either ignore or correct issue");
                if (!resolverModels.isEmpty()) {
                    throw new SslCertificatesExtendedException(errorTypeException, resolverModels);
                }
                throw errorTypeException;
            }
        }
    }
}
