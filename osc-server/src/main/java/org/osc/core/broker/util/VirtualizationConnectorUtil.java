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

import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.rest.client.crypto.SslCertificateExceptionResolver;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class VirtualizationConnectorUtil {

	private static final Logger LOG = Logger.getLogger(VirtualizationConnectorUtil.class);
	 
	 private VimUtils vimUtils = null;
	 private SslCertificateExceptionResolver sslCertificateExceptionResolver = new SslCertificateExceptionResolver();
	 private X509TrustManagerFactory managerFactory = null;
	 private OsRabbitMQClient rabbitClient = null;
	 private Endpoint endPoint = null; 
	 private JCloudKeyStone keystoneAPi = null;
	 
	 public void setVimUtils(VimUtils vimUtils) {
			this.vimUtils = vimUtils;
		}

	 /**
     * Checks connection for vmware.
     *
     * @throws ErrorTypeException in case of controller/provider connection issues
     * @throws Exception          in case of any other issues
     */
    public void checkVmwareConnection(DryRunRequest<VirtualizationConnectorDto> request,
                                             VirtualizationConnector vc) throws Exception {
        if (!request.isSkipAllDryRun()) {

        	ErrorTypeException errorTypeException = null;
            final ArrayList<CertificateResolverModel> certificateResolverModels = new ArrayList<>();

            // Check Connectivity with NSX if rest exception is not to be ignored

            if(this.managerFactory == null){
                this.managerFactory = X509TrustManagerFactory.getInstance();
            }

            if (!request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                try {
                    VMwareSdnApi vmwareSdnApi = SdnControllerApiFactory.createVMwareSdnApi(vc);
                    vmwareSdnApi.checkStatus(new VMwareSdnConnector(vc));
                } catch (HttpException exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.CONTROLLER_EXCEPTION);
                    if(exception.getSatus() == null){
                        List<CertificateResolverModel> connectionCertificates = this.managerFactory.getConnectionCertificates();
                        connectionCertificates.forEach(model -> {
                            model.setAlias("nsx_" + model.getAlias());
                            certificateResolverModels.add(model);
                        });
                    }
                    LOG.warn("Rest Exception encountered when trying to add NSX info to Virtualization Connector, " +
                            "allowing user to either ignore or correct issue.");
                }
            }
            
            // Check Connectivity with vCenter
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                try {
                    if(this.vimUtils == null) {
                        this.vimUtils = new VimUtils(request.getDto().getProviderIP(), request.getDto().getProviderUser(), request.getDto().getProviderPassword());
                    }
                } catch (RemoteException remoteException) {
                    errorTypeException = new ErrorTypeException(remoteException, ErrorType.PROVIDER_EXCEPTION);
                    List<CertificateResolverModel> connectionCertificates = this.managerFactory.getConnectionCertificates();
                    connectionCertificates.forEach(model -> {
                        model.setAlias("vmware_" + model.getAlias());
                        certificateResolverModels.add(model);
                    });
                    LOG.warn("Exception encountered when trying to add vCenter info to Virtualization Connector, " +
                            "allowing user to either ignore or correct issue.");
                }
            }

            if (certificateResolverModels.size() > 0) {
                throw new SslCertificatesExtendedException(errorTypeException, certificateResolverModels);
            } else if(errorTypeException != null){
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
    public void checkOpenstackConnection(DryRunRequest<VirtualizationConnectorDto> request,
                                                VirtualizationConnector vc) throws Exception {
        if (!request.isSkipAllDryRun()) {

        	ErrorTypeException errorTypeException = null;
            final ArrayList<CertificateResolverModel> certificateResolverModels = new ArrayList<>();

            if(this.managerFactory == null){
                this.managerFactory = X509TrustManagerFactory.getInstance();
            }

            if (request.getDto().isControllerDefined() && !request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                try {
                    // Check NSC Connectivity and Credentials
                    try (SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(vc)) {
                        controller.getStatus();
                    }
                } catch (Exception exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.CONTROLLER_EXCEPTION);
                    if (this.sslCertificateExceptionResolver.checkExceptionTypeForSSL(exception) && StringUtils.isNotEmpty(request.getDto().getControllerIP())) {
                        List<CertificateResolverModel> connectionCertificates = this.managerFactory.getConnectionCertificates();
                        connectionCertificates.forEach(model -> {
                            model.setAlias("openstack_" + model.getAlias());
                            certificateResolverModels.add(model);
                        });
                    }
                    LOG.warn("Exception encountered when trying to add SDN Controller info to Virtualization Connector, allowing user to either ignore or correct issue");
                }
            }
            // Check Connectivity with Key stone if https response exception is not to be ignored
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                
                try {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = VirtualizationConnector.isHttps(vcDto.getProviderAttributes());
                    
                    if(this.endPoint == null) {
                        this.endPoint = new Endpoint(vcDto.getProviderIP(), vcDto.getAdminTenantName(),
                            vcDto.getProviderUser(), vcDto.getProviderPassword(), isHttps, new SslContextProvider().getSSLContext());
                    }
                    if(this.keystoneAPi == null) {
                        this.keystoneAPi = new JCloudKeyStone(this.endPoint);
                    }
                    this.keystoneAPi.listTenants();

                } catch (Exception exception) {
                    errorTypeException = new ErrorTypeException(exception, ErrorType.PROVIDER_EXCEPTION);
                    if (this.sslCertificateExceptionResolver.checkExceptionTypeForSSL(exception)) {
                        List<CertificateResolverModel> connectionCertificates = this.managerFactory.getConnectionCertificates();
                        connectionCertificates.forEach(model -> {
                            model.setAlias("openstackkeystone_" + model.getAlias());
                            certificateResolverModels.add(model);
                        });
                    }
                    LOG.warn("Exception encountered when trying to add Keystone info to Virtualization Connector, allowing user to either ignore or correct issue");
                } finally {
                    if (this.keystoneAPi != null) {
                        this.keystoneAPi.close();
                    }
                }
            }

            if (!request.isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION)) {
                 
                if(this.rabbitClient == null ) {
                    this.rabbitClient = new OsRabbitMQClient(vc);
                }
                try {
                    this.rabbitClient.testConnection();
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
                    if(this.sslCertificateExceptionResolver.checkExceptionTypeForSSL(exception)){
                        List<CertificateResolverModel> connectionCertificates = this.managerFactory.getConnectionCertificates();
                        connectionCertificates.forEach(model -> {
                            model.setAlias("rabbitmq_" + model.getAlias());
                            certificateResolverModels.add(model);
                        });
                    }
                    LOG.warn("Exception encountered when trying to connect to RabbitMQ, allowing user to either ignore or correct issue");
                }
            }

            if (!certificateResolverModels.isEmpty()) {
                throw new SslCertificatesExtendedException(errorTypeException, certificateResolverModels);
            } else if (errorTypeException != null) {
                throw errorTypeException;
            }
        }
    }

}