package org.osc.core.broker.service.tasks.conformance.manager;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ManagerApiFactory.class})
public class MCConformanceCheckMetaTaskTest {
    @Mock
    public Session sessionMock;

    private ApplianceManagerConnector mc;

    private TaskGraph expectedGraph;

    public MCConformanceCheckMetaTaskTest(ApplianceManagerConnector mc, TaskGraph tg) {
        this.mc = mc;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        for (ApplianceManagerConnector mc: TEST_MANAGER_CONNECTORS) {
            doReturn(mc).when(this.sessionMock).get(ApplianceManagerConnector.class, mc.getId());
        }

        ApplianceManagerApi mcPolicyMappingSupportedApi = createApplianceManagerApi(true);
        ApplianceManagerApi mcPolicyMappingNotSupportedApi = createApplianceManagerApi(false);

        PowerMockito.mockStatic(ManagerApiFactory.class);
        when(ManagerApiFactory.createApplianceManagerApi(POLICY_MAPPING_NOT_SUPPORTED_MC.getManagerType())).thenReturn(mcPolicyMappingNotSupportedApi);
        when(ManagerApiFactory.createApplianceManagerApi(POLICY_MAPPING_SUPPORTED_MC.getManagerType())).thenReturn(mcPolicyMappingSupportedApi);
    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        MCConformanceCheckMetaTask task = new MCConformanceCheckMetaTask(this.mc, null);

        // Act.
        task.executeTransaction(this.sessionMock);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() throws Exception {
        return Arrays.asList(new Object[][] {
            {POLICY_MAPPING_NOT_SUPPORTED_MC, createMcPolicyMappingNotSupportedGraph(POLICY_MAPPING_NOT_SUPPORTED_MC)},
            {POLICY_MAPPING_SUPPORTED_MC, createMcPolicyMappingSupportedGraph(POLICY_MAPPING_SUPPORTED_MC)}
        });
    }

    private ApplianceManagerApi createApplianceManagerApi(boolean isPolicyMappingSupported) throws Exception {
        ApplianceManagerApi mcApi = mock(ApplianceManagerApi.class);
        when(mcApi.isPolicyMappingSupported()).thenReturn(isPolicyMappingSupported);
        when(mcApi.getPublicKey(any(ApplianceManagerConnectorElement.class))).thenReturn(PUBLIC_KEY);
        return mcApi;
    }
}
