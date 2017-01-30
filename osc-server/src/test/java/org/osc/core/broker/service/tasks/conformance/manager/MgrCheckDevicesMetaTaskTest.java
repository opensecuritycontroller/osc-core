package org.osc.core.broker.service.tasks.conformance.manager;

import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.*;
import org.osc.core.broker.util.SessionStub;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ ManagerApiFactory.class })
public class MgrCheckDevicesMetaTaskTest {
    @Mock
    public Session sessionMock;

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    private SessionStub sessionStub;

    public MgrCheckDevicesMetaTaskTest(VirtualSystem vs, TaskGraph tg) {
        this.vs = vs;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.sessionStub = new SessionStub(this.sessionMock);

        for (VirtualSystem vs : TEST_VIRTUAL_SYSTEMS) {
            Mockito.doReturn(vs).when(this.sessionMock).get(VirtualSystem.class, vs.getId());
        }

        this.sessionStub.stubFindByFieldName("name", MGR_DEVICE_MEMBER_ELEMENT_NO_DAI.getName(), null);

        this.sessionStub.stubFindByFieldName("name", MGR_DEVICE_MEMBER_ELEMENT_WITH_DAI.getName(),
                new DistributedApplianceInstance());

        ManagerDeviceApi mgrDeviceGroupSupportedApi = mock(ManagerDeviceApi.class);
        when(mgrDeviceGroupSupportedApi.isDeviceGroupSupported()).thenReturn(true);

        ManagerDeviceApi mgrDeviceNoDaiApi = mock(ManagerDeviceApi.class);
        when(mgrDeviceNoDaiApi.isDeviceGroupSupported()).thenReturn(true);

        ManagerDeviceApi mgrDeviceGroupNotSupportedApi = mock(ManagerDeviceApi.class);
        when(mgrDeviceGroupNotSupportedApi.isDeviceGroupSupported()).thenReturn(false);

        Mockito.<List<? extends ManagerDeviceMemberElement>> when(mgrDeviceNoDaiApi.listDeviceMembers())
                .thenReturn(Arrays.asList(MGR_DEVICE_MEMBER_ELEMENT_NO_DAI));

        Mockito.<List<? extends ManagerDeviceMemberElement>> when(mgrDeviceGroupSupportedApi.listDeviceMembers())
                .thenReturn(Arrays.asList(MGR_DEVICE_MEMBER_ELEMENT_WITH_DAI));

        PowerMockito.mockStatic(ManagerApiFactory.class);
        when(ManagerApiFactory.createManagerDeviceApi(MANAGER_ID_PRESENT_NO_DAI_DEVICE_VS))
                .thenReturn(mgrDeviceNoDaiApi);
        when(ManagerApiFactory.createManagerDeviceApi(MANAGER_ID_NOT_PRESENT_VS))
                .thenReturn(mgrDeviceGroupSupportedApi);
        when(ManagerApiFactory.createManagerDeviceApi(MANAGER_DEVICE_ID_PRESENT_VS))
                .thenReturn(mgrDeviceGroupSupportedApi);
        when(ManagerApiFactory.createManagerDeviceApi(DAI_IP_PRESENT_VS))
        .thenReturn(mgrDeviceGroupSupportedApi);
        when(ManagerApiFactory.createManagerDeviceApi(MANAGER_DEVICE_ID_AND_DAI_IP_NOT_PRESENT_VS))
                .thenReturn(mgrDeviceGroupSupportedApi);
        when(ManagerApiFactory.createManagerDeviceApi(DEVICE_GROUP_NOT_SUPPORTED_VS))
                .thenReturn(mgrDeviceGroupNotSupportedApi);
        when(ManagerApiFactory.createManagerDeviceApi(MANAGER_ID_AND_DAI_DEVICE_PRESENT_VS))
                .thenReturn(mgrDeviceGroupSupportedApi);

    }

    @Test
    public void testExecuteTransaction_WithVariousVirtualSystem_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        MgrCheckDevicesMetaTask task = new MgrCheckDevicesMetaTask(this.vs);

        // Act.
        task.executeTransaction(this.sessionMock);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
                { MANAGER_ID_PRESENT_NO_DAI_DEVICE_VS, updateVSSDeviceAndDeleteMemberGraph(MANAGER_ID_PRESENT_NO_DAI_DEVICE_VS) },
                { MANAGER_ID_NOT_PRESENT_VS, createVSSDeviceGraph(MANAGER_ID_NOT_PRESENT_VS) },
                { MANAGER_DEVICE_ID_PRESENT_VS, mgrCreateVSSDeviceAndUpdateMemberGraph(MANAGER_DEVICE_ID_PRESENT_VS) },
                {DAI_IP_PRESENT_VS, mgrCreateVSSDeviceAndCreateMemberGraph(DAI_IP_PRESENT_VS)},
                { MANAGER_DEVICE_ID_AND_DAI_IP_NOT_PRESENT_VS, createVSSDeviceGraph(MANAGER_DEVICE_ID_AND_DAI_IP_NOT_PRESENT_VS) },
                { DEVICE_GROUP_NOT_SUPPORTED_VS, updateDAISManagerDeviceIdGraph(DEVICE_GROUP_NOT_SUPPORTED_VS) },
                { MANAGER_ID_AND_DAI_DEVICE_PRESENT_VS, updateVSSDeviceGraph(MANAGER_ID_AND_DAI_DEVICE_PRESENT_VS) } 
                });
    }
}
