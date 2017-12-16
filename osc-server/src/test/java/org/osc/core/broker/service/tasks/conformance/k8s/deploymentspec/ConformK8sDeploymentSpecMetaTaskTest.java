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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import static org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec.ConformK8sDeploymentSpecMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DeleteDSFromDbTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(Parameterized.class)
public class ConformK8sDeploymentSpecMetaTaskTest {
    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    ConformK8sDeploymentSpecMetaTask factoryTask;

    private DeploymentSpec ds;

    private TaskGraph expectedGraph;

    public ConformK8sDeploymentSpecMetaTaskTest(DeploymentSpec ds, TaskGraph tg) {
        this.ds = ds;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        populateDatabase();
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        this.em.persist(this.ds.getVirtualSystem()
                .getVirtualizationConnector());
        this.em.persist(this.ds.getVirtualSystem()
                .getDistributedAppliance().getApplianceManagerConnector());
        this.em.persist(this.ds.getVirtualSystem()
                .getDistributedAppliance().getAppliance());
        this.em.persist(this.ds.getVirtualSystem().getDistributedAppliance());
        this.em.persist(this.ds.getVirtualSystem().getApplianceSoftwareVersion());
        this.em.persist(this.ds.getVirtualSystem().getDomain());
        this.em.persist(this.ds.getVirtualSystem());
        for(DistributedApplianceInstance dai : this.ds.getVirtualSystem().getDistributedApplianceInstances()) {
            this.em.persist(dai);
        }

        this.em.persist(this.ds);

        this.em.getTransaction().commit();

    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        this.factoryTask.deleteDSFromDbTask = new DeleteDSFromDbTask();
        this.factoryTask.mgrCheckDevicesMetaTask = new MgrCheckDevicesMetaTask();
        this.factoryTask.deleteK8sDAIInspectionPortTask = new DeleteK8sDAIInspectionPortTask();
        this.factoryTask.createOrUpdateK8sDeploymentSpecMetaTask = new CreateOrUpdateK8sDeploymentSpecMetaTask();
        this.factoryTask.deleteK8sDeploymentTask = new DeleteK8sDeploymentTask();

        ConformK8sDeploymentSpecMetaTask task = this.factoryTask.create(this.ds);

        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {DELETE_DS_MARKED_FOR_DELETION, deleteDSGraph(DELETE_DS_MARKED_FOR_DELETION)},
            {DELETE_DS_VS_MARKED_FOR_DELETION, deleteDSGraph(DELETE_DS_VS_MARKED_FOR_DELETION)},
            {DELETE_DS_DA_MARKED_FOR_DELETION, deleteDSGraph(DELETE_DS_DA_MARKED_FOR_DELETION)},
            {DELETE_DS_MARKED_DELETION_WITH_DAIS, deleteDSGraph(DELETE_DS_MARKED_DELETION_WITH_DAIS)},
            {UPDATE_DS, updateDSGraph(UPDATE_DS)},
        });
    }
}
