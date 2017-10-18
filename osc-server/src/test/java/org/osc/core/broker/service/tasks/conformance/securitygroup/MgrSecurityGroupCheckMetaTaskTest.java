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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import static org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTaskTestData.*;

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
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupElement;

@RunWith(value = Parameterized.class)
public class MgrSecurityGroupCheckMetaTaskTest {
    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    ManagerSecurityGroupApi mgrSgApi;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @InjectMocks
    MgrSecurityGroupCheckMetaTask factoryTask;

    private SecurityGroup sg;

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    public MgrSecurityGroupCheckMetaTaskTest(
            SecurityGroup sg,
            VirtualSystem vs,
            TaskGraph expectedGraph) {
        this.sg = sg;
        this.vs = vs;
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

        registerMgrSecurityGroup(this.sg, this.vs, MGR_SG_ELEMENT);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    @Test
    public void testExecute_WithVariousSecurityGroups_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        this.factoryTask.createMgrSecurityGroupTask = new CreateMgrSecurityGroupTask();
        this.factoryTask.updateMgrSecurityGroupTask = new UpdateMgrSecurityGroupTask();
        this.factoryTask.deleteMgrSecurityGroupTask = new DeleteMgrSecurityGroupTask();

        MgrSecurityGroupCheckMetaTask task = this.factoryTask.create(this.vs, this.sg);

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            { NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION,
                NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION_VS,
                deleteMgrSecGroupGraph(NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION_VS, MGR_SG_ELEMENT) },
        });
    }

    private void registerMgrSecurityGroup(SecurityGroup sg, VirtualSystem vs, ManagerSecurityGroupElement mgrSecGroupElement) throws Exception {
        Mockito.when(this.mgrSgApi.getSecurityGroupById(sg.getSecurityGroupInterfaces().iterator().next().getMgrSecurityGroupId())).thenReturn(mgrSecGroupElement);
        Mockito.when(this.apiFactoryServiceMock.createManagerSecurityGroupApi(vs)).thenReturn(this.mgrSgApi);
    }

    private void populateDatabase() {
        persistObjects(this.em, this.sg);
    }
}
