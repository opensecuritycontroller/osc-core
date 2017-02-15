package org.osc.core.broker.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.net.URL;
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
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osc.sdk.sdn.exception.HttpException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.rabbitmq.client.ShutdownSignalException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SdnControllerApiFactory.class, RabbitMQRunner.class })
public class VirtualizationConnectorUtilTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	SslCertificateResolver sslCertificateResolver;

	@Mock
	OsRabbitMQClient rabbitClient;

	@Mock
	VimUtils vimUtils;
	
	@InjectMocks
	VirtualizationConnectorUtil util;

	@Before
	public void testInitialize() {
		MockitoAnnotations.initMocks(this);

	}
	
	@Test
	public void testVmwareConnection_WhenSkipDryRunRequest_ReturnsSuccessful() throws Exception {

		// Arrange.
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		request.setSkipAllDryRun(true);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = mock(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(vc)).thenReturn(vmwareSdnApi);
		doNothing().when(vmwareSdnApi).checkStatus(new VMwareSdnConnector(vc));
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doReturn(new ArrayList()).when(sslCertificateResolver).getCertificateResolverModels();
		
		// Act.
		util.checkVmwareConnection(spyRequest, vc);

		// Assert
		verify(vmwareSdnApi, times(0)).checkStatus( any(VMwareSdnConnector.class));
		verify(sslCertificateResolver, times(0)).fetchCertificatesFromURL(any(URL.class), any(String.class));
		verify(sslCertificateResolver, times(0)).getCertificateResolverModels();
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	
	}

	@Test
	public void testOpenstackConnection_WhenSkipDryRunRequest_ReturnsSuccessful() throws Exception {

		// Arrange.
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST;
		request.setSkipAllDryRun(true);
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		
		//Act
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(spyRequest, times(0)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
		
	}

	@Test
	public void testVmwareConnection_WhenProviderRequest_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = mock(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(vc)).thenReturn(vmwareSdnApi);
		doNothing().when(vmwareSdnApi).checkStatus(new VMwareSdnConnector(vc));

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		
		// Act.
		util.checkVmwareConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(vmwareSdnApi, times(1)).checkStatus( any(VMwareSdnConnector.class));
	}

	@Test
	public void testVmwareConnection_WithVmwareSDNconnectionfailure_throwsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = spy(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(vc)).thenReturn(vmwareSdnApi);
		HttpException httpException = new HttpException(10, null, null, null, null);
		doThrow(httpException).when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));

		// Act.
		util.checkVmwareConnection(spyRequest, vc);
		
		//Assert
		verify(vmwareSdnApi, times(1)).checkStatus( any(VMwareSdnConnector.class));
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testVmwareConnection_WithVmwareControllerRequest_throwsErrorTypeException() throws Exception {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		PowerMockito.mockStatic(SdnControllerApiFactory.class);
		VMwareSdnApi vmwareSdnApi = spy(VMwareSdnApi.class);
		when(SdnControllerApiFactory.createVMwareSdnApi(vc)).thenReturn(vmwareSdnApi);
		HttpException httpException = new HttpException(null, null, "http://www.osctest.com", null, null);
		doThrow(httpException).when(vmwareSdnApi).checkStatus(any(VMwareSdnConnector.class));
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		util.checkVmwareConnection(spyRequest, vc);
		
		//Assert
		verify(vmwareSdnApi, times(1)).checkStatus( any(VMwareSdnConnector.class));
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testVmwareConnection_WithVmwareProviderRequest_ReturnsSuccessful() throws Exception {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData.getVmwareRequest();
		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		VimUtils utils = mock(VimUtils.class);
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doReturn(new ArrayList()).when(sslCertificateResolver).getCertificateResolverModels();
		

		// Act.
		util.checkVmwareConnection(spyRequest, vc);
		
		//Assert
		verify(sslCertificateResolver, times(1)).getCertificateResolverModels();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testOpenStackConnection_WithControllerRequest_ReturnsSuccessful() throws Exception {

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
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(sdnController, times(1)).getStatus();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
	}

	@Test
	public void testOpenStackConnection_WithControllerRequest_ReturnsErrorTypeException() throws Exception {

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
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		
		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(sdnController, times(1)).getStatus();
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));

	}

	@Test
	public void testOpenStackConnection_WithProviderRequest_ReturnsSuccessful() throws Exception {

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
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WithHttpsProviderRequest_ReturnsSuccessful2() throws Exception {

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
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		doNothing().when(cloudKeyStone).close();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.RABBITMQ_EXCEPTION);
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
		verify(cloudKeyStone, times(1)).listTenants();
	}

	@Test
	public void testOpenStackConnection_WhenRabbitClientRequest_ReturnsSuccessful() throws Throwable {

		// Arrange
		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		doNothing().when(rabbitClient).testConnection();
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(rabbitClient, times(1)).testConnection();

	}

	@Test
	public void testOpenStackConnection_WhenRequest_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());

		doThrow(new Exception()).when(rabbitClient).testConnection();
		when(rabbitClient.getServerIP()).thenReturn("www.osctest.com");
		when(rabbitClient.getPort()).thenReturn(80);

		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(rabbitClient, times(1)).testConnection();
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WhenRabbitClientRequest_ThrowsErrorTypeException() throws Throwable {

		// Arrange
		this.exception.expect(ErrorTypeException.class);

		DryRunRequest<VirtualizationConnectorDto> request = VirtualizationConnectorServiceData
				.getOpenStackRequestwithSDN();

		List<ErrorType> errorList = new ArrayList<ErrorType>();
		errorList.add(ErrorType.CONTROLLER_EXCEPTION);
		errorList.add(ErrorType.PROVIDER_EXCEPTION);
		request.addErrorsToIgnore(errorList);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(request.getDto());
		doThrow(mock(ShutdownSignalException.class)).when(rabbitClient).testConnection();
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(rabbitClient, times(1)).testConnection();
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WhenRabbitClientRequestwithVcId_ThrowsErrorTypeException() throws Throwable {

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

		doThrow(mock(ShutdownSignalException.class)).when(rabbitClient).testConnection();
		doNothing().when(sslCertificateResolver).fetchCertificatesFromURL(any(URL.class), any(String.class));
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(rabbitClient, times(1)).testConnection();
		verify(sslCertificateResolver, times(1)).fetchCertificatesFromURL(any(URL.class), any(String.class));
	}

	@Test
	public void testOpenStackConnection_WhenRequestwithShutdownSignalException_ReturnsSuccessful() throws Throwable {

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
		doThrow(mock(ShutdownSignalException.class)).when(rabbitClient).testConnection();	
		DryRunRequest<VirtualizationConnectorDto> spyRequest = spy(request); 

		// Act.
		util.checkOpenstackConnection(spyRequest, vc);

		// Assert
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.CONTROLLER_EXCEPTION);
		verify(spyRequest, times(1)).isIgnoreErrorsAndCommit(ErrorType.PROVIDER_EXCEPTION);
		verify(rabbitClient, times(1)).testConnection();
	}

}