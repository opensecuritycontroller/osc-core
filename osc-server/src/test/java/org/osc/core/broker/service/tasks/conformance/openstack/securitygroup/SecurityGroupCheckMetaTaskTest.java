package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import static org.mockito.Mockito.*;
import static org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupCheckMetaTaskTestData.*;

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
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.util.SessionStub;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ManagerApiFactory.class})
public class SecurityGroupCheckMetaTaskTest {
    @Mock
    public Session sessionMock;

    private SecurityGroup sg;

    private TaskGraph expectedGraph;

    private SessionStub sessionStub;

    public SecurityGroupCheckMetaTaskTest(SecurityGroup sg, TaskGraph tg) {
        this.sg = sg;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        this.sessionStub = new SessionStub(this.sessionMock);

        for (SecurityGroup sg: TEST_SECURITY_GROUPS) {
            doReturn(sg).when(this.sessionMock).get(SecurityGroup.class, sg.getId());
        }

        this.sessionStub.stubListReferencedVSBySecurityGroup(NO_MC_POLICY_MAPPING_SUPPORTED_SG.getId(), Arrays.asList(MC_POLICY_MAPPING_NOT_SUPPORTED_VS));
        this.sessionStub.stubListReferencedVSBySecurityGroup(SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG.getId(), Arrays.asList(MC_POLICY_MAPPING_SUPPORTED_VS));
        this.sessionStub.stubListReferencedVSBySecurityGroup(MULTIPLE_MC_POLICY_MAPPING_SUPPORTED_SG.getId(), MC_POLICY_MAPPING_SUPPORTED_VS_LIST);

        ApplianceManagerApi mcPolicyMappingSupportedApi = mock(ApplianceManagerApi.class);
        when(mcPolicyMappingSupportedApi.isPolicyMappingSupported()).thenReturn(true);

        ApplianceManagerApi mcPolicyMappingNotSupportedApi = mock(ApplianceManagerApi.class);
        when(mcPolicyMappingNotSupportedApi.isPolicyMappingSupported()).thenReturn(false);

        PowerMockito.mockStatic(ManagerApiFactory.class);

        when(ManagerApiFactory.createApplianceManagerApi(MC_POLICY_MAPPING_NOT_SUPPORTED_VS)).thenReturn(mcPolicyMappingNotSupportedApi);

        for (VirtualSystem vs : MC_POLICY_MAPPING_SUPPORTED_VS_LIST) {
            when(ManagerApiFactory.createApplianceManagerApi(vs)).thenReturn(mcPolicyMappingSupportedApi);
        }
    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        SecurityGroupCheckMetaTask task = new SecurityGroupCheckMetaTask(this.sg);

        // Act.
        task.executeTransaction(this.sessionMock);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {NO_MC_POLICY_MAPPING_SUPPORTED_SG, createNoMcPolicyMappingGraph(NO_MC_POLICY_MAPPING_SUPPORTED_SG)},
            {SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG, createSingleMcPolicyMappingGraph(SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG)},
            {MULTIPLE_MC_POLICY_MAPPING_SUPPORTED_SG, createMultipleMcPolicyMappingGraph(MULTIPLE_MC_POLICY_MAPPING_SUPPORTED_SG)}
        });
    }
}
