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
package org.osc.core.broker.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.rabbitmq.client.ShutdownSignalException;

@Component(service = VirtualizationConnectorUtil.class)
public class VirtualizationConnectorUtil {

    private static final Logger LOG = Logger.getLogger(VirtualizationConnectorUtil.class);

    private X509TrustManagerFactory managerFactory = null;

    @Reference
    private ApiFactoryService apiFactoryService;

    // target ensures this only binds to active runner published by Server
    @Reference(target = "(active=true)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ComponentServiceObjects<RabbitMQRunner> activeRunnerCSO;
    RabbitMQRunner activeRunner;

    private final AtomicBoolean initDone = new AtomicBoolean();

    @Deactivate
    private void deactivate() {
        if (this.initDone.get()) {
            this.activeRunnerCSO.ungetService(this.activeRunner);
        }
    }

    private void delayedInit() {
        if (this.initDone.compareAndSet(false, true)) {
            if (this.activeRunnerCSO != null) { // allow test injection
                this.activeRunner = this.activeRunnerCSO.getService();
            }
        }
    }

    /**
     * Checks connection for vmware.
     *
     * @throws ErrorTypeException
     *             in case of controller/provider connection issues
     * @throws Exception
     *             in case of any other issues
     */
    public <T extends VirtualizationConnectorDto> void checkVmwareConnection(DryRunRequest<T> request,
            VirtualizationConnector vc) throws Exception {
        if (!request.isSkipAllDryRun()) {

            ErrorTypeException errorTypeException = null;
            final ArrayList<CertificateResolverModel> certificateResolverModels = new ArrayList<>();

            // Check Connectivity with NSX if rest exception is not to be ignored

            if (this.managerFactory == null) {
                this.managerFactory = X509TrustManagerFactory.getInstance();
            }

            if (!request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                initSSLCertificatesListener(this.managerFactory, certificateResolverModels, "nsx");
                try {
                    VMwareSdnApi vmwareSdnApi = this.apiFactoryService.createVMwareSdnApi(vc);
                    vmwareSdnApi.checkStatus(new VMwareSdnConnector(vc));
                } catch (HttpException exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.CONTROLLER_EXCEPTION);
                    LOG.warn("Rest Exception encountered when trying to add NSX info to Virtualization Connector, "
                            + "allowing user to either ignore or correct issue.");
                }
            }

            // Check Connectivity with vCenter
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                initSSLCertificatesListener(this.managerFactory, certificateResolverModels, "vmware");
                try {
                    new VimUtils(request.getDto().getProviderIP(), request.getDto().getProviderUser(),
                            request.getDto().getProviderPassword());
                } catch (RemoteException remoteException) {
                    errorTypeException = new ErrorTypeException(remoteException, ErrorType.PROVIDER_EXCEPTION);
                    LOG.warn("Exception encountered when trying to add vCenter info to Virtualization Connector, "
                            + "allowing user to either ignore or correct issue.");
                }
            }

            this.managerFactory.clearListener();

            if (certificateResolverModels.size() > 0) {
                throw new SslCertificatesExtendedException(errorTypeException, certificateResolverModels);
            } else if (errorTypeException != null) {
                throw errorTypeException;
            }
        }
    }

    /**
     * Checks connection for openstack.
     *
     * @throws ErrorTypeException
     *             in case of keystone/controller/rabbitmq connection issues
     * @throws Exception
     *             in case of any other issues
     */
    public <T extends VirtualizationConnectorDto> void checkOpenstackConnection(DryRunRequest<T> request,
            VirtualizationConnector vc) throws Exception {
        if (!request.isSkipAllDryRun()) {

            ErrorTypeException errorTypeException = null;
            final ArrayList<CertificateResolverModel> certificateResolverModels = new ArrayList<>();

            if (this.managerFactory == null) {
                this.managerFactory = X509TrustManagerFactory.getInstance();
            }

            if (request.getDto().isControllerDefined()
                    && !request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                initSSLCertificatesListener(this.managerFactory, certificateResolverModels, "openstack");
                try {
                    // Check NSC Connectivity and Credentials
                    this.apiFactoryService.getStatus(vc, null);
                } catch (Exception exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.CONTROLLER_EXCEPTION);
                    LOG.warn(
                            "Exception encountered when trying to add SDN Controller info to Virtualization Connector, allowing user to either ignore or correct issue");
                }
            }
            // Check Connectivity with Key stone if https response exception is not to be ignored
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                initSSLCertificatesListener(this.managerFactory, certificateResolverModels, "openstackkeystone");
                JCloudKeyStone keystoneAPi = null;
                try {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = isHttps(vcDto.getProviderAttributes());

                    Endpoint endPoint = new Endpoint(vcDto.getProviderIP(), vcDto.getAdminTenantName(),
                            vcDto.getProviderUser(), vcDto.getProviderPassword(), isHttps,
                            SslContextProvider.getInstance().getSSLContext());
                    keystoneAPi = new JCloudKeyStone(endPoint);
                    keystoneAPi.listTenants();

                } catch (Exception exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.PROVIDER_EXCEPTION);
                    LOG.warn(
                            "Exception encountered when trying to add Keystone info to Virtualization Connector, allowing user to either ignore or correct issue");
                } finally {
                    if (keystoneAPi != null) {
                        keystoneAPi.close();
                    }
                }
            }

            if (!request.isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION)) {
                initSSLCertificatesListener(this.managerFactory, certificateResolverModels, "rabbitmq");
                try {
                    OsRabbitMQClient rabbitClient = new OsRabbitMQClient(vc);
                    rabbitClient.testConnection();
                } catch (ShutdownSignalException shutdownException) {
                    // If its an existing VC which we are connected to, then this exception is expected
                    if (vc.getId() != null) {
                        delayedInit();
                        OsRabbitMQClient osRabbitMQClient = this.activeRunner.getVcToRabbitMQClientMap().get(vc.getId());
                        if (osRabbitMQClient != null && osRabbitMQClient.isConnected()) {
                            LOG.info(
                                    "Exception encountered when connecting to RabbitMQ, ignoring since we are already connected",
                                    shutdownException);
                        } else {
                            errorTypeException = new ErrorTypeException(shutdownException,
                                    ErrorType.RABBITMQ_EXCEPTION);
                        }
                    } else {
                        errorTypeException = new ErrorTypeException(shutdownException, ErrorType.RABBITMQ_EXCEPTION);
                    }
                } catch (Throwable exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.RABBITMQ_EXCEPTION);
                    LOG.warn(
                            "Exception encountered when trying to connect to RabbitMQ, allowing user to either ignore or correct issue");
                }
            }

            this.managerFactory.clearListener();

            if (!certificateResolverModels.isEmpty()) {
                throw new SslCertificatesExtendedException(errorTypeException, certificateResolverModels);
            } else if (errorTypeException != null) {
                throw errorTypeException;
            }
        }
    }

    private static boolean isHttps(Map<String, String> attributes) {
        return attributes.containsKey(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS)
                && String.valueOf(true).equals(attributes.get(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS));
    }

    private void initSSLCertificatesListener(X509TrustManagerFactory managerFactory,
            ArrayList<CertificateResolverModel> resolverList, String aliasPrefix) {
        try {
            managerFactory.setListener(model -> {
                model.setAlias(aliasPrefix + "_" + model.getAlias());
                resolverList.add(model);
            });
        } catch (Exception e1) {
            LOG.error("Error occurred in TrustStoreManagerFactory", e1);
        }
    }

}
