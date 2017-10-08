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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import static org.mockito.Mockito.*;
import static org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberHookCheckTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierCreateTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierDeleteTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierUpdateTask;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class SecurityGroupMemberHookCheckTaskTest {

    @Mock
    private EntityManager em;

    @Mock
    private EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    private DBConnectionManager dbMgr;

    private SecurityGroupMember sgm;

    private TaskGraph expectedGraph;

    private SecurityGroupMemberHookCheckTaskTestData data;

    public SecurityGroupMemberHookCheckTaskTest(SecurityGroupMember sgm, VmDiscoveryCache vdc, TaskGraph expected,
            SecurityGroupMemberHookCheckTaskTestData data) {
        this.sgm = sgm;
        this.expectedGraph = expected;
        this.data = data;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        when(this.em.find(SecurityGroupMember.class, this.sgm.getId())).thenReturn(this.sgm);
    }


    @Test
    public void testExecuteTransaction_WithSfcSgm_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        SecurityGroupMemberHookCheckTask task = new SecurityGroupMemberHookCheckTask();
        task.vmPortDeleteFromDbTask = new VmPortDeleteFromDbTask();
        task.sfcFlowClassifierCreateTask = new SfcFlowClassifierCreateTask();
        task.sfcFlowClassifierUpdateTask = new SfcFlowClassifierUpdateTask();
        task.sfcFlowClassifierDeleteTask = new SfcFlowClassifierDeleteTask();

        task = task.create(this.sgm, null);
        task.apiFactoryService = this.data.getApiFactoryServiceMock();

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {

        SecurityGroupMemberHookCheckTaskTestData sfcWithPortsData = new SecurityGroupMemberHookCheckTaskTestData();
        SecurityGroupMemberHookCheckTaskTestData sfcWithDeletedPortsData = new SecurityGroupMemberHookCheckTaskTestData();
        SecurityGroupMemberHookCheckTaskTestData sfcWithMixedPortsData = new SecurityGroupMemberHookCheckTaskTestData();
        SecurityGroupMemberHookCheckTaskTestData sfcWithMismatchPortsData = new SecurityGroupMemberHookCheckTaskTestData();
        SecurityGroupMemberHookCheckTaskTestData sfcWithSgmUnbinded = new SecurityGroupMemberHookCheckTaskTestData();
        SecurityGroupMemberHookCheckTaskTestData sfcWithSgmUnbindedPortsUnbinded = new SecurityGroupMemberHookCheckTaskTestData();

        return Arrays.asList(
                new Object[][] {
                    { SFC_SGM_WITH_NO_PORTS, null, new TaskGraph(), new SecurityGroupMemberHookCheckTaskTestData() },
                    { SFC_SGM_WITH_PORTS, null, sfcWithPortsData.getSfcWithPortsTaskGraph(SFC_SGM_WITH_PORTS), sfcWithPortsData },
                    { SFC_SGM_WITH_DELETED_PORTS, null, sfcWithDeletedPortsData.getSfcWithDeletedPortsTaskGraph(SFC_SGM_WITH_DELETED_PORTS), sfcWithDeletedPortsData },
                    { SFC_SGM_WITH_MIXED_PORTS, null, sfcWithMixedPortsData.getSfcWithMixedPortsTaskGraph(SFC_SGM_WITH_MIXED_PORTS), sfcWithMixedPortsData },
                    { SFC_SGM_WITH_PORTS, null, sfcWithMismatchPortsData.getSfcMismatchTaskGraph(SFC_SGM_WITH_PORTS), sfcWithMismatchPortsData },
                    { SFC_SGM_UNBINDED_WITH_PORTS, null, sfcWithSgmUnbinded.getSfcUnbindedTaskGraph(SFC_SGM_UNBINDED_WITH_PORTS), sfcWithSgmUnbinded },
                    { SFC_SGM_UNBINDED_WITH_PORTS_UNBINDED, null, new TaskGraph(), sfcWithSgmUnbindedPortsUnbinded },
                    });
    }
}
