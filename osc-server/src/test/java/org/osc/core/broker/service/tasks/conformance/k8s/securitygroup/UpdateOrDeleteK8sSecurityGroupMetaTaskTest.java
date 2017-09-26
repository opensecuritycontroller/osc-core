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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import static org.osc.core.broker.service.tasks.conformance.k8s.securitygroup.UpdateOrDeleteK8sSecurityGroupMetaTaskTestData.*;

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
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.DeleteSecurityGroupFromDbTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.PortGroupCheckMetaTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class UpdateOrDeleteK8sSecurityGroupMetaTaskTest {
    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    UpdateOrDeleteK8sSecurityGroupMetaTask factoryTask;

    private SecurityGroup sg;

    private TaskGraph expectedGraph;

    public UpdateOrDeleteK8sSecurityGroupMetaTaskTest(
            SecurityGroup sg,
            TaskGraph expectedGraph) {
        this.sg = sg;
        this.expectedGraph = expectedGraph;
    }

    @Before
    public void testInitialize() throws Exception {
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

    @Test
    public void testExecute_WithVariousSecurityGroups_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        this.factoryTask.checkK8sSecurityGroupLabelMetaTask = new CheckK8sSecurityGroupLabelMetaTask();
        this.factoryTask.portGroupCheckMetaTask = new PortGroupCheckMetaTask();
        this.factoryTask.deleteSecurityGroupFromDbTask = new DeleteSecurityGroupFromDbTask();
        UpdateOrDeleteK8sSecurityGroupMetaTask task = this.factoryTask.create(this.sg);

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            { NO_LABEL_SG, createK8sGraph(NO_LABEL_SG, false) },
            { SINGLE_LABEL_SG, createK8sGraph(SINGLE_LABEL_SG, false) },
            { MULTI_LABEL_SG, createK8sGraph(MULTI_LABEL_SG, false) },
            { SINGLE_LABEL_MARKED_FOR_DELETION_SG, deleteSGK8sGraph(SINGLE_LABEL_MARKED_FOR_DELETION_SG, true) }
        });
    }

    private void populateDatabase() {
        persistObjects(this.em, this.sg);
    }
}
