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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfaceCheckMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupInterfaceElement;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ ManagerApiFactory.class })
public class MgrSecurityGroupInterfaceCheckMetaTaskTest {
    @Mock
    public EntityManager em;

    @Mock
    ApiFactoryService afs;

    private VirtualSystem vs;
    private DistributedAppliance da;

    private TaskGraph expectedGraph;

    public MgrSecurityGroupInterfaceCheckMetaTaskTest(DistributedAppliance da, VirtualSystem vs, TaskGraph tg) {
        this.da = da;
        this.vs = vs;
        this.expectedGraph = tg;
    }

    private void createMgrApiWithSgi(ManagerSecurityGroupInterfaceElement mgrSgiElem, VirtualSystem vs)
            throws Exception {
        createMgrApiWithSgiList(Arrays.asList(mgrSgiElem), vs);
    }

    private void createMgrApiWithSgiList(List<ManagerSecurityGroupInterfaceElement> mgrSgiElemList,
            VirtualSystem vs) throws Exception {
        ManagerSecurityGroupInterfaceApi mgrSgiApi = mock(ManagerSecurityGroupInterfaceApi.class);
        Mockito.<List<? extends ManagerSecurityGroupInterfaceElement>>when(mgrSgiApi.listSecurityGroupInterfaces())
                .thenReturn(mgrSgiElemList);
        when(this.afs.createManagerSecurityGroupInterfaceApi(vs)).thenReturn(mgrSgiApi);
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        for (VirtualSystem vs : TEST_VIRTUAL_SYSTEMS) {
            Mockito.when(this.em.find(VirtualSystem.class, vs.getId())).thenReturn(vs);
        }

        for (DistributedAppliance da : TEST_DISTRIBUTED_APPLIANCE) {
            Mockito.when(this.em.find(DistributedAppliance.class, da.getId())).thenReturn(da);
        }

        createMgrApiWithSgi(MGR_SGI, VS_WITH_SGI_AND_NO_MANAGER_SGI_VS);
        createMgrApiWithSgi(MGR_SGI_AND_SGI_WITH_SAME_NAME, VS_WITH_MANAGER_SGI_VS);
        createMgrApiWithSgi(MGR_SGI_TO_BE_DELETED, VS_WITH_MANAGER_SGI_TO_BE_DELETED_VS);
        createMgrApiWithSgiList(MGR_SGI_AND_MGR_SGI_TO_BE_DELETED_LIST, VS_WITH_SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI_AND_SGI_MARK_FOR_DELETION_VS_SGI);
        createMgrApiWithSgi(MGR_SGI, DA_WITH_SGI_AND_NO_MANAGER_SGI_DA.getVirtualSystems().iterator().next());
        createMgrApiWithSgi(MGR_SGI_AND_SGI_WITH_SAME_NAME, DA_WITH_MANAGER_SGI_DA.getVirtualSystems().iterator().next());
        createMgrApiWithSgi(MGR_SGI_TO_BE_DELETED, DA_WITH_MANAGER_SGI_TO_BE_DELETED_DA.getVirtualSystems().iterator().next());

    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        MgrSecurityGroupInterfacesCheckMetaTask task;
        if (this.da != null) {
            task = new MgrSecurityGroupInterfacesCheckMetaTask().create(this.da, null);
        } else {
            task = new MgrSecurityGroupInterfacesCheckMetaTask().create(this.vs);
        }
        task.apiFactoryService = this.afs;
        task.createMgrSecurityGroupInterfaceTask = new CreateMgrSecurityGroupInterfaceTask();
        task.deleteMgrSecurityGroupInterfaceTask = new DeleteMgrSecurityGroupInterfaceTask();
        task.updateMgrSecurityGroupInterfaceTask = new UpdateMgrSecurityGroupInterfaceTask();

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays
                .asList(new Object[][] {
                        { null, VS_WITH_SGI_AND_NO_MANAGER_SGI_VS,
                                createAndDeleteStaleMgrSecurityGroupInterfaceGraph(VS_WITH_SGI_AND_NO_MANAGER_SGI_VS,
                                        SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI, MGR_SGI) },
                        { null, VS_WITH_MANAGER_SGI_VS,
                                updateMgrSecurityGroupInterfaceGraph(VS_WITH_MANAGER_SGI_VS,
                                        SGI_NOT_MARK_FOR_DELETION_WITH_DIFFERENT_POLICY_ID_AND_TAG_VS_SGI) },
                        { null, VS_WITH_MANAGER_SGI_TO_BE_DELETED_VS,
                                deleteMgrSecurityGroupGraph(VS_WITH_MANAGER_SGI_TO_BE_DELETED_VS,
                                        MGR_SGI_TO_BE_DELETED) },
                        { null, VS_WITH_SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI_AND_SGI_MARK_FOR_DELETION_VS_SGI,
                                vsWithMultipleSgiGraph(
                                        VS_WITH_SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI_AND_SGI_MARK_FOR_DELETION_VS_SGI,
                                        SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI) },
                        { DA_WITH_SGI_AND_NO_MANAGER_SGI_DA, null,
                                createAndDeleteStaleMgrSecurityGroupInterfaceGraph(
                                        DA_WITH_SGI_AND_NO_MANAGER_SGI_DA.getVirtualSystems().iterator().next(),
                                        SGI_NOT_MARK_FOR_DELETION_WITH_ID_DA_SGI, MGR_SGI) },
                        { DA_WITH_MANAGER_SGI_DA, null,
                                updateMgrSecurityGroupInterfaceGraph(
                                        DA_WITH_MANAGER_SGI_DA.getVirtualSystems().iterator().next(),
                                        SGI_NOT_MARK_FOR_DELETION_WITH_DIFFERENT_POLICY_ID_AND_TAG_DA_SGI) },
                        { DA_WITH_MANAGER_SGI_TO_BE_DELETED_DA, null,
                                deleteMgrSecurityGroupGraph(
                                        DA_WITH_MANAGER_SGI_TO_BE_DELETED_DA.getVirtualSystems().iterator().next(),
                                        MGR_SGI_TO_BE_DELETED) }, });
    }

}
