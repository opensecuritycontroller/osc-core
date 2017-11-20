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

import static java.util.stream.Collectors.toSet;
import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD;
import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.NO_CONTROLLER_TYPE;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.UpdateVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.validator.BaseDtoValidator;
import org.osc.core.broker.service.validator.VirtualizationConnectorDtoValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UpdateVirtualizationConnectorService
extends ServiceDispatcher<DryRunRequest<VirtualizationConnectorRequest>, BaseResponse>
implements UpdateVirtualizationConnectorServiceApi {

    private static final Logger log = LoggerFactory.getLogger(UpdateVirtualizationConnectorService.class);

    @Reference
    private VirtualizationConnectorUtil util;

    @Reference
    private EncryptionApi encryption;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Override
    public BaseResponse exec(DryRunRequest<VirtualizationConnectorRequest> request, EntityManager em) throws Exception {

        BaseDtoValidator.checkForNullId(request.getDto());

        OSCEntityManager<VirtualizationConnector> vcEntityMgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);

        // retrieve existing entry from db
        VirtualizationConnector vc = vcEntityMgr.findByPrimaryKey(request.getDto().getId());

        Set<SslCertificateAttr> persistentSslCertificatesSet = vc.getSslCertificateAttrSet();

        try {
            validate(em, request, vc, vcEntityMgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && request.getDto().isForceAddSSLCertificates()) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(em, request, vc, vcEntityMgr);
            } else {
                throw e;
            }
        }

        String vcName = vc.getName();

        UnlockObjectMetaTask vcUnlock = null;
        try {
            vcUnlock = LockUtil.tryLockVC(vc, LockType.WRITE_LOCK);

            updateVirtualizationConnector(request, vc);
            SslCertificateAttrEntityMgr sslMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
            vc.setSslCertificateAttrSet(sslMgr.storeSSLEntries(request.getDto().getSslCertificateAttrSet().stream()
                    .map(SslCertificateAttrEntityMgr::createEntity)
                    .collect(toSet()),
                    request.getDto().getId(), persistentSslCertificatesSet));
            vcEntityMgr.update(vc);

            // Broadcast notifications to UI if VC name has changed, so that Appliance Instances view and Virtual System
            // view are refreshed to reflect the correct VC name
            if (!request.getDto().getName().equals(vcName)) {

                List<Long> daiIds = DistributedApplianceInstanceEntityMgr.listByVcId(em, vc.getId());
                if (daiIds != null) {
                    for (Long daiId : daiIds) {
                        this.txBroadcastUtil.addMessageToMap(daiId,
                                DistributedApplianceInstance.class.getSimpleName(), EventType.UPDATED);
                    }
                }
                List<Long> vsIds = VirtualSystemEntityMgr.listByVcId(em, vc.getId());
                if (vsIds != null) {
                    for (Long vsId : vsIds) {
                        this.txBroadcastUtil.addMessageToMap(vsId, VirtualSystem.class.getSimpleName(),
                                EventType.UPDATED);
                    }
                }
            }
        } finally {
            LockUtil.releaseLocks(vcUnlock);
        }

        return new BaseResponse(vc.getId());
    }

    private DryRunRequest<VirtualizationConnectorRequest> internalSSLCertificatesFetch(
            DryRunRequest<VirtualizationConnectorRequest> request, SslCertificatesExtendedException sslCertificatesException)
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

    void validate(EntityManager em, DryRunRequest<VirtualizationConnectorRequest> request,
            VirtualizationConnector existingVc, OSCEntityManager<VirtualizationConnector> emgr) throws Exception {

        // check for null/empty values
        VirtualizationConnectorDto dto = request.getDto();
        boolean usesProviderCreds = dto.isControllerDefined() && this.apiFactoryService.usesProviderCreds(dto.getControllerType());
        VirtualizationConnectorDtoValidator.checkForNullFields(dto, request.isApi(), usesProviderCreds);
        VirtualizationConnectorDtoValidator.checkFieldLength(dto);

        // entry must pre-exist in db
        if (existingVc == null) {
            throw new VmidcBrokerValidationException(
                    "Virtualization Connector entry with name " + dto.getName() + " was not found.");
        }

        // check for uniqueness of vc name
        if (emgr.isDuplicate("name", dto.getName(), dto.getId())) {
            throw new VmidcBrokerValidationException(
                    "Virtualization Connector Name: " + dto.getName() + " already exists.");
        }

        // check for uniqueness of controller IP
        if (dto.isControllerDefined() && !this.apiFactoryService.usesProviderCreds(dto.getControllerType())) {
            ValidateUtil.checkForValidIpAddressFormat(dto.getControllerIP());
            if (emgr.isExisting("controllerIpAddress", dto.getControllerIP())) {

                throw new VmidcBrokerValidationException(
                        "Controller IP Address: " + dto.getControllerIP() + " already exists.");
            }
        }

        VirtualizationConnectorDtoValidator.checkFieldFormat(dto);

        // check for uniqueness of Provider IP
        if (emgr.isDuplicate("providerIpAddress", dto.getProviderIP(), dto.getId())) {
            throw new VmidcBrokerValidationException(
                    "Provider IP Address: " + dto.getProviderIP() + " already exists.");
        }

        // cannot change type once created
        if (!dto.getType().name().equals(existingVc.getVirtualizationType().name())) {
            throw new VmidcBrokerInvalidRequestException("Cannot change type of Virtualization Connector.");
        }

        //Check if it connected to Virtual System. If yes generate warning.
        if (!request.isIgnoreErrorsAndCommit(ErrorType.IP_CHANGED_EXCEPTION) && !request.isSkipAllDryRun()
                && existingVc.getVirtualSystems().size() > 0
                && ((existingVc.getControllerIpAddress() != null
                && !existingVc.getControllerIpAddress().equals(dto.getControllerIP())
                || !existingVc.getProviderIpAddress().equals(dto.getProviderIP())))) {
            log.info("Ip changed for either the controller or the provider with deployed Virtual Systems");
            throw new ErrorTypeException(VmidcMessages.getString(VmidcMessages_.VC_WARNING_IPUPDATE),
                    ErrorType.IP_CHANGED_EXCEPTION);
        }

        // If controller type is changed, only NONE->new-type is allowed unconditionally.
        // For all other cases (current-type->NONE, current-type->new-type), there should not be any virtual systems using it.
        if (!existingVc.getControllerType().equals(dto.getControllerType())
                && !existingVc.getControllerType().equals(NO_CONTROLLER_TYPE)
                && (existingVc.getVirtualSystems().size() > 0 || existingVc.getSecurityGroups().size() > 0)) {
            throw new VmidcBrokerInvalidRequestException(
                    "SDN Controller type cannot be changed if this Virtualization Connector is "
                            + "alreday referenced by other objects (Security Groups, Virtual Systems).");
        }

        // Transforms the existing vc based on the update request
        updateVirtualizationConnector(request, existingVc);

        this.util.checkConnection(request, existingVc);
    }

    /**
     * Transforms the request to a Virtualization Connector entity. If the request is coming through the API and it has
     * no password specified, it uses the password from the DB.
     */
    private void updateVirtualizationConnector(DryRunRequest<VirtualizationConnectorRequest> request,
            VirtualizationConnector existingVc) throws EncryptionException {
        // cache existing DB passwords
        String providerDbPassword = existingVc.getProviderPassword();
        String controllerDbPassword = existingVc.getControllerPassword();
        String rabbitDbPassword = existingVc.getProviderAttributes()
                .get(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD);

        VirtualizationConnectorDto dto = request.getDto();
        // Vanilla Transform the request to entity
        VirtualizationConnectorEntityMgr.toEntity(existingVc, dto, this.encryption);

        if (request.isApi()) {
            // For API requests if password is not specified, use existing password unaltered
            if (StringUtils.isEmpty(dto.getProviderPassword())) {
                existingVc.setProviderPassword(providerDbPassword);
            }
            if (StringUtils.isEmpty(dto.getControllerPassword())) {
                existingVc.setControllerPassword(controllerDbPassword);
            }
            if (StringUtils.isEmpty(
                    dto.getProviderAttributes().get(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD))) {
                existingVc.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                        rabbitDbPassword);
            }
        }
    }
}
