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

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ManagerConnectorConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.UpdateApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
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
import org.osc.core.broker.service.validator.BaseDtoValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UpdateApplianceManagerConnectorService
extends ServiceDispatcher<DryRunRequest<ApplianceManagerConnectorRequest>, BaseJobResponse>
implements UpdateApplianceManagerConnectorServiceApi {

    static final Logger log = LoggerFactory.getLogger(UpdateApplianceManagerConnectorService.class);

    @Reference
    private ManagerConnectorConformJobFactory mcConformJobFactory;

    @Reference
    private AddApplianceManagerConnectorService addApplianceManagerConnectorService;

    @Reference
    private EncryptionApi encryption;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Override
    public BaseJobResponse exec(DryRunRequest<ApplianceManagerConnectorRequest> request, EntityManager em) throws Exception {

        BaseDtoValidator.checkForNullId(request.getDto());

        OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em, this.txBroadcastUtil);

        // retrieve existing entry from db
        ApplianceManagerConnector mc = emgr.findByPrimaryKey(request.getDto().getId());

        Set<SslCertificateAttr> persistentSslCertificatesSet = mc.getSslCertificateAttrSet();

        try {
            validate(em, request, mc, emgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && request.getDto().isForceAddSSLCertificates()) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(em, request, mc, emgr);
            } else {
                throw e;
            }
        }
        String mcName = mc.getName();

        UnlockObjectTask mcUnlock = null;
        try {
            mcUnlock = LockUtil.tryLockMC(mc, LockType.WRITE_LOCK);

            updateApplianceManagerConnector(request, mc);
            SslCertificateAttrEntityMgr sslMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
            mc.setSslCertificateAttrSet(sslMgr.storeSSLEntries(
                    request.getDto().getSslCertificateAttrSet().stream()
                    .map(SslCertificateAttrEntityMgr::createEntity)
                    .collect(toSet()),
                    request.getDto().getId(), persistentSslCertificatesSet));
            emgr.update(mc);

            // Broadcast notifications to UI if MC name has changed, so that Appliance Instances view is refreshed to
            // reflect the correct MC name
            if (!request.getDto().getName().equals(mcName)) {

                List<Long> daiIds = DistributedApplianceInstanceEntityMgr.listByMcId(em, mc.getId());
                if (daiIds != null) {
                    for (Long daiId : daiIds) {
                        this.txBroadcastUtil.addMessageToMap(daiId,
                                DistributedApplianceInstance.class.getSimpleName(), EventType.UPDATED);
                    }
                }
            }

            // Commit the changes early so that the entity is available for the job engine
            UnlockObjectTask forLambda = mcUnlock;
            chain(() -> {
                try {
                    Long jobId = this.mcConformJobFactory.startMCConformJob(mc, forLambda, em).getId();
                    return new BaseJobResponse(mc.getId(), jobId);
                } catch (Exception e) {
                    // If we experience any failure, unlock MC.
                    log.info("Releasing lock for MC '" + mc.getName() + "'");
                    LockManager.getLockManager().releaseLock(new LockRequest(forLambda));
                    throw e;
                }
            });
        } catch (Exception e) {
            // If we experience any failure, unlock MC.
            if (mcUnlock != null) {
                log.info("Releasing lock for MC '" + mc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(mcUnlock));
            }
            throw e;
        }

        return null;
    }

    private DryRunRequest<ApplianceManagerConnectorRequest> internalSSLCertificatesFetch(
            DryRunRequest<ApplianceManagerConnectorRequest> request, SslCertificatesExtendedException sslCertificatesException)
                    throws Exception {
        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();

        int i = 1;
        for (CertificateResolverModel certObj : sslCertificatesException.getCertificateResolverModels()) {
            String newAlias = certObj.getAlias().replaceFirst(SslCertificateAttrEntityMgr.replaceTimestampPattern, "_" + request.getDto().getId() + "_" + i);
            trustManagerFactory.addEntry(certObj.getCertificate(), newAlias);
            request.getDto().getSslCertificateAttrSet().add(new SslCertificateAttrDto(newAlias, certObj.getSha1()));
            i++;
        }

        return request;
    }

    private void validate(EntityManager em, DryRunRequest<ApplianceManagerConnectorRequest> request,
            ApplianceManagerConnector existingMc, OSCEntityManager<ApplianceManagerConnector> emgr) throws Exception {

        boolean basicAuth = this.apiFactoryService.isBasicAuth(request.getDto().getManagerType());

        // check for null/empty values
        ApplianceManagerConnectorDtoValidator.checkForNullFields(request.getDto(), request.isApi(), basicAuth);
        ApplianceManagerConnectorDtoValidator.checkFieldLength(request.getDto(), basicAuth);

        // entry must pre-exist in db
        if (existingMc == null) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector with ID " + request.getDto().getId()
                    + " is not found.");
        }

        // check for uniqueness of mc name
        if (emgr.isDuplicate("name", request.getDto().getName(), existingMc.getId())) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector Name: " + request.getDto().getName()
                    + " already exists.");
        }

        // check for valid IP address format
        ValidateUtil.checkForValidIpAddressFormat(request.getDto().getIpAddress());

        // check for uniqueness of mc IP
        if (emgr.isDuplicate("ipAddress", request.getDto().getIpAddress(), existingMc.getId())) {

            throw new VmidcBrokerValidationException("Appliance Manager IP Address: " + request.getDto().getIpAddress()
                    + " already exists.");
        }

        // cannot change type once created
        if (!request.getDto().getManagerType()
                .equals(existingMc.getManagerType())) {

            throw new VmidcBrokerInvalidRequestException("Cannot change type of Appliance Manager Connector.");
        }

        // check for the deployed appliances by this particular manager
        if (!request.isIgnoreErrorsAndCommit(ErrorType.IP_CHANGED_EXCEPTION)
                && DistributedApplianceEntityMgr.isReferencedByDistributedAppliance(em, existingMc)
                && !existingMc.getIpAddress().equals(request.getDto().getIpAddress())) {

            throw new ErrorTypeException(VmidcMessages.getString(VmidcMessages_.MC_WARNING_IPUPDATE),
                    ErrorType.IP_CHANGED_EXCEPTION);

        }

        // Transforms the existing mc based on the update request
        updateApplianceManagerConnector(request, existingMc);

        this.addApplianceManagerConnectorService.checkManagerConnection(request, existingMc);
    }

    /**
     * Transforms the request to a Manager Connector entity. If the request is coming through the API and it has
     * no password specified, it uses the password from the DB.
     *
     * @throws Exception
     *
     */
    private void updateApplianceManagerConnector(DryRunRequest<ApplianceManagerConnectorRequest> request,
            ApplianceManagerConnector existingMc) throws Exception {

        String mcDbPassword = existingMc.getPassword();
        String mcDbApiKey = existingMc.getApiKey();

        ApplianceManagerConnectorDto dto = request.getDto();
        String serviceName = this.apiFactoryService.getServiceName(dto.getManagerType());
        ApplianceManagerConnectorEntityMgr.toEntity(existingMc, dto, this.encryption, serviceName);

        if (request.isApi()) {
            // For API requests if password is not specified, use existing password unaltered
            if (StringUtils.isEmpty(dto.getPassword())) {
                existingMc.setPassword(mcDbPassword);
            }
            if (StringUtils.isEmpty(dto.getApiKey())) {
                existingMc.setApiKey(mcDbApiKey);
            }
        }
    }

}
