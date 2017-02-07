package org.osc.core.broker.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.osc.core.broker.service.AgentRegisterServiceTestData.*;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.AgentStatus;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.broker.service.request.AgentRegisterServiceRequestValidator;
import org.osc.core.broker.service.response.AgentRegisterServiceResponse;
import org.osc.core.broker.service.tasks.agent.AgentInterfaceEndpointMapSetTask;
import org.osc.core.broker.service.tasks.agent.UpdateApplianceConsolePasswordTask;
import org.osc.core.broker.util.SessionStub;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.SetLockObjectReferenceMatcher;
import org.osc.core.test.util.TaskGraphMatcher;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.NetworkUtil;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.DistributedApplianceInstanceElement;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.osc.sdk.sdn.element.AgentStatusElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ManagerApiFactory.class, JobEngine.class, VMwareSdnApiFactory.class, HibernateUtil.class })
public class AgentRegisterServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Session sessionMock;
    
    @Mock
    private SessionFactory sessionFactoryMock;

    @Mock
    private AgentRegisterServiceRequestValidator validatorMock;

    @InjectMocks
    private AgentRegisterService service;

    private SessionStub sessionStub;

    private JobEngine jobEngineMock;

    private AgentApi agentApiMock;

    private TaskGraphMatcher syncPwdTgMatcher;
    private SetLockObjectReferenceMatcher syncPwdLorMatcher;

    private TaskGraphMatcher syncSecGroupTgMatcher;
    private SetLockObjectReferenceMatcher syncSecGroupLorMatcher;

    private static String UPDATE_CONSOLE_PWD_STR = "Update out-of-sync Appliance Console Password for DAI : '";
    private static String UPDATE_POLICY_STR = "Update out-of-sync Appliance Traffic Policy Mapping for DAI : '";

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.sessionStub = new SessionStub(this.sessionMock);
        this.agentApiMock = mock(AgentApi.class);

        PowerMockito.spy(HibernateUtil.class);
        PowerMockito.doReturn(sessionFactoryMock).when(HibernateUtil.class, "init");
        
        Mockito.when(sessionFactoryMock.openSession()).thenReturn(sessionMock);
        Mockito.when(service.getSessionFactory()).thenReturn(sessionFactoryMock);

        doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock).validateAndLoad(INVALID_REQUEST);

        registerExistingDais();
        registerNewDais();
        registerExistingVirtualSystems();
        registerApplianceManagerApis();
        registerAgent();
        registerAgentHealthStatus();

        setupJobEngine();
    }

    @Test
    public void testDispatch_WhenRequestValidationFails_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);

        // Act.
        this.service.dispatch(INVALID_REQUEST);

        // Assert.
        verify(this.validatorMock).validateAndLoad(INVALID_REQUEST);
    }

    @Test
    public void testDispatch_WithNullDaiAndOpenstackVs_ExpectsNothingIsPersisted() throws Exception {
        // Arrange.

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(NULL_DAI_OPENSTACK_REQUEST);

        // Assert.
        validateEmptyResponse(response);

        verify(this.validatorMock).validateAndLoad(NULL_DAI_OPENSTACK_REQUEST);
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(0)).update(any());
    }

    @Test
    public void testDispatch_WithNullDaiAndVmwareVs_ExpectsDaiIsPersisted() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher = new DistributedApplianceInstanceMatcher(
                NULL_DAI_VMWARE_REQUEST,
                null,
                NULL_DAI_VMWARE_DAI_ID,
                NULL_DAI_VMWARE_REQUEST.getName(),
                true,
                NULL_DAI_VMWARE_REQUEST.getApplianceIp(),
                NULL_DAI_VMWARE_REQUEST.getApplianceGateway(),
                null);

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(NULL_DAI_VMWARE_REQUEST);

        // Assert.
        validateResponse(response, NULL_DAI_VMWARE_REQUEST, VMWARE_VS);
        verify(this.validatorMock).validateAndLoad(NULL_DAI_VMWARE_REQUEST);
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock, times(1)).update(any());
        verify(this.sessionMock).update(argThat(daiMatcher));
    }

    @Test
    public void testDispatch_WithOpenstackMismatchingVsId_ExpectsDaiIsDeleted() throws Exception {
        // Arrange.

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(OPENSTACK_MISMATCH_VS_ID_REQUEST);

        // Assert.
        validateEmptyResponse(response);
        verify(this.validatorMock).validateAndLoad(OPENSTACK_MISMATCH_VS_ID_REQUEST);
        verify(this.sessionMock).delete(MISTMATCH_VS_ID_DAI);
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(0)).update(any());
    }

    @Test
    public void testDispatch_WithExistingDai_ExpectsDaiIsNotCreated() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher = new DistributedApplianceInstanceMatcher(
                EXISTING_DAI_REQUEST,
                null,
                EXISTING_DAI.getId(),
                EXISTING_DAI.getName(),
                EXISTING_DAI.isPolicyMapOutOfSync(),
                EXISTING_DAI.getIpAddress(),
                EXISTING_DAI_REQUEST.getApplianceGateway(),
                null);

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(EXISTING_DAI_REQUEST);

        // Assert.
        validateResponse(response, EXISTING_DAI_REQUEST, VMWARE_VS);
        verify(this.validatorMock).validateAndLoad(EXISTING_DAI_REQUEST);
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(1)).update(any());
        verify(this.sessionMock).update(argThat(daiMatcher));
    }

    @Test
    public void testDispatch_WithoutNsxAgent_ExpectsASingleUpdateCall() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher = new DistributedApplianceInstanceMatcher(
                NO_NSX_AGENT_REQUEST,
                null,
                NO_NSX_AGENT_DAI_ID,
                NO_NSX_AGENT_REQUEST.getName(),
                true,
                NO_NSX_AGENT_REQUEST.getApplianceIp(),
                NO_NSX_AGENT_REQUEST.getApplianceGateway(),
                null);

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(NO_NSX_AGENT_REQUEST);

        // Assert.
        validateResponse(response, NO_NSX_AGENT_REQUEST, VMWARE_VS);
        verify(this.validatorMock).validateAndLoad(NO_NSX_AGENT_REQUEST);
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock, times(1)).update(any());
        verify(this.sessionMock).update(argThat(daiMatcher));
    }

    @Test
    public void testDispatch_WithNsxAgent_ExpectsUpdateIsCalledTwice() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher = new DistributedApplianceInstanceMatcher(
                WITH_NSX_AGENT_REQUEST,
                NSX_AGENT,
                WITH_NSX_AGENT_DAI_ID,
                WITH_NSX_AGENT_REQUEST.getName(),
                true,
                WITH_NSX_AGENT_REQUEST.getApplianceIp(),
                NSX_AGENT.getGateway(),
                NSX_AGENT.getSubnetPrefixLength());

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(WITH_NSX_AGENT_REQUEST);

        // Assert.
        validateResponse(response, WITH_NSX_AGENT_REQUEST, WITH_NSX_AGENT_VS);
        verify(this.validatorMock).validateAndLoad(WITH_NSX_AGENT_REQUEST);
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock, times(2)).update(argThat(daiMatcher));
    }

    @Test
    public void testDispatch_WithMismatchHealthStatusInspectionReady_ResourceHealthIsUpdated() throws Exception {
        // Arrange.
        AgentStatus expectedStatus = new AgentStatus("UP", null);

        // Act.
        // Assert.
        testDispatch_WithMismatchHealthStatus(DAI_INSPECTION_READY_REQUEST, AGENT_HEALTH_MISMATCH_DAI, expectedStatus);
    }

    @Test
    public void testDispatch_WithMismatchHealthStatusDiscovered_ResourceHealthIsUpdated() throws Exception {
        // Arrange.
        AgentStatus expectedStatus = new AgentStatus("WARNING", "Appliance is not ready for inspection.");

        // Act.
        // Assert.
        testDispatch_WithMismatchHealthStatus(DAI_DISCOVERED_REQUEST, AGENT_HEALTH_MISMATCH_DISCOVERED_DAI, expectedStatus);
    }

    @Test
    public void testDispatch_WithMismatchHealthStatusNotDiscoveredNotReady_ResourceHealthIsUpdated() throws Exception {
        // Arrange.
        AgentStatus expectedStatus = new AgentStatus("DOWN", "Appliance is not discovered and/or ready (discovery:false, ready:false).");

        // Act.
        // Assert.
        testDispatch_WithMismatchHealthStatus(DAI_NOT_DISCOVERED_NOT_READY_REQUEST, AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_DAI, expectedStatus);
    }

    @Test
    public void testDispatch_WithMatchingHealthStatus_ResourceHealthIsNotUpdated() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher =
                new DistributedApplianceInstanceMatcher(
                        DAI_AGENT_HEALTH_MATCH_REQUEST,
                        null,
                        AGENT_HEALTH_MATCH_DAI.getId(),
                        AGENT_HEALTH_MATCH_DAI.getName(),
                        false,
                        AGENT_HEALTH_MATCH_DAI.getIpAddress(),
                        DAI_AGENT_HEALTH_MATCH_REQUEST.getApplianceGateway(),
                        null);

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(DAI_AGENT_HEALTH_MATCH_REQUEST);

        // Assert.
        validateResponse(response, null, VMWARE_VS, AGENT_HEALTH_MATCH_DAI);
        verify(this.validatorMock).validateAndLoad(DAI_AGENT_HEALTH_MATCH_REQUEST);
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock).update(argThat(daiMatcher));
        verify(this.agentApiMock, times(0)).updateAgentStatus(anyString(), any(AgentStatusElement.class));
    }

    @Test
    public void testDispatch_WhenDaiHasNewConsolePassword_PasswordIsSyncd() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher =
                new DistributedApplianceInstanceMatcher(
                        NEW_CONSOLE_PASSWORD_REQUEST,
                        null,
                        NEW_CONSOLE_PASSWORD_DAI.getId(),
                        NEW_CONSOLE_PASSWORD_DAI.getName(),
                        false,
                        NEW_CONSOLE_PASSWORD_DAI.getIpAddress(),
                        NEW_CONSOLE_PASSWORD_REQUEST.getApplianceGateway(),
                        String.valueOf(NetworkUtil.getPrefixLength(NEW_CONSOLE_PASSWORD_REQUEST.getApplianceSubnetMask())));

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(NEW_CONSOLE_PASSWORD_REQUEST);

        // Assert.
        validateResponse(response, null, VMWARE_VS, NEW_CONSOLE_PASSWORD_DAI);

        verify(this.validatorMock).validateAndLoad(NEW_CONSOLE_PASSWORD_REQUEST);
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock).update(argThat(daiMatcher));

        verify(this.jobEngineMock)
        .submit(
                matches(UPDATE_CONSOLE_PWD_STR + NEW_CONSOLE_PASSWORD_DAI.getName() + "'"),
                argThat(this.syncPwdTgMatcher),
                argThat(this.syncPwdLorMatcher));
    }

    @Test
    public void testDispatch_WhenPolicyMappingIsOutOfSync_PolicyIsSyncd() throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher =
                new DistributedApplianceInstanceMatcher(
                        SEC_GROUP_OUT_OF_SYNC_REQUEST,
                        null,
                        SEC_GROUP_OUT_OF_SYNC_DAI.getId(),
                        SEC_GROUP_OUT_OF_SYNC_DAI.getName(),
                        true,
                        SEC_GROUP_OUT_OF_SYNC_REQUEST.getApplianceIp(),
                        SEC_GROUP_OUT_OF_SYNC_REQUEST.getApplianceGateway(),
                        null);

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(SEC_GROUP_OUT_OF_SYNC_REQUEST);

        // Assert.
        validateResponse(response, null, VMWARE_VS, SEC_GROUP_OUT_OF_SYNC_DAI);
        verify(this.validatorMock).validateAndLoad(SEC_GROUP_OUT_OF_SYNC_REQUEST);
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock).update(argThat(daiMatcher));

        verify(this.jobEngineMock)
        .submit(
                matches(UPDATE_POLICY_STR + SEC_GROUP_OUT_OF_SYNC_DAI.getName() + "'"),
                argThat(this.syncSecGroupTgMatcher),
                argThat(this.syncSecGroupLorMatcher));
    }

    private void testDispatch_WithMismatchHealthStatus(AgentRegisterServiceRequest request, DistributedApplianceInstance dai, AgentStatus expectedStatus) throws Exception {
        // Arrange.
        DistributedApplianceInstanceMatcher daiMatcher =
                new DistributedApplianceInstanceMatcher(
                        request,
                        null,
                        dai.getId(),
                        dai.getName(),
                        false,
                        dai.getIpAddress(),
                        request.getApplianceGateway(),
                        null);

        // Act.
        AgentRegisterServiceResponse response = this.service.dispatch(request);

        // Assert.
        validateResponse(response, null, VMWARE_VS, dai);
        verify(this.validatorMock).validateAndLoad(request);
        verify(this.sessionMock, times(0)).save(any());
        verify(this.sessionMock, times(0)).delete(any());
        verify(this.sessionMock).update(argThat(daiMatcher));
        verify(this.agentApiMock).updateAgentStatus(dai.getNsxAgentId(), expectedStatus);
    }

    private void validateResponse(AgentRegisterServiceResponse response, AgentRegisterServiceRequest request,
            VirtualSystem vs) {
        validateResponse(response, request, vs, null);
    }

    private void validateResponse(AgentRegisterServiceResponse response, AgentRegisterServiceRequest request,
            VirtualSystem vs, DistributedApplianceInstance dai) {
        assertThat(response).as("response").isNotNull();
        assertThat(response.getApplianceConfig1()).as("applianceConfig1").isEqualTo(DEVICE_CONFIGURATION);
        assertThat(response.getApplianceConfig2()).as("applianceConfig2").isEqualTo(DEVICE_ADDITIONAL_CONFIGURATION);
        assertThat(response.getApplianceName()).as("applianceName")
                .isEqualTo(dai == null ? request.getName() : dai.getName());
        assertThat(response.getMgrIp()).as("mgrIp")
                .isEqualTo(vs.getDistributedAppliance().getApplianceManagerConnector().getIpAddress());
        assertThat(response.getSharedSecretKey()).as("sharedSecretKey")
                .isEqualTo(vs.getDistributedAppliance().getMgrSecretKey());
    }

    private void validateEmptyResponse(AgentRegisterServiceResponse response) {
        assertThat(response).as("response").isNotNull();
        assertThat(response.getApplianceConfig1()).as("applianceConfig1").isNull();
        assertThat(response.getApplianceConfig2()).as("applianceConfig2").isNull();
        assertThat(response.getApplianceName()).as("applianceName").isNull();
        assertThat(response.getMgrIp()).as("mgrIp").isNull();
        assertThat(response.getSharedSecretKey()).as("sharedSecretKey").isNull();
    }

    private void registerExistingDais() throws Exception {
        when(this.validatorMock.validateAndLoad(OPENSTACK_MISMATCH_VS_ID_REQUEST)).thenReturn(MISTMATCH_VS_ID_DAI);
        when(this.validatorMock.validateAndLoad(EXISTING_DAI_REQUEST)).thenReturn(EXISTING_DAI);
        when(this.validatorMock.validateAndLoad(DAI_INSPECTION_READY_REQUEST)).thenReturn(AGENT_HEALTH_MISMATCH_DAI);
        when(this.validatorMock.validateAndLoad(DAI_DISCOVERED_REQUEST)).thenReturn(AGENT_HEALTH_MISMATCH_DISCOVERED_DAI);
        when(this.validatorMock.validateAndLoad(DAI_NOT_DISCOVERED_NOT_READY_REQUEST)).thenReturn(AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_DAI);
        when(this.validatorMock.validateAndLoad(DAI_AGENT_HEALTH_MATCH_REQUEST)).thenReturn(AGENT_HEALTH_MATCH_DAI);
        when(this.validatorMock.validateAndLoad(NEW_CONSOLE_PASSWORD_REQUEST)).thenReturn(NEW_CONSOLE_PASSWORD_DAI);
        when(this.validatorMock.validateAndLoad(SEC_GROUP_OUT_OF_SYNC_REQUEST)).thenReturn(SEC_GROUP_OUT_OF_SYNC_DAI);
    }

    private void registerNewDais() {
        this.sessionStub.stubSaveEntity(new DistributedApplianceInstanceIpMatcher(
                NULL_DAI_VMWARE_REQUEST.getApplianceIp()), NULL_DAI_VMWARE_DAI_ID);

        this.sessionStub.stubSaveEntity(new DistributedApplianceInstanceIpMatcher(
                NO_NSX_AGENT_REQUEST.getApplianceIp()), NO_NSX_AGENT_DAI_ID);

        this.sessionStub.stubSaveEntity(new DistributedApplianceInstanceIpMatcher(
                WITH_NSX_AGENT_REQUEST.getApplianceIp()), WITH_NSX_AGENT_DAI_ID);
    }

    private void registerExistingVirtualSystems() throws Exception {
        when(this.sessionMock.get(VirtualSystem.class, OPENSTACK_VS_ID)).thenReturn(OPENSTACK_VS);
        when(this.sessionMock.get(VirtualSystem.class, MISMATCHING_VS_ID)).thenReturn(OPENSTACK_VS);
        when(this.sessionMock.get(VirtualSystem.class, VMWARE_VS_ID)).thenReturn(VMWARE_VS);
        when(this.sessionMock.get(VirtualSystem.class, NO_NSX_AGENT_VS_ID)).thenReturn(NO_NSX_AGENT_VS);
        when(this.sessionMock.get(VirtualSystem.class, WITH_NSX_AGENT_VS_ID)).thenReturn(WITH_NSX_AGENT_VS);

        ManagerDeviceApi deviceMgrApi = mock(ManagerDeviceApi.class);

        when(deviceMgrApi.getDeviceMemberConfiguration(
                argThat(new DistributedApplianceInstanceElementMatcher(VMWARE_VS))))
        .thenReturn(DEVICE_CONFIGURATION);

        when(deviceMgrApi.getDeviceMemberAdditionalConfiguration(
                argThat(new DistributedApplianceInstanceElementMatcher(VMWARE_VS))))
        .thenReturn(DEVICE_ADDITIONAL_CONFIGURATION);

        when(deviceMgrApi.getDeviceMemberConfiguration(
                argThat(new DistributedApplianceInstanceElementMatcher(NO_NSX_AGENT_VS))))
        .thenReturn(DEVICE_CONFIGURATION);

        when(deviceMgrApi.getDeviceMemberAdditionalConfiguration(
                argThat(new DistributedApplianceInstanceElementMatcher(NO_NSX_AGENT_VS))))
        .thenReturn(DEVICE_ADDITIONAL_CONFIGURATION);

        when(deviceMgrApi.getDeviceMemberConfiguration(
                argThat(new DistributedApplianceInstanceElementMatcher(WITH_NSX_AGENT_VS))))
        .thenReturn(DEVICE_CONFIGURATION);

        when(deviceMgrApi.getDeviceMemberAdditionalConfiguration(
                argThat(new DistributedApplianceInstanceElementMatcher(WITH_NSX_AGENT_VS))))
        .thenReturn(DEVICE_ADDITIONAL_CONFIGURATION);

        PowerMockito.mockStatic(ManagerApiFactory.class);

        when(ManagerApiFactory.createManagerDeviceApi(VMWARE_VS)).thenReturn(deviceMgrApi);
        when(ManagerApiFactory.createManagerDeviceApi(NO_NSX_AGENT_VS)).thenReturn(deviceMgrApi);
        when(ManagerApiFactory.createManagerDeviceApi(WITH_NSX_AGENT_VS)).thenReturn(deviceMgrApi);

        PowerMockito.mockStatic(VMwareSdnApiFactory.class);

        when(VMwareSdnApiFactory.createAgentApi(VMWARE_VS)).thenReturn(this.agentApiMock);
        when(VMwareSdnApiFactory.createAgentApi(NO_NSX_AGENT_VS)).thenReturn(this.agentApiMock);
        when(VMwareSdnApiFactory.createAgentApi(WITH_NSX_AGENT_VS)).thenReturn(this.agentApiMock);
    }

    private void registerApplianceManagerApis() throws Exception {
        ApplianceManagerApi applianceMgrSecurityGroupSyncNotSupported = createApplianceManagerApi(false, false);
        ApplianceManagerApi applianceMgrSecurityGroupSyncSupported = createApplianceManagerApi(true, true);
        ApplianceManagerApi applianceMgrNsmSupported = createApplianceManagerApi(false, true);

        when(ManagerApiFactory.createApplianceManagerApi(
                argThat(new DistributedApplianceInstanceGroupSyncNotSupportMatcher())))
        .thenReturn(applianceMgrSecurityGroupSyncNotSupported);

        when(ManagerApiFactory.createApplianceManagerApi(SEC_GROUP_OUT_OF_SYNC_DAI))
        .thenReturn(applianceMgrSecurityGroupSyncSupported);

        when(ManagerApiFactory.createApplianceManagerApi(VMWARE_VS)).thenReturn(applianceMgrNsmSupported);
        when(ManagerApiFactory.createApplianceManagerApi(NO_NSX_AGENT_VS)).thenReturn(applianceMgrNsmSupported);
        when(ManagerApiFactory.createApplianceManagerApi(WITH_NSX_AGENT_VS)).thenReturn(applianceMgrNsmSupported);
    }

    private ApplianceManagerApi createApplianceManagerApi(boolean secGroupSyncSupported, boolean isAgentManaged) {
        ApplianceManagerApi applianceMgrApi = mock(ApplianceManagerApi.class);
        when(applianceMgrApi.isSecurityGroupSyncSupport()).thenReturn(secGroupSyncSupported);
        when(applianceMgrApi.isAgentManaged()).thenReturn(isAgentManaged);

        return applianceMgrApi;
    }

    private void registerAgent() throws Exception {
        List<AgentElement> agents = Arrays.asList(NSX_AGENT, mock(AgentElement.class));

        doReturn(agents).when(this.agentApiMock).getAgents(WITH_NSX_AGENT_VS.getNsxServiceId());
    }

    private void registerAgentHealthStatus() throws Exception {
        AgentStatus currentStatus = new AgentStatus("CURRENT_STATUS", null);

        AgentStatus matchStatus = new AgentStatus("UP", null);

        registerAgentHealthStatus(AGENT_HEALTH_MISMATCH_DAI.getNsxAgentId(), currentStatus);
        registerAgentHealthStatus(AGENT_HEALTH_MISMATCH_DISCOVERED_DAI.getNsxAgentId(), currentStatus);
        registerAgentHealthStatus(AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_DAI.getNsxAgentId(), currentStatus);
        registerAgentHealthStatus(AGENT_HEALTH_MATCH_DAI.getNsxAgentId(), matchStatus);
        registerAgentHealthStatus(NSX_AGENT.getId(), matchStatus);
    }

    private void registerAgentHealthStatus(String nsxAgentId, AgentStatusElement status) throws Exception {
        doReturn(status).when(this.agentApiMock).getAgentStatus(nsxAgentId);
    }

    private void setupJobEngine() throws Exception {
        Job jobMock = mock(Job.class);
        doReturn(10000L).when(jobMock).getId();

        this.jobEngineMock = mock(JobEngine.class);

        PowerMockito.mockStatic(JobEngine.class);
        when(JobEngine.getEngine())
        .thenReturn(this.jobEngineMock);

        TaskGraph syncPwdTg = new TaskGraph();
        syncPwdTg.addTask(new UpdateApplianceConsolePasswordTask(NEW_CONSOLE_PASSWORD_DAI,
                NEW_CONSOLE_PASSWORD_DAI.getNewConsolePassword()));

        this.syncPwdTgMatcher = new TaskGraphMatcher(syncPwdTg);
        this.syncPwdLorMatcher = new SetLockObjectReferenceMatcher(LockObjectReference.getObjectReferences(NEW_CONSOLE_PASSWORD_DAI));

        doReturn(jobMock).when(this.jobEngineMock)
        .submit(
                matches(UPDATE_CONSOLE_PWD_STR + NEW_CONSOLE_PASSWORD_DAI.getName() + "'"),
                argThat(this.syncPwdTgMatcher),
                argThat(this.syncPwdLorMatcher));

        TaskGraph syncSecGroupTg = new TaskGraph();
        syncSecGroupTg.addTask(new AgentInterfaceEndpointMapSetTask(SEC_GROUP_OUT_OF_SYNC_DAI));

        this.syncSecGroupTgMatcher = new TaskGraphMatcher(syncSecGroupTg);
        this.syncSecGroupLorMatcher = new SetLockObjectReferenceMatcher(LockObjectReference.getObjectReferences(SEC_GROUP_OUT_OF_SYNC_DAI));

        doReturn(jobMock).when(this.jobEngineMock)
        .submit(matches(
                UPDATE_POLICY_STR + SEC_GROUP_OUT_OF_SYNC_DAI.getName() + "'"),
                argThat(this.syncSecGroupTgMatcher),
                argThat(this.syncSecGroupLorMatcher));
    }

    private class DistributedApplianceInstanceIpMatcher extends ArgumentMatcher<Object> {
        private String applianceIp;

        public DistributedApplianceInstanceIpMatcher(String applianceIp) {
            this.applianceIp = applianceIp;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceInstance)) {
                return false;
            }

            DistributedApplianceInstance dai = (DistributedApplianceInstance)object;
            return this.applianceIp.equals(dai.getIpAddress()) &&
                    dai.getName().startsWith(DAI_TEMPORARY_NAME);
        }
    }

    private class DistributedApplianceInstanceGroupSyncNotSupportMatcher extends ArgumentMatcher<DistributedApplianceInstance> {

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceInstance)) {
                return false;
            }

            DistributedApplianceInstance dai = (DistributedApplianceInstance)object;
            return SEC_GROUP_OUT_OF_SYNC_DAI.getId() != dai.getId();
        }
    }

    private class DistributedApplianceInstanceElementMatcher extends ArgumentMatcher<DistributedApplianceInstanceElement> {
        private VirtualSystem vs;

        public DistributedApplianceInstanceElementMatcher(VirtualSystem vs) {
            this.vs = vs;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceInstanceElement)) {
                return false;
            }

            DistributedApplianceInstanceElement dai = (DistributedApplianceInstanceElement)object;
            return dai.getVirtualSystem().equals(this.vs);
        }
    }

    private class DistributedApplianceInstanceMatcher extends ArgumentMatcher<DistributedApplianceInstanceMatcher> {
        private AgentRegisterServiceRequest request;
        private AgentElement agent;
        private Long daiId;
        private String daiName;
        private boolean daiIsPolicyMapOutOfSync;
        private String daiIpAddress;
        private String mgmtGateway;
        private String daiSubnetMaskPrefixLength;

        public DistributedApplianceInstanceMatcher(
                AgentRegisterServiceRequest request,
                AgentElement agent,
                Long daiId,
                String daiName,
                boolean daiIsPolicyMapOutOfSync,
                String daiIpAddress,
                String mgmtGateway,
                String daiSubnetMaskPrefixLength) {
            this.request = request;
            this.agent = agent;
            this.daiId = daiId;
            this.daiName = daiName;
            this.daiIsPolicyMapOutOfSync= daiIsPolicyMapOutOfSync;
            this.daiIpAddress = daiIpAddress;
            this.mgmtGateway = mgmtGateway;
            this.daiSubnetMaskPrefixLength = daiSubnetMaskPrefixLength;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceInstance)) {
                return false;
            }

            DistributedApplianceInstance dai = (DistributedApplianceInstance)object;
            boolean result =
                    this.daiId.equals(dai.getId()) &&
                    this.daiIpAddress.equals(dai.getIpAddress()) &&
                    this.daiName.equals(dai.getName()) &&
                    this.daiIsPolicyMapOutOfSync == dai.isPolicyMapOutOfSync() &&
                    EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS).equals(dai.getPassword()) &&
                    this.request.getAgentVersion().getMajor().equals(dai.getAgentVersionMajor()) &&
                    this.request.getAgentVersion().getMinor().equals(dai.getAgentVersionMinor()) &&
                    this.request.getAgentVersion().getVersionStr().equals(dai.getAgentVersionStr()) &&
                    this.request.getApplianceIp().equals(dai.getMgmtIpAddress()) &&
                    this.mgmtGateway.equals(dai.getMgmtGateway()) &&
                    ((this.daiSubnetMaskPrefixLength == null && dai.getMgmtSubnetPrefixLength() == null) ||
                            this.daiSubnetMaskPrefixLength.equals(dai.getMgmtSubnetPrefixLength())) &&
                    this.request.isDiscovered() == dai.getDiscovered() &&
                    this.request.isInspectionReady() == dai.getInspectionReady() &&
                    dai.getLastStatus() != null &&
                    this.request.getAgentDpaInfo().netXDpaRuntimeInfo.workloadInterfaces.equals(dai.getWorkloadInterfaces()) &&
                    this.request.getAgentDpaInfo().netXDpaRuntimeInfo.rx.equals(dai.getPackets());

            if (result && this.agent != null) {
                result = result &&
                        this.agent.getId().equals(dai.getNsxAgentId()) &&
                        this.agent.getHostId().equals(dai.getNsxHostId()) &&
                        this.agent.getHostName().equals(dai.getNsxHostName()) &&
                        this.agent.getVmId().equals(dai.getNsxVmId()) &&
                        this.agent.getHostVsmId().equals(dai.getNsxHostVsmUuid()) &&
                        this.agent.getGateway().equals(dai.getMgmtGateway()) &&
                        this.agent.getSubnetPrefixLength().equals(dai.getMgmtSubnetPrefixLength());
            }
            return result;
        }
    }
}
