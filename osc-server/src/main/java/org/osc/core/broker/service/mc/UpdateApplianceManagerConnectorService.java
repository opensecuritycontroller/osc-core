package org.osc.core.broker.service.mc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
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
import org.osc.core.broker.service.persistence.EntityManager;
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

import java.util.List;
import java.util.Set;

public class UpdateApplianceManagerConnectorService extends
        ServiceDispatcher<DryRunRequest<ApplianceManagerConnectorDto>, BaseJobResponse> {

    static final Logger log = Logger.getLogger(UpdateApplianceManagerConnectorService.class);

    private boolean forceAddSSLCertificates = false;

    public UpdateApplianceManagerConnectorService() {
    }

    public UpdateApplianceManagerConnectorService(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }


    @Override
    public BaseJobResponse exec(DryRunRequest<ApplianceManagerConnectorDto> request, Session session) throws Exception {

        BaseDto.checkForNullId(request.getDto());

        BaseJobResponse response = new BaseJobResponse();

        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<>(ApplianceManagerConnector.class, session);

        // retrieve existing entry from db
        ApplianceManagerConnector mc = emgr.findByPrimaryKey(request.getDto().getId());

        Set<SslCertificateAttr> persistentSslCertificatesSet = mc.getSslCertificateAttrSet();

        try {
            validate(session, request, mc, emgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && this.forceAddSSLCertificates) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(session, request, mc, emgr);
            } else {
                throw e;
            }
        }

        String mcName = mc.getName();

        UnlockObjectTask mcUnlock = null;
        try {
            mcUnlock = LockUtil.tryLockMC(mc, LockType.WRITE_LOCK);

            updateApplianceManagerConnector(request, mc);
            SslCertificateAttrEntityMgr sslMgr = new SslCertificateAttrEntityMgr(session);
            mc.setSslCertificateAttrSet(sslMgr.storeSSLEntries(request.getDto().getSslCertificateAttrSet(), request.getDto().getId(), persistentSslCertificatesSet));
            emgr.update(mc);

            // Broadcast notifications to UI if MC name has changed, so that Appliance Instances view is refreshed to
            // reflect the correct MC name
            if (!request.getDto().getName().equals(mcName)) {

                List<Long> daiIds = DistributedApplianceInstanceEntityMgr.listByMcId(session, mc.getId());
                if (daiIds != null) {
                    for (Long daiId : daiIds) {
                        TransactionalBroadcastUtil.addMessageToMap(session, daiId,
                                DistributedApplianceInstance.class.getSimpleName(), EventType.UPDATED);
                    }
                }
            }

            // Commit the changes early so that the entity is available for the job engine
            commitChanges(true);
        } catch (Exception e) {
            // If we experience any failure, unlock MC.
            if (mcUnlock != null) {
                log.info("Releasing lock for MC '" + mc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(mcUnlock));
            }
            throw e;
        }

        response.setId(mc.getId());

        Long jobId = ConformService.startMCConformJob(mc, mcUnlock, session).getId();
        response.setJobId(jobId);

        return response;
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

    private void validate(Session session, DryRunRequest<ApplianceManagerConnectorDto> request,
                          ApplianceManagerConnector existingMc, EntityManager<ApplianceManagerConnector> emgr) throws Exception {

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
        if (!request.getDto().getManagerType().equals(existingMc.getManagerType())) {

            throw new VmidcBrokerInvalidRequestException("Cannot change type of Appliance Manager Connector.");
        }

        // check for the deployed appliances by this particular manager
        if (!request.isIgnoreErrorsAndCommit(ErrorType.IP_CHANGED_EXCEPTION)
                && DistributedApplianceEntityMgr.isReferencedByDistributedAppliance(session, existingMc)
                && !existingMc.getIpAddress().equals(request.getDto().getIpAddress())) {

            throw new ErrorTypeException(VmidcMessages.getString(VmidcMessages_.MC_WARNING_IPUPDATE),
                    ErrorType.IP_CHANGED_EXCEPTION);

        }

        // Transforms the existing mc based on the update request
        updateApplianceManagerConnector(request, existingMc);

        AddApplianceManagerConnectorService.checkManagerConnection(log, request, existingMc);
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
