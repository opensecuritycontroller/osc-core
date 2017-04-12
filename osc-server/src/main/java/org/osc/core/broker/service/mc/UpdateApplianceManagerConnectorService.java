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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.EventType;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Set;

@Component(service = UpdateApplianceManagerConnectorService.class)
public class UpdateApplianceManagerConnectorService extends
        ServiceDispatcher<DryRunRequest<ApplianceManagerConnectorDto>, BaseJobResponse> {

    static final Logger log = Logger.getLogger(UpdateApplianceManagerConnectorService.class);

    private boolean forceAddSSLCertificates = false;

    @Reference
    private ConformService conformService;

    @Reference
    private AddApplianceManagerConnectorService addApplianceManagerConnectorService;

    public void setForceAddSSLCertificates(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    @Override
    public BaseJobResponse exec(DryRunRequest<ApplianceManagerConnectorDto> request, EntityManager em) throws Exception {

        BaseDto.checkForNullId(request.getDto());

        OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em);

        // retrieve existing entry from db
        ApplianceManagerConnector mc = emgr.findByPrimaryKey(request.getDto().getId());

        Set<SslCertificateAttr> persistentSslCertificatesSet = mc.getSslCertificateAttrSet();

        try {
            validate(em, request, mc, emgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && this.forceAddSSLCertificates) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(em, request, mc, emgr);
            } else {
                throw e;
            }
        }
        setForceAddSSLCertificates(false); // set default ssl state for future calls

        String mcName = mc.getName();

        UnlockObjectTask mcUnlock = null;
        try {
            mcUnlock = LockUtil.tryLockMC(mc, LockType.WRITE_LOCK);

            updateApplianceManagerConnector(request, mc);
            SslCertificateAttrEntityMgr sslMgr = new SslCertificateAttrEntityMgr(em);
            mc.setSslCertificateAttrSet(sslMgr.storeSSLEntries(request.getDto().getSslCertificateAttrSet(), request.getDto().getId(), persistentSslCertificatesSet));
            emgr.update(mc);

            // Broadcast notifications to UI if MC name has changed, so that Appliance Instances view is refreshed to
            // reflect the correct MC name
            if (!request.getDto().getName().equals(mcName)) {

                List<Long> daiIds = DistributedApplianceInstanceEntityMgr.listByMcId(em, mc.getId());
                if (daiIds != null) {
                    for (Long daiId : daiIds) {
                        TransactionalBroadcastUtil.addMessageToMap(daiId,
                                DistributedApplianceInstance.class.getSimpleName(), EventType.UPDATED);
                    }
                }
            }

            // Commit the changes early so that the entity is available for the job engine
            UnlockObjectTask forLambda = mcUnlock;
            chain(() -> {
                try {
                    Long jobId = this.conformService.startMCConformJob(mc, forLambda, em).getId();
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

    private DryRunRequest<ApplianceManagerConnectorDto> internalSSLCertificatesFetch(
            DryRunRequest<ApplianceManagerConnectorDto> request, SslCertificatesExtendedException sslCertificatesException)
            throws Exception {
        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();

        int i = 1;
        for (CertificateResolverModel certObj : sslCertificatesException.getCertificateResolverModels()) {
            String newAlias = certObj.getAlias().replaceFirst(SslCertificateAttrEntityMgr.replaceTimestampPattern, "_" + request.getDto().getId() + "_" + i);
            trustManagerFactory.addEntry(certObj.getCertificate(), newAlias);
            request.getDto().getSslCertificateAttrSet().add(new SslCertificateAttr(newAlias, certObj.getSha1()));
            i++;
        }

        return request;
    }

    private void validate(EntityManager em, DryRunRequest<ApplianceManagerConnectorDto> request,
                          ApplianceManagerConnector existingMc, OSCEntityManager<ApplianceManagerConnector> emgr) throws Exception {

        // check for null/empty values
        ApplianceManagerConnectorDto.checkForNullFields(request.getDto(), request.isApi());
        ApplianceManagerConnectorDto.checkFieldLength(request.getDto());

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
        if (!request.getDto().getManagerType().getValue()
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

        this.addApplianceManagerConnectorService.checkManagerConnection(log, request, existingMc);
    }

    /**
     * Transforms the request to a Manager Connector entity. If the request is coming through the API and it has
     * no password specified, it uses the password from the DB.
     *
     * @throws Exception
     *
     */
    private void updateApplianceManagerConnector(DryRunRequest<ApplianceManagerConnectorDto> request,
            ApplianceManagerConnector existingMc) throws Exception {

        String mcDbPassword = existingMc.getPassword();
        String mcDbApiKey = existingMc.getApiKey();

        ApplianceManagerConnectorDto dto = request.getDto();
        ApplianceManagerConnectorEntityMgr.toEntity(existingMc, dto);

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
