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
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.*;

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
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.tasks.conformance.GenerateVSSKeysTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteVsFromDbTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteVSSDeviceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteFlavorTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteImageFromGlanceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.SecurityGroupCleanupCheckMetaTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.util.ServerUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({LockUtil.class, StaticRegistry.class})
@PowerMockIgnore("javax.net.ssl.*")
public class VSConformanceCheckMetaTaskTest {

    private static final String ENCRYPTED_PASSWORD = "encrypted";

    @Mock
    public EntityManager em;

    // This is initialised to create the test data
    private static EncryptionApi encryption;

    @InjectMocks
    private VSConformanceCheckMetaTask vsConformanceCheckMetaTask;

    @InjectMocks
    private PasswordUtil passwordUtil;

    @InjectMocks
    private MgrCheckDevicesMetaTask mgrCheckDevicesMetaTask;

    @InjectMocks
    private DSConformanceCheckMetaTask dsConformanceCheckMetaTask;

    @InjectMocks
    private MgrDeleteVSSDeviceTask mgrDeleteVSSDeviceTask;

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    public VSConformanceCheckMetaTaskTest(VirtualSystem vs, TaskGraph tg, boolean extraAutomation) {
        this.vs = vs;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        // @InjectMocks doesn't inject these fields
        this.vsConformanceCheckMetaTask.passwordUtil = this.passwordUtil;
        this.vsConformanceCheckMetaTask.mgrCheckDevicesMetaTask = this.mgrCheckDevicesMetaTask;
        this.vsConformanceCheckMetaTask.dsConformanceCheckMetaTask = this.dsConformanceCheckMetaTask;
        this.vsConformanceCheckMetaTask.mgrDeleteVSSDeviceTask = this.mgrDeleteVSSDeviceTask;
        this.vsConformanceCheckMetaTask.generateVSSKeysTask = new GenerateVSSKeysTask();
        this.vsConformanceCheckMetaTask.securityGroupCleanupCheckMetaTask = new SecurityGroupCleanupCheckMetaTask();
        this.vsConformanceCheckMetaTask.deleteVsFromDbTask = new DeleteVsFromDbTask();
        this.vsConformanceCheckMetaTask.deleteFlavorTask = new DeleteFlavorTask();
        this.vsConformanceCheckMetaTask.deleteImageFromGlanceTask = new DeleteImageFromGlanceTask();

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

        PowerMockito.mockStatic(StaticRegistry.class);
        Mockito.when(StaticRegistry.encryptionApi()).thenReturn(encryption);
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
        encryption = Mockito.mock(EncryptionApi.class);

        PowerMockito.mockStatic(StaticRegistry.class);
        Mockito.when(StaticRegistry.encryptionApi()).thenReturn(encryption);

        Mockito.when(encryption.encryptAESCTR("")).thenReturn(ENCRYPTED_PASSWORD);

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
