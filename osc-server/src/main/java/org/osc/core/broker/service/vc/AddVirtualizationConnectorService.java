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

import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnConnector;
import org.osc.core.broker.model.virtualization.OpenstackSoftwareVersion;
import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.VimUtils;
import org.osc.core.rest.client.crypto.SslCertificateResolver;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.core.rest.client.exception.RestClientException;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;

import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;

public class AddVirtualizationConnectorService extends ServiceDispatcher<DryRunRequest<VirtualizationConnectorDto>, BaseResponse> {

    private static final Logger log = Logger.getLogger(AddVirtualizationConnectorService.class);

    private boolean forceAddSSLCertificates = false;

    public AddVirtualizationConnectorService() {
    }

    public AddVirtualizationConnectorService(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    @Override
    public BaseResponse exec(DryRunRequest<VirtualizationConnectorDto> request, Session session)
            throws VmidcException, RestClientException, RemoteException, Exception {
        EntityManager<VirtualizationConnector> vcEntityMgr = new EntityManager<>(VirtualizationConnector.class, session);
        try {
            validate(session, request, vcEntityMgr);
        } catch (Exception e) {
            if (e instanceof SslCertificatesExtendedException && this.forceAddSSLCertificates) {
                request = internalSSLCertificatesFetch(request, (SslCertificatesExtendedException) e);
                validate(session, request, vcEntityMgr);
            } else {
                throw e;
            }
        }

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
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

    void validate(Session session, DryRunRequest<VirtualizationConnectorDto> request,
                  EntityManager<VirtualizationConnector> emgr)
            throws VmidcException, RestClientException, RemoteException, Exception {
        VirtualizationConnectorDto dto = request.getDto();

        VirtualizationConnectorDto.checkForNullFields(dto);
        VirtualizationConnectorDto.checkFieldLength(dto);

        // TODO: Future. Right now we assume Icehouse and 5.5 regardless of the actual version passed in, need to
        // fix this later.
        if (dto.getType().isOpenstack()) {
            dto.setSoftwareVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
        } else if (dto.getType().isVmware()) {
            dto.setSoftwareVersion(VmwareSoftwareVersion.VMWARE_V5_5.toString());
        }

        // check for uniqueness of vc name
        if (emgr.isExisting("name", dto.getName())) {

            throw new VmidcBrokerValidationException(
                    "Virtualization Connector Name: " + dto.getName() + " already exists.");
        }

        // check for valid IP address format
        if (!dto.getType().isOpenstack()) {
            ValidateUtil.checkForValidIpAddressFormat(dto.getControllerIP());

            // check for uniqueness of vc nsx IP
            if (emgr.isExisting("controllerIpAddress", dto.getControllerIP())) {

                throw new VmidcBrokerValidationException(
                        "Controller IP Address: " + dto.getControllerIP() + " already exists.");
            }
        }

        VirtualizationConnectorDto.checkFieldFormat(dto);

        // check for uniqueness of vc vCenter IP
        if (emgr.isExisting("providerIpAddress", dto.getProviderIP())) {

            throw new VmidcBrokerValidationException(
                    "Provider IP Address: " + dto.getProviderIP() + " already exists.");
        }

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());

        if (dto.getType().isVmware()) {
            checkVmwareConnection(log, request, vc);
        } else {
            checkOpenstackConnection(log, request, vc);
        }
    }

    /**
     * Checks connection for vmware.
     *
     * @throws ErrorTypeException in case of controller/provider connection issues
     * @throws Exception          in case of any other issues
     */
    public static void checkVmwareConnection(Logger log, DryRunRequest<VirtualizationConnectorDto> request,
                                             VirtualizationConnector vc) throws Exception, ErrorTypeException, SslCertificatesExtendedException {
        if (!request.isSkipAllDryRun()) {
            SslCertificateResolver sslCertificateResolver = new SslCertificateResolver();
            ErrorTypeException errorTypeException = null;
            // Check Connectivity with NSX if rest exception is not to be ignored
            if (!request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                try {
                    VMwareSdnApi vmwareSdnApi = SdnControllerApiFactory.createVMwareSdnApi(vc);
                    vmwareSdnApi.checkStatus(new VMwareSdnConnector(vc));
                } catch (HttpException exception) {
                    if (exception.getSatus() == null && sslCertificateResolver.checkExceptionTypeForSSL(exception)) {
                        URL url = new URL(exception.getResourcePath());
                        sslCertificateResolver.fetchCertificatesFromURL(url, "nsx");
                    }
                    log.warn("Rest Exception encountered when trying to add NSX info to Virtualization Connector, " +
                            "allowing user to either ignore or correct issue.");
                    log.error("Controller exception: " + exception.getMessage());
                    errorTypeException = new ErrorTypeException(exception, ErrorType.CONTROLLER_EXCEPTION);
                }
            }

            // Check Connectivity with vCenter
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                try {
                    new VimUtils(request.getDto().getProviderIP(), request.getDto().getProviderUser(), request.getDto().getProviderPassword());
                } catch (RemoteException remoteException) {
                    if (sslCertificateResolver.checkExceptionTypeForSSL(remoteException)) {
                        sslCertificateResolver.fetchCertificatesFromURL(VimUtils.getServiceURL(request.getDto().getProviderIP()), "vmware");
                    }
                    log.warn("Exception encountered when trying to add vCenter info to Virtualization Connector, " +
                            "allowing user to either ignore or correct issue.");
                    log.error("Provider exception: " + remoteException.getMessage());
                    errorTypeException = new ErrorTypeException(remoteException, ErrorType.PROVIDER_EXCEPTION);
                }
            }

            if (!sslCertificateResolver.getCertificateResolverModels().isEmpty()) {
                throw new SslCertificatesExtendedException(errorTypeException, sslCertificateResolver.getCertificateResolverModels());
            } else if (errorTypeException != null) {
                throw errorTypeException;
            }
        }
    }

    /**
     * Checks connection for openstack.
     *
     * @throws ErrorTypeException in case of keystone/controller/rabbitmq connection issues
     * @throws Exception          in case of any other issues
     */
    public static void checkOpenstackConnection(Logger log, DryRunRequest<VirtualizationConnectorDto> request,
                                                VirtualizationConnector vc) throws Exception, ErrorTypeException, SslCertificatesExtendedException {
        if (!request.isSkipAllDryRun()) {
            SslCertificateResolver sslCertificateResolver = new SslCertificateResolver();
            ErrorTypeException errorTypeException = null;
            if (request.getDto().isControllerDefined() && !request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                try {
                    // Check NSC Connectivity and Credentials
                    try (SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(vc)) {
                        controller.getStatus();
                    }
                } catch (Exception e) {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = VirtualizationConnector.isHttps(vcDto.getProviderAttributes());
                    if (isHttps && StringUtils.isNotEmpty(request.getDto().getControllerIP()) &&
                            sslCertificateResolver.checkExceptionTypeForSSL(e)) {
                        URI uri = new URI("https", request.getDto().getControllerIP(), null, null);
                        sslCertificateResolver.fetchCertificatesFromURL(uri.toURL(), "openstack");
                    }
                    log.warn("Exception encountered when trying to add SDN Controller info to Virtualization Connector, allowing user to either ignore or correct issue");
                    errorTypeException = new ErrorTypeException(e, ErrorType.CONTROLLER_EXCEPTION);
                }
            }
            // Check Connectivity with Key stone if https response exception is not to be ignored
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                JCloudKeyStone keystoneAPi = null;
                try {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = VirtualizationConnector.isHttps(vcDto.getProviderAttributes());
                    Endpoint endPoint = new Endpoint(vcDto.getProviderIP(), vcDto.getAdminTenantName(),
                            vcDto.getProviderUser(), vcDto.getProviderPassword(), isHttps, new SslContextProvider().getSSLContext());

                    keystoneAPi = new JCloudKeyStone(endPoint);
                    keystoneAPi.listTenants();

                } catch (Exception exception) {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = VirtualizationConnector.isHttps(vcDto.getProviderAttributes());
                    if (isHttps && sslCertificateResolver.checkExceptionTypeForSSL(exception)) {
                        URI uri = new URI("https", vcDto.getProviderIP(), null, null);
                        sslCertificateResolver.fetchCertificatesFromURL(uri.toURL(), "openstackkeystone");
                    }
                    log.warn("Exception encountered when trying to add Keystone info to Virtualization Connector, allowing user to either ignore or correct issue");
                    errorTypeException = new ErrorTypeException(exception, ErrorType.PROVIDER_EXCEPTION);
                } finally {
                    if (keystoneAPi != null) {
                        keystoneAPi.close();
                    }
                }
            }

            if (!request.isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION)) {
                OsRabbitMQClient rabbitClient = new OsRabbitMQClient(vc);
                try {
                    rabbitClient.testConnection();
                } catch (ShutdownSignalException shutdownException) {
                    // If its an existing VC which we are connected to, then this exception is expected
                    if (vc.getId() != null) {
                        OsRabbitMQClient osRabbitMQClient = RabbitMQRunner.getVcToRabbitMQClientMap().get(vc.getId());
                        if (osRabbitMQClient != null && osRabbitMQClient.isConnected()) {
                            log.info("Exception encountered when connecting to RabbitMQ, ignoring since we are already connected", shutdownException);
                        } else {
                            errorTypeException = new ErrorTypeException(shutdownException, ErrorType.RABBITMQ_EXCEPTION);
                        }
                    } else {
                        errorTypeException = new ErrorTypeException(shutdownException, ErrorType.RABBITMQ_EXCEPTION);
                    }
                } catch (Throwable e) {
                    if (sslCertificateResolver.checkExceptionTypeForSSL(e)) {
                        URI uri = new URI("https", null, rabbitClient.getServerIP(), rabbitClient.getPort(), null, null, null);
                        sslCertificateResolver.fetchCertificatesFromURL(uri.toURL(), "rabbitmq");
                    }

                    log.warn("Exception encountered when trying to connect to RabbitMQ, allowing user to either ignore or correct issue");
                    errorTypeException = new ErrorTypeException(e, ErrorType.RABBITMQ_EXCEPTION);
                }
            }

            if (!sslCertificateResolver.getCertificateResolverModels().isEmpty()) {
                throw new SslCertificatesExtendedException(errorTypeException, sslCertificateResolver.getCertificateResolverModels());
            } else if (errorTypeException != null) {
                throw errorTypeException;
            }
        }
    }
}
