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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.DAI_IP_PRESENT_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.DEVICE_GROUP_NOT_SUPPORTED_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MANAGER_DEVICE_ID_AND_DAI_IP_NOT_PRESENT_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MANAGER_DEVICE_ID_PRESENT_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MANAGER_ID_AND_DAI_DEVICE_PRESENT_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MANAGER_ID_NOT_PRESENT_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MANAGER_ID_PRESENT_NO_DAI_DEVICE_VS;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MGR_DEVICE_MEMBER_ELEMENT_NO_DAI;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.MGR_DEVICE_MEMBER_ELEMENT_WITH_DAI;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.createVSSDeviceGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.mgrCreateVSSDeviceAndCreateMemberGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.mgrCreateVSSDeviceAndUpdateMemberGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.updateDAISManagerDeviceIdGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.updateVSSDeviceAndDeleteMemberGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTaskTestData.updateVSSDeviceGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.test.InMemDB;
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

    public EntityManager em;

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    public MgrCheckDevicesMetaTaskTest(VirtualSystem vs, TaskGraph tg) {
        this.vs = vs;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        ManagerDeviceApi mgrDeviceGroupSupportedApi = mock(ManagerDeviceApi.class);
        when(mgrDeviceGroupSupportedApi.isDeviceGroupSupported()).thenReturn(true);

        ManagerDeviceApi mgrDeviceNoDaiApi = mock(ManagerDeviceApi.class);
        when(mgrDeviceNoDaiApi.isDeviceGroupSupported()).thenReturn(true);

        ManagerDeviceApi mgrDeviceGroupNotSupportedApi = mock(ManagerDeviceApi.class);
        when(mgrDeviceGroupNotSupportedApi.isDeviceGroupSupported()).thenReturn(false);

        Mockito.<List<? extends ManagerDeviceMemberElement>>when(mgrDeviceNoDaiApi.listDeviceMembers())
                .thenReturn(Arrays.asList(MGR_DEVICE_MEMBER_ELEMENT_NO_DAI));

        Mockito.<List<? extends ManagerDeviceMemberElement>>when(mgrDeviceGroupSupportedApi.listDeviceMembers())
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

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {

        this.em.getTransaction().begin();

        this.em.persist(this.vs.getVirtualizationConnector());
        this.em.persist(this.vs.getApplianceSoftwareVersion().getAppliance());
        this.em.persist(this.vs.getApplianceSoftwareVersion());
        this.em.persist(this.vs.getDistributedAppliance().getApplianceManagerConnector());
        this.em.persist(this.vs.getDistributedAppliance());
        this.em.persist(this.vs.getDomain());
        this.em.persist(this.vs);

        DistributedApplianceInstance dai = new DistributedApplianceInstance(this.vs);
        dai.setName(MGR_DEVICE_MEMBER_ELEMENT_WITH_DAI.getName());
        dai.setIpAddress("REQUEST_WITHOUT_NAME_IP");
        dai.setApplianceConfig(new byte[3]);

        this.em.persist(dai);

        this.em.getTransaction().commit();

    }

    @Test
    public void testExecuteTransaction_WithVariousVirtualSystem_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        MgrCheckDevicesMetaTask task = new MgrCheckDevicesMetaTask(this.vs);

        // Act.
        task.executeTransaction(this.em);

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
