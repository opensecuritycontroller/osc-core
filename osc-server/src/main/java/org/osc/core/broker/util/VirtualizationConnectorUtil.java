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

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import com.rabbitmq.client.ShutdownSignalException;

public class VirtualizationConnectorUtil {

    private static final Logger LOG = Logger.getLogger(VirtualizationConnectorUtil.class);

    private X509TrustManagerFactory managerFactory = null;

    /**
     * Checks connection for openstack.
     *
     * @throws ErrorTypeException in case of keystone/controller/rabbitmq connection issues
     * @throws Exception          in case of any other issues
     */
    public void checkOpenstackConnection(DryRunRequest<VirtualizationConnectorDto> request,
                                         VirtualizationConnector vc) throws Exception {
        if (!request.isSkipAllDryRun()) {

            ErrorTypeException errorTypeException = null;
            final ArrayList<CertificateResolverModel> certificateResolverModels = new ArrayList<>();

            if (this.managerFactory == null) {
                this.managerFactory = X509TrustManagerFactory.getInstance();
            }

            if (request.getDto().isControllerDefined() && !request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                initSSLCertificatesListener(this.managerFactory, certificateResolverModels, "openstack");
                try {
                    // Check NSC Connectivity and Credentials
                    SdnControllerApiFactory.getStatus(vc, null);
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
                                vcDto.getProviderUser(), vcDto.getProviderPassword(), isHttps, SslContextProvider.getInstance().getSSLContext());
                    keystoneAPi = new JCloudKeyStone(endPoint);
                    keystoneAPi.listTenants();

                } catch (Exception exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.PROVIDER_EXCEPTION);
                    LOG.warn("Exception encountered when trying to add Keystone info to Virtualization Connector, allowing user to either ignore or correct issue");
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
                        OsRabbitMQClient osRabbitMQClient = RabbitMQRunner.getVcToRabbitMQClientMap().get(vc.getId());
                        if (osRabbitMQClient != null && osRabbitMQClient.isConnected()) {
                            LOG.info("Exception encountered when connecting to RabbitMQ, ignoring since we are already connected", shutdownException);
                        } else {
                            errorTypeException = new ErrorTypeException(shutdownException, ErrorType.RABBITMQ_EXCEPTION);
                        }
                    } else {
                        errorTypeException = new ErrorTypeException(shutdownException, ErrorType.RABBITMQ_EXCEPTION);
                    }
                } catch (Throwable exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.RABBITMQ_EXCEPTION);
                    LOG.warn("Exception encountered when trying to connect to RabbitMQ, allowing user to either ignore or correct issue");
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
        return attributes.containsKey(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS) &&
                String.valueOf(true).equals(attributes.get(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS));
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