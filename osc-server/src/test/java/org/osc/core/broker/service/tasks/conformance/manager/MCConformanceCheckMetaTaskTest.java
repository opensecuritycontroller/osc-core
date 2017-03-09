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
package org.osc.core.broker.service.tasks.conformance.manager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.POLICY_MAPPING_NOT_SUPPORTED_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.POLICY_MAPPING_SUPPORTED_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.PUBLIC_KEY;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.TEST_MANAGER_CONNECTORS;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.createMcPolicyMappingNotSupportedGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTaskTestData.createMcPolicyMappingSupportedGraph;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

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
import org.osc.core.broker.model.plugin.manager.ManagerType;
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
    public EntityManager em;

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
            doReturn(mc).when(this.em).find(ApplianceManagerConnector.class, mc.getId());
        }

        PowerMockito.mockStatic(ManagerApiFactory.class);

        setupApplianceManagerApiFactory(POLICY_MAPPING_SUPPORTED_MC.getManagerType(), true);
        setupApplianceManagerApiFactory(POLICY_MAPPING_NOT_SUPPORTED_MC.getManagerType(), false);

        when(ManagerApiFactory.syncsSecurityGroup(ManagerType.fromText(POLICY_MAPPING_SUPPORTED_MC.getManagerType()))).thenReturn(true);
    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        MCConformanceCheckMetaTask task = new MCConformanceCheckMetaTask(this.mc, null);

        // Act.
        task.executeTransaction(this.em);

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

    private void setupApplianceManagerApiFactory(String mgrType, boolean isPolicyMappingSupported) throws Exception {
        ManagerType.addType(mgrType);
        ApplianceManagerApi mcApi = mock(ApplianceManagerApi.class);
        when(mcApi.getPublicKey(any(ApplianceManagerConnectorElement.class))).thenReturn(PUBLIC_KEY);

        when(ManagerApiFactory.syncsPolicyMapping(ManagerType.fromText(mgrType))).thenReturn(isPolicyMappingSupported);
        when(ManagerApiFactory.createApplianceManagerApi(mgrType)).thenReturn(mcApi);
    }
}
