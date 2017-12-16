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

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import javax.net.ssl.SSLException;
import javax.persistence.EntityManager;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ManagerConnectorConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.AddApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.ApplianceManagerConnectorRequest;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.validator.ApplianceManagerConnectorDtoValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component exposes both the API and the implementation so that the
 * {@link AddApplianceManagerConnectorService} and {@link UpdateApplianceManagerConnectorService}
 * can call the {@link #checkManagerConnection(DryRunRequest, ApplianceManagerConnector)}
 * method.
 */
@Component(service={AddApplianceManagerConnectorService.class, AddApplianceManagerConnectorServiceApi.class})
public class AddApplianceManagerConnectorService
extends ServiceDispatcher<DryRunRequest<ApplianceManagerConnectorRequest>, BaseJobResponse>
implements AddApplianceManagerConnectorServiceApi {

    private static final Logger LOG = LoggerFactory.getLogger(AddApplianceManagerConnectorService.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private ManagerConnectorConformJobFactory mcConformJobFactory;

    @Reference
    private EncryptionApi encryption;

    @Override
    public BaseJobResponse exec(DryRunRequest<ApplianceManagerConnectorRequest> request, EntityManager em)
            throws Exception {
        OSCEntityManager<ApplianceManagerConnector> appMgrEntityMgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em, this.txBroadcastUtil);

        try {
            validate(request, appMgrEntityMgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && request.getDto().isForceAddSSLCertificates()) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(request, appMgrEntityMgr);
            } else {
                throw e;
            }
        }

        String serviceName = this.apiFactoryService.getServiceName(request.getDto().getManagerType());
        ApplianceManagerConnector mc = ApplianceManagerConnectorEntityMgr.createEntity(request.getDto(), this.encryption, serviceName);
        appMgrEntityMgr.create(mc);

        SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
        mc.setSslCertificateAttrSet(certificateAttrEntityMgr.storeSSLEntries(mc.getSslCertificateAttrSet(), mc.getId()));

        appMgrEntityMgr.update(mc);

        // Commit the changes early so that the entity is available for the job engine
        chain(() -> {
            UnlockObjectTask mcUnlock = LockUtil.tryLockMC(mc, LockType.WRITE_LOCK);
            Job job = this.mcConformJobFactory.startMCConformJob(mc, mcUnlock, em);
            return new BaseJobResponse(mc.getId(), job.getId());
        });

        return null;
    }

    private DryRunRequest<ApplianceManagerConnectorRequest> internalSSLCertificatesFetch(
            DryRunRequest<ApplianceManagerConnectorRequest> request, SslCertificatesExtendedException sslCertificatesException) throws Exception {
        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();
        for (CertificateResolverModel certObj : sslCertificatesException.getCertificateResolverModels()) {
            trustManagerFactory.addEntry(certObj.getCertificate(), certObj.getAlias());
            request.getDto().getSslCertificateAttrSet().add(new SslCertificateAttrDto(certObj.getAlias(), certObj.getSha1()));
        }
        return request;
    }

    private void validate(DryRunRequest<ApplianceManagerConnectorRequest> request,
            OSCEntityManager<ApplianceManagerConnector> emgr) throws Exception {

        String managerType = request.getDto().getManagerType();
        boolean basicAuth = this.apiFactoryService.isBasicAuth(managerType);
        ApplianceManagerConnectorDtoValidator.checkForNullFields(request.getDto(), false, basicAuth);
        ApplianceManagerConnectorDtoValidator.checkFieldLength(request.getDto(), basicAuth);

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

        String serviceName = this.apiFactoryService.getServiceName(request.getDto().getManagerType());
        checkManagerConnection(request, ApplianceManagerConnectorEntityMgr.createEntity(request.getDto(), this.encryption, serviceName));
    }

    void checkManagerConnection(DryRunRequest<ApplianceManagerConnectorRequest> request,
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

                Throwable rootCause = ExceptionUtils.getRootCause(e);
                Throwable cause = e;

                if (rootCause instanceof SocketException
                        || rootCause instanceof SSLException
                        || rootCause instanceof SocketTimeoutException) {
                    cause = rootCause;
                }

                ErrorTypeException errorTypeException = new ErrorTypeException(cause, ErrorType.MANAGER_CONNECTOR_EXCEPTION);
                LOG.warn("Exception encountered when trying to add Manager Connector, allowing user to either ignore or correct issue");
                if (!resolverModels.isEmpty()) {
                    throw new SslCertificatesExtendedException(errorTypeException, resolverModels);
                }
                throw errorTypeException;
            }
        }
    }
}
