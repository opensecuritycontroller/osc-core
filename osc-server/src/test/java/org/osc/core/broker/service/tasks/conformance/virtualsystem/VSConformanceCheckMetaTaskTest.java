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
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.TEST_VIRTUAL_SYSTEMS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteOpenStackWithDeploymentSpecGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteOpenStackWithOSFlavorRefGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteOpenStackWithOSImageRefGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createOpenstacWhenLockingDeploymentSpecFailsGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createOpenstackNoDeploymentSpecGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createOpenstackWithDeploymentSpecGraph;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PowerMockIgnore("javax.net.ssl.*")
public class VSConformanceCheckMetaTaskTest {
    @Mock
    public EntityManager em;

    // this is initialised from getTestData(); otherwise the TaskNodeComparer fails
    private static ApiFactoryService apiFactoryService;

    @InjectMocks
    private VSConformanceCheckMetaTask vsConformanceCheckMetaTask;

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    public VSConformanceCheckMetaTaskTest(VirtualSystem vs, TaskGraph tg, boolean extraAutomation) {
        this.vs = vs;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        for (VirtualSystem vs: TEST_VIRTUAL_SYSTEMS) {
            Mockito.doReturn(vs).when(this.em).find(VirtualSystem.class, vs.getId());
        }

        PowerMockito.spy(LockUtil.class);
        PowerMockito.doReturn(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK).when(LockUtil.class, "tryLockDS",
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDeploymentSpecs().toArray()[0],
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDistributedAppliance(),
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDistributedAppliance().getApplianceManagerConnector(),
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getVirtualizationConnector());

        PowerMockito.doThrow(new NullPointerException()).when(LockUtil.class, "tryLockDS",
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDeploymentSpecs().toArray()[0],
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDistributedAppliance(),
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDistributedAppliance().getApplianceManagerConnector(),
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getVirtualizationConnector());

    }


    @Test
    public void testExecuteTransaction_WithVariousVirtualSystems_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        VSConformanceCheckMetaTask task = this.vsConformanceCheckMetaTask.create(this.vs);

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() throws EncryptionException {
        apiFactoryService = VSConformanceCheckMetaTaskTestData.apiFactoryService;
        return Arrays.asList(new Object[][] {
            {UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS,  createOpenstackNoDeploymentSpecGraph(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS), false},
            {UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS,  createOpenstackWithDeploymentSpecGraph(UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS), false},
            {UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS,  createOpenstacWhenLockingDeploymentSpecFailsGraph(UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS), false},
            {DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS,  createDeleteOpenStackWithDeploymentSpecGraph(DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS), false},
            {DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS,  createDeleteOpenStackWithOSImageRefGraph(DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS), false},
            {DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS,  createDeleteOpenStackWithOSFlavorRefGraph(DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS), false},
        });
    }

    private static String getDefaultServerIp() {
        ServerUtil.setServerIP("127.0.0.1");
        return ServerUtil.getServerIP();
    }
}
