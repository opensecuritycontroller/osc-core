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

import static org.osc.core.broker.service.tasks.conformance.k8s.securitygroup.CheckK8sSecurityGroupLabelMetaTaskTestData.*;

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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberDeleteTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MarkSecurityGroupMemberDeleteTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class CheckK8sSecurityGroupLabelMetaTaskTest {

    private EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private CheckK8sSecurityGroupLabelMetaTask factoryTask;

    private TaskGraph expectedGraph;
    private SecurityGroupMember sgm;
    private boolean isDelete;

    public CheckK8sSecurityGroupLabelMetaTaskTest(SecurityGroupMember sgm, boolean isDelete) {
        this.sgm = sgm;
        this.isDelete = isDelete;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);
        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
        persist(this.sgm, this.em);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    @Test
    public void testExecute_WithVariousSGM_ExpectCorrectTaskGraph() throws Exception {

        // Arrange.
        CheckK8sSecurityGroupLabelMetaTask task =
                this.factoryTask.create(this.sgm, this.isDelete);
        UpdateK8sSecurityGroupMemberLabelMetaTask updateK8sSecurityGroupMemberLabelMetaTask =
                new UpdateK8sSecurityGroupMemberLabelMetaTask();
        MarkSecurityGroupMemberDeleteTask markSecurityGroupMemberDeleteTask =
                new MarkSecurityGroupMemberDeleteTask();
        SecurityGroupMemberDeleteTask securityGroupMemberDeleteTask =
                new SecurityGroupMemberDeleteTask();

        task.markSecurityGroupMemberDeleteTask = markSecurityGroupMemberDeleteTask;
        task.securityGroupMemberDeleteTask = securityGroupMemberDeleteTask;
        task.updateK8sSecurityGroupMemberLabelMetaTask = updateK8sSecurityGroupMemberLabelMetaTask;

        this.expectedGraph = new TaskGraph();
        if (!this.isDelete) {
            this.expectedGraph.addTask(updateK8sSecurityGroupMemberLabelMetaTask.create(this.sgm));
        } else {
            this.expectedGraph.addTask(markSecurityGroupMemberDeleteTask.create(this.sgm));
            this.expectedGraph.appendTask(securityGroupMemberDeleteTask.create(this.sgm));
        }

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            { CREATED_SGM, false },
            { DELETED_SGM, true },
        });
    }
}
