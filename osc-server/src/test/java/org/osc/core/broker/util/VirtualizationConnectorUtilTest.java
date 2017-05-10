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
import static org.mockito.Mockito.*;

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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.server.Server;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.rabbitmq.client.ShutdownSignalException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SdnControllerApiFactory.class, StaticRegistry.class,
        VirtualizationConnectorUtil.class, X509TrustManagerFactory.class, SslContextProvider.class})
@PowerMockIgnore("javax.net.ssl.*")
public class VirtualizationConnectorUtilTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private OsRabbitMQClient rabbitClient;

    @Mock
    private X509TrustManagerFactory trustManagerFactory;

    @Mock
    private EncryptionApi encrypter;

    @Mock
    private SslContextProvider contextProvider;

    @InjectMocks
    private VirtualizationConnectorUtil util;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(this.encrypter.encryptAESCTR(any(String.class))).thenReturn("Encrypted Passowrd");
        when(this.encrypter.decryptAESCTR(any(String.class))).thenReturn("Decrypted Passowrd");

        PowerMockito.mockStatic(StaticRegistry.class);
        when(StaticRegistry.encryptionApi()).thenReturn(this.encrypter);

        when(this.contextProvider.getSSLContext()).thenReturn(null);
        PowerMockito.whenNew(SslContextProvider.class).withAnyArguments().thenReturn(this.contextProvider);
    }

	@Test
	public void testVmwareConnection_WithSkipDryRunRequest_ReturnsSuccessful() throws Exception {

		// Arrange.
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		request.setSkipAllDryRun(true);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = mock(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(vc)).thenReturn(vmwareSdnApi);
		doNothing().when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));

		// Act.
		this.util.checkVmwareConnection(spyRequest, vc);

		// Assert
		verify(vmwareSdnApi, times(0)).checkStatus(any(VMwareSdnConnector.class));
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);

	}

	@Test
	public void testOpenstackConnection_WithSkipDryRunRequest_ReturnsSuccessful() throws Exception {

		// Arrange.
		DryRunRequest<VirtualizationConnectorRequest> request = VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST;
		request.setSkipAllDryRun(true);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		DryRunRequest<VirtualizationConnectorRequest> spyRequest = spy(request);

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

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
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
		verify(vmwareSdnApi, times(1)).checkStatus(any(VMwareSdnConnector.class));
	}

	@Test
	public void testVmwareConnection_WithIgnoreProviderException_WhenProviderCheckStatusFail_throwsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = spy(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(spyVc)).thenReturn(vmwareSdnApi);
		HttpException httpException = new HttpException(10, null, null, null, null);
		doThrow(httpException).when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));

		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(vmwareSdnApi, times(1)).checkStatus(any(VMwareSdnConnector.class));
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testVmwareConnection_WithIgnoreProviderException_WhenProviderCheckStatusFail_WhenFetchCertificatesFromUrl_throwsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = spy(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(spyVc)).thenReturn(vmwareSdnApi);
		HttpException httpException = new HttpException(null, null, "http://www.osctest.com", null, null);
		doThrow(httpException).when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));

		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(vmwareSdnApi, times(1)).checkStatus(any(VMwareSdnConnector.class));
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}


	@Test
	public void testVmwareConnection_WithIgnoreControllerException_WhenVCenterConnectionFail_ReturnsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		VirtualizationConnector spyVc = spy(vc);
		PowerMockito.whenNew(VimUtils.class).withAnyArguments().thenThrow(new RemoteException());


		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testVmwareConnection_WithIgnoreControllerException_WhenVCenterConnectionSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		VirtualizationConnector spyVc = spy(vc);
		VimUtils utils = mock(VimUtils.class);
		PowerMockito.whenNew(VimUtils.class).withAnyArguments().thenReturn(utils);

		// Act.
		this.util.checkVmwareConnection(spyRequest, spyVc);

		//Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testOpenStackConnection_WithIgnoreProviderException_WithIgnoreRabbitMqException_WhenSdnControllerStatusSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		when(SdnControllerApiFactory.getStatus(vc, null)).thenReturn(null);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
        PowerMockito.verifyStatic(times(1));
        SdnControllerApiFactory.getStatus(vc, null);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testOpenStackConnection_WithIgnoreProviderException_WithIgnoreRabbitMqException_WhenSdnControllerStatusFail_ReturnsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
        when(SdnControllerApiFactory.getStatus(vc, null)).thenThrow(new Exception());

		request.getDto().getProviderAttributes().putIfAbsent(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, "true");

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		PowerMockito.verifyStatic(times(1));
        SdnControllerApiFactory.getStatus(vc, null);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);

	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreRabbitMqException_WhenKeyStoneListTenantsFail_ReturnsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		request.getDto().getProviderAttributes().putIfAbsent(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, "true");

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreRabbitMqException_WhenKeyStoneListTenantsSuccess_ReturnsSuccessful() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.RABBITMQ_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		JCloudKeyStone cloudKeyStone = mock(JCloudKeyStone.class);
		when(cloudKeyStone.listTenants()).thenReturn(null);
		request.getDto().getProviderAttributes().putIfAbsent(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, "true");
		doNothing().when(cloudKeyStone).close();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
		verify(cloudKeyStone, times(1)).listTenants();
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionSuccess_ReturnsSuccessful() throws Throwable {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
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

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);

		doThrow(new Exception()).when(this.rabbitClient).testConnection();
		when(this.rabbitClient.getServerIP()).thenReturn("www.osctest.com");
		when(this.rabbitClient.getPort()).thenReturn(80);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionThrowsSignalException_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
		doThrow(mock(ShutdownSignalException.class)).when(this.rabbitClient).testConnection();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionThrowsSignalException_WhenMqClientIsNotConnected_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		request.getDto().setId(20l);
		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		PowerMockito.mockStatic(StaticRegistry.class);

        Server server = Mockito.mock(Server.class);
        RabbitMQRunner runner = Mockito.mock(RabbitMQRunner.class);

        @SuppressWarnings("unchecked")
        HashMap<Long, OsRabbitMQClient> map = mock(HashMap.class);
        when(StaticRegistry.server()).thenReturn(server);
        when(server.getActiveRabbitMQRunner()).thenReturn(runner);
        when(runner.getVcToRabbitMQClientMap()).thenReturn(map);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);

		doThrow(mock(ShutdownSignalException.class)).when(this.rabbitClient).testConnection();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request);

		// Act.
		this.util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(this.rabbitClient, times(1)).testConnection();
	}

	@Test
	public void testOpenStackConnection_WithIgnoreControllerException_WithIgnoreProviderException_WhenRabbitClientConnectionThrowsSignalException_WhenMqClientIsConnected_ReturnsSuccessful() throws Throwable {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		PowerMockito.mockStatic(StaticRegistry.class);

		Server server = Mockito.mock(Server.class);
		RabbitMQRunner runner = Mockito.mock(RabbitMQRunner.class);

		@SuppressWarnings("unchecked")
        HashMap<Long, OsRabbitMQClient> map = mock(HashMap.class);
		when(StaticRegistry.server()).thenReturn(server);
		when(server.getActiveRabbitMQRunner()).thenReturn(runner);
		when(runner.getVcToRabbitMQClientMap()).thenReturn(map);
		OsRabbitMQClient mqClient = mock(OsRabbitMQClient.class);
		doReturn(mqClient).when(map).get(any(Integer.class));
		doReturn(true).when(mqClient).isConnected();

		request.getDto().setId(20l);
		List<ErrorType> errorList = new ArrayList<>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto(), this.encrypter);
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