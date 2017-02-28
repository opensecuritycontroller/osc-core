package org.osc.core.broker.service.mc;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.rest.client.crypto.SslCertificateResolver;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.core.rest.client.exception.RestClientException;

public class AddApplianceManagerConnectorService extends
        ServiceDispatcher<DryRunRequest<ApplianceManagerConnectorDto>, BaseJobResponse> {

    static final Logger log = Logger.getLogger(AddApplianceManagerConnectorService.class);

    private boolean forceAddSSLCertificates = false;

    public AddApplianceManagerConnectorService() {
    }

    public AddApplianceManagerConnectorService(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    @Override
    public BaseJobResponse exec(DryRunRequest<ApplianceManagerConnectorDto> request, Session session)
            throws Exception, RestClientException {
        EntityManager<ApplianceManagerConnector> appMgrEntityMgr = new EntityManager<>(ApplianceManagerConnector.class, session);

        try {
            validate(session, request, appMgrEntityMgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && this.forceAddSSLCertificates) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(session, request, appMgrEntityMgr);
            } else {
                throw e;
            }
        }

        ApplianceManagerConnector mc = null;
        UnlockObjectTask mcUnlock = null;

        try {
            mc = ApplianceManagerConnectorEntityMgr.createEntity(request.getDto());
            mc = appMgrEntityMgr.create(mc);

            SslCertificateAttrEntityMgr certificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
            mc.setSslCertificateAttrSet(certificateAttrEntityMgr.storeSSLEntries(mc.getSslCertificateAttrSet(), mc.getId()));

            appMgrEntityMgr.update(mc);

            // Commit the changes early so that the entity is available for the job engine
            commitChanges(true);
            mcUnlock = LockUtil.tryLockMC(mc, LockType.WRITE_LOCK);
        } catch (Exception e) {
            // If we experience any failure, unlock MC.
            //:TODO dead code - mcUnlock will be always null - release will never happen
            if (mcUnlock != null) {
                log.info("Releasing lock for MC '" + mc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(mcUnlock));
            }
            throw e;
        }

        BaseJobResponse response = new BaseJobResponse();
        response.setId(mc.getId());
        Job job = ConformService.startMCConformJob(mc, mcUnlock, session);
        response.setJobId(job.getId());

        return response;
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

    private void validate(Session session, DryRunRequest<ApplianceManagerConnectorDto> request,
                          EntityManager<ApplianceManagerConnector> emgr) throws Exception, ErrorTypeException {

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

        checkManagerConnection(log, request, ApplianceManagerConnectorEntityMgr.createEntity(request.getDto()));
    }

    /**
     * Checks connection for manager.
     *
     * If thrown exception is instance of SSLException this error will be cached and handled through additional
     * SSL resolver which automatically fetch necessary certificates
     *
     * @throws ErrorTypeException in case of manager connection issues
     */
    public static void checkManagerConnection(Logger log, DryRunRequest<ApplianceManagerConnectorDto> request,
                                              ApplianceManagerConnector mc) throws ErrorTypeException {
        if (!request.isSkipAllDryRun()) {
            SslCertificateResolver sslCertificateResolver = new SslCertificateResolver();
            ErrorTypeException errorTypeException = null;
            try {
                // Check Connectivity
                if (!request.isIgnoreErrorsAndCommit(ErrorType.MANAGER_CONNECTOR_EXCEPTION)) {
                    ManagerApiFactory.checkConnection(mc);
                }
            } catch (Exception e) {
                log.warn("Exception encountered when trying to add Manager Connector, allowing user to either ignore or correct issue");
                if (sslCertificateResolver.checkExceptionTypeForSSL(e)) {
                    try {
                        sslCertificateResolver.fetchCertificatesFromURL(ManagerApiFactory.getConnectionUrl(mc), "manager");
                    } catch (Exception e1) {
                        log.warn("Failed to fetch SSL certificates from requested resource:" + e1.getMessage());
                    }
                }
                errorTypeException = new ErrorTypeException(e, ErrorType.MANAGER_CONNECTOR_EXCEPTION);
            }

            if (!sslCertificateResolver.getCertificateResolverModels().isEmpty()) {
                throw new SslCertificatesExtendedException(errorTypeException, sslCertificateResolver.getCertificateResolverModels());
            } else if (errorTypeException != null) {
                throw errorTypeException;
            }
        }
    }
}