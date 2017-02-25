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

import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;

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
import org.osc.core.rest.client.crypto.SslCertificateResolver;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;

import com.rabbitmq.client.ShutdownSignalException;

public class VirtualizationConnectorUtil {

	private static final Logger LOG = Logger.getLogger(VirtualizationConnectorUtil.class);
	 
	 private VimUtils vimUtils = null;
	 private SslCertificateResolver sslCertificateResolver = null;
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
                                             VirtualizationConnector vc) throws Exception, ErrorTypeException, SslCertificatesExtendedException {
        if (!request.isSkipAllDryRun()) {
            
        	if(sslCertificateResolver == null) {
        		sslCertificateResolver = new SslCertificateResolver();                
        	}
        	
        	ErrorTypeException errorTypeException = null;
            // Check Connectivity with NSX if rest exception is not to be ignored
            if (!request.isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION)) {
                try {
                    VMwareSdnApi vmwareSdnApi = SdnControllerApiFactory.createVMwareSdnApi(vc);
                    vmwareSdnApi.checkStatus(new VMwareSdnConnector(vc));
                } catch (HttpException exception) {
                	if(exception.getSatus() == null){
                        URL url = new URL(exception.getResourcePath());
                		sslCertificateResolver.fetchCertificatesFromURL(url, "nsx");
                	}
                    LOG.warn("Rest Exception encountered when trying to add NSX info to Virtualization Connector, " +
                            "allowing user to either ignore or correct issue.");
                    errorTypeException = new ErrorTypeException(exception, ErrorType.CONTROLLER_EXCEPTION);
                }
            }
            
            // Check Connectivity with vCenter
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                try {
                    if(vimUtils == null) {
                    	vimUtils = new VimUtils(request.getDto().getProviderIP(), request.getDto().getProviderUser(), request.getDto().getProviderPassword());
                    }
                } catch (RemoteException remoteException) {
                    sslCertificateResolver.fetchCertificatesFromURL(VimUtils.getServiceURL(request.getDto().getProviderIP()), "vmware");
                    LOG.warn("Exception encountered when trying to add vCenter info to Virtualization Connector, " +
                            "allowing user to either ignore or correct issue.");
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
    public void checkOpenstackConnection(DryRunRequest<VirtualizationConnectorDto> request,
                                                VirtualizationConnector vc) throws Exception, ErrorTypeException, SslCertificatesExtendedException {
        if (!request.isSkipAllDryRun()) {
        	if(sslCertificateResolver == null) {
        		sslCertificateResolver = new SslCertificateResolver();                
        	}
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
                    if (isHttps && StringUtils.isNotEmpty(request.getDto().getControllerIP())) {
                        URI uri = new URI("https", request.getDto().getControllerIP(), null, null);
                        sslCertificateResolver.fetchCertificatesFromURL(uri.toURL(), "openstack");
                    }
                    LOG.warn("Exception encountered when trying to add SDN Controller info to Virtualization Connector, allowing user to either ignore or correct issue");
                    errorTypeException = new ErrorTypeException(e, ErrorType.CONTROLLER_EXCEPTION);
                }
            }
            // Check Connectivity with Key stone if https response exception is not to be ignored
            if (!request.isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION)) {
                
                try {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = VirtualizationConnector.isHttps(vcDto.getProviderAttributes());
                    
                    if(endPoint == null) {
                    	endPoint = new Endpoint(vcDto.getProviderIP(), vcDto.getAdminTenantName(),
                    
                            vcDto.getProviderUser(), vcDto.getProviderPassword(), isHttps, new SslContextProvider().getSSLContext());
                    }
                    if(keystoneAPi == null) {
                    	keystoneAPi = new JCloudKeyStone(endPoint);
                    }
                    keystoneAPi.listTenants();

                } catch (Exception exception) {
                    VirtualizationConnectorDto vcDto = request.getDto();
                    boolean isHttps = VirtualizationConnector.isHttps(vcDto.getProviderAttributes());
                    if (isHttps) {
                        URI uri = new URI("https", vcDto.getProviderIP(), null, null);
                        sslCertificateResolver.fetchCertificatesFromURL(uri.toURL(), "openstackkeystone");
                    }
                    LOG.warn("Exception encountered when trying to add Keystone info to Virtualization Connector, allowing user to either ignore or correct issue");
                    errorTypeException = new ErrorTypeException(exception, ErrorType.PROVIDER_EXCEPTION);
                } finally {
                    if (keystoneAPi != null) {
                        keystoneAPi.close();
                    }
                }
            }

            if (!request.isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION)) {
                 
                if( rabbitClient == null ) {
                	rabbitClient = new OsRabbitMQClient(vc);
                }
                try {
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
                } catch (Throwable e) {
                    URI uri = new URI("https", null, rabbitClient.getServerIP(), rabbitClient.getPort(), null, null, null);
                    sslCertificateResolver.fetchCertificatesFromURL(uri.toURL(), "rabbitmq");
                    LOG.warn("Exception encountered when trying to connect to RabbitMQ, allowing user to either ignore or correct issue");
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