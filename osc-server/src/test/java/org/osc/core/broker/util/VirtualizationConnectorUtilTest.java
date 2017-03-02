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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.osc.core.rest.client.crypto.SslCertificateResolver;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.rabbitmq.client.ShutdownSignalException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SdnControllerApiFactory.class, RabbitMQRunner.class, EncryptionUtil.class })
@PowerMockIgnore("javax.net.ssl.*")
public class VirtualizationConnectorUtilTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	SslCertificateResolver sslCertificateResolver;

	@Mock
	OsRabbitMQClient rabbitClient;

	@InjectMocks
	VirtualizationConnectorUtil util;

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);

		  PowerMockito.mockStatic(EncryptionUtil.class);
	      when(EncryptionUtil.encryptAESCTR(any(String.class))).thenReturn("Encrypted Passowrd");
	      when(EncryptionUtil.decryptAESCTR(any(String.class))).thenReturn("Decrypted Passowrd");
	}

	@Test
	public void testVmwareConnection_WithSkipDryRunRequest_ReturnsSuccessful() throws Exception {

		// Arrange.
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		request.setSkipAllDryRun(true);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = mock(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(vc)).thenReturn(vmwareSdnApi);
		doNothing().when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doReturn(new ArrayList<>()).when(this.sslCertificateResolver).getCertificateResolverModels();

		// Act.
		this.util.checkVmwareConnection(spyRequest, vc);

		// Assert
		verify(vmwareSdnApi, times(0)).checkStatus( any(VMwareSdnConnector.class));
		verify(this.sslCertificateResolver, times(0)).fetchCertificatesFromURL(any(URL.class), any(String.class));
		verify(this.sslCertificateResolver, times(0)).getCertificateResolverModels();
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);

	}

	@Test
	public void testOpenstackConnection_WithSkipDryRunRequest_ReturnsSuccessful() throws Exception {

		// Arrange.
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST;
		request.setSkipAllDryRun(true);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		//Act
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);

	}

	@Test
	public void testVmwareConnection_WithIgnoreProviderException_WhenProviderCheckStatusSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = mock(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(spyVc)).thenReturn(vmwareSdnApi);
		doNothing().when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(vmwareSdnApi, times(1)).checkStatus( any(VMwareSdnConnector.class));
	}

	@Test
	public void testVmwareConnection_WithIgnoreProviderException_WhenProviderCheckStatusFail_throwsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = spy(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(spyVc)).thenReturn(vmwareSdnApi);
		HttpException httpException = new HttpException(10, null, null, null, null);
		doThrow(httpException).when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));

		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(vmwareSdnApi, times(1)).checkStatus( any(VMwareSdnConnector.class));
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testVmwareConnection_WithIgnoreProviderException_WhenProviderCheckStatusFail_WhenFetchCertificatesFromUrl_throwsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = spy(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(spyVc)).thenReturn(vmwareSdnApi);
		HttpException httpException = new HttpException(null, null, "http://www.osctest.com", null, null);
		doThrow(httpException).when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(vmwareSdnApi, times(1)).checkStatus( any(VMwareSdnConnector.class));
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}


	@Test
	public void testVmwareConnection_WithIgnoreControllerException_WhenVCenterConnectionFail_ReturnsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.whenNew(VimUtils.class).withAnyArguments().thenThrow(new RemoteException());
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doReturn(new ArrayList<>()).when(this.sslCertificateResolver).getCertificateResolverModels();


		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(this.sslCertificateResolver, times(1)).getCertificateResolverModels();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testVmwareConnection_WithIgnoreControllerException_WhenVCenterConnectionSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		VirtualizationConnector spyVc = spy(vc);
		VimUtils utils = mock(VimUtils.class);
		this.util.setVimUtils(utils);
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doReturn(new ArrayList<>()).when(this.sslCertificateResolver).getCertificateResolverModels();


		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(this.sslCertificateResolver, times(1)).getCertificateResolverModels();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}


	@Test
	public void testOpenStackConnection_WithIgnoreProviderException_WithIgnoreRabbitMqException_WhenSdnControllerStatusSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		SdnControllerApi sdnController = mock(SdnControllerApi.class);
		when(SdnControllerApiFactory.createNetworkControllerApi(vc)).thenReturn(sdnController);
		doReturn(null).when(sdnController).getStatus();

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(sdnController, times(1)).getStatus();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testOpenStackConnection_WithIgnoreProviderException_WithIgnoreRabbitMqException_WhenSdnControllerStatusFail_ReturnsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		SdnControllerApi sdnController = mock(SdnControllerApi.class);
		when(SdnControllerApiFactory.createNetworkControllerApi(vc)).thenReturn(sdnController);

		doThrow(new Exception()).when(sdnController).getStatus();
		request.getDto().getProviderAttributes().putIfAbsent(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, "true");
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(sdnController, times(1)).getStatus();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));

	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreRabbitMqException_WhenKeyStoneListTenantsFail_ReturnsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		request.getDto().getProviderAttributes().putIfAbsent(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, "true");
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreRabbitMqException_WhenKeyStoneListTenantsSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		JCloudKeyStone cloudKeyStone = mock(JCloudKeyStone.class);
		when(cloudKeyStone.listTenants()).thenReturn(null);
		request.getDto().getProviderAttributes().putIfAbsent(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, "true");
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doNothing().when(cloudKeyStone).close();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
		verify(cloudKeyStone, times(1)).listTenants();
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionSuccess_ReturnsSuccessful() throws Throwable {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		doNothing().when(this.rabbitClient).testConnection();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();

	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionFail_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());

		doThrow(new Exception()).when(this.rabbitClient).testConnection();
		when(this.rabbitClient.getServerIP()).thenReturn("www.osctest.com");
		when(this.rabbitClient.getPort()).thenReturn(80);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionThrowsSignalException_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		doThrow(mock(ShutdownSignalException.class)).when(this.rabbitClient).testConnection();
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionThrowsSignalException_WhenMqClientIsNotConnected_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		request.getDto().setId(20l);
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());

		doThrow(mock(ShutdownSignalException.class)).when(this.rabbitClient).testConnection();
		doNothing().when(this.sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
		verify(this.sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionThrowsSignalException_WhenMqClientIsConnected_ReturnsSuccessful() throws Throwable {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		PowerMockito.mockStatic(RabbitMQRunner.class);

		HashMap<Long, OsRabbitMQClient> map = mock(HashMap.class);
		when(RabbitMQRunner.getVcToRabbitMQClientMap()).thenReturn(map);
		OsRabbitMQClient mqClient = mock(OsRabbitMQClient.class);
		doReturn(mqClient).when(map).get(any(Integer.class));
		doReturn(true).when(mqClient).isConnected();

		request.getDto().setId(20l);
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		doThrow(mock(ShutdownSignalException.class)).when(this.rabbitClient).testConnection();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
	}

}