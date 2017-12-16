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

import static org.junit.Assert.assertTrue;
import static org.osc.core.broker.service.tasks.conformance.securitygroup.MarkSecurityGroupMemberDeleteTaskTestData.*;

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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class MarkSecurityGroupMemberDeleteTaskTest {

    private EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    MarkSecurityGroupMemberDeleteTask factoryTask;

    private SecurityGroupMember sgm;

    public MarkSecurityGroupMemberDeleteTaskTest(SecurityGroupMember sgm) {
        this.sgm = sgm;
    }

    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);
        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);
        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
        persist(this.sgm, this.em);
    }

    @After
    public void tearDown() {
        InMemDB.shutdown();
    }

    @Test
    public void testExecute_WithVariousSGM_ExpectMarkedInTheEnd() throws Exception {
        // Arrange.
        MarkSecurityGroupMemberDeleteTask task = this.factoryTask.create(this.sgm);

        // Act.
        task.execute();

        // Assert.
        SecurityGroupMember sgm = this.txControl.required(() -> this.em.find(SecurityGroupMember.class, this.sgm.getId()));
        assertTrue(sgm.getMarkedForDeletion());
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            { NOT_YET_MARKED_FOR_DELETE_SGM },
            { ALREADY_MARKED_FOR_DELETE_SGM }
        });
    }
}
