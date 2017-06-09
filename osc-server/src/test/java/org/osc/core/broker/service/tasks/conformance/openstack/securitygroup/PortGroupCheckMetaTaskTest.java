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

import static org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.PortGroupCheckMetaTaskTestData.*;

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
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({HibernateUtil.class})
public class PortGroupCheckMetaTaskTest {
    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @InjectMocks
    PortGroupCheckMetaTask factoryTask;

    @Mock
    DBConnectionManager dbMgr;

    private SecurityGroup sg;

    private TaskGraph expectedGraph;

    private boolean isDeleteTg;

    private String domainId;

    public PortGroupCheckMetaTaskTest(SecurityGroup sg, boolean isDeleteTg, String domainId, TaskGraph expectedGraph) {
        this.sg = sg;
        this.isDeleteTg = isDeleteTg;
        this.domainId = domainId;
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
    public void testExecuteTransaction_WithVariousSecurityGroups_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        this.factoryTask.createPortGroupTask = new CreatePortGroupTask();
        this.factoryTask.updatePortGroupTask = new UpdatePortGroupTask();
        this.factoryTask.deletePortGroupTask = new DeletePortGroupTask();

        PortGroupCheckMetaTask task = this.factoryTask.create(this.sg, this.isDeleteTg, this.domainId);

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {SG_WITHOUT_NET_ELEMENT_ID, false, null, createPortGroupGraph(SG_WITHOUT_NET_ELEMENT_ID)},
            {SG_WITH_NET_ELEMENT_ID_FOR_DELETE, true, SG_WITH_NET_ELEMENT_ID_FOR_DELETE.getName(), deletePortGroupGraph(SG_WITH_NET_ELEMENT_ID_FOR_DELETE, SG_WITH_NET_ELEMENT_ID_FOR_DELETE.getName())},
            {SG_WITH_NET_ELEMENT_ID_FOR_UPDATE, false, SG_WITH_NET_ELEMENT_ID_FOR_UPDATE.getName(), updatePortGroupGraph(SG_WITH_NET_ELEMENT_ID_FOR_UPDATE, SG_WITH_NET_ELEMENT_ID_FOR_UPDATE.getName())},
        });
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        this.em.persist(this.sg.getVirtualizationConnector());

        this.em.persist(this.sg);

        this.em.getTransaction().commit();
    }
}

