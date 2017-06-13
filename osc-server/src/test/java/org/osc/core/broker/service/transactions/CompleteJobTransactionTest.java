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
package org.osc.core.broker.service.transactions;

import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HibernateUtil.class)
public class CompleteJobTransactionTest {
    @SuppressWarnings("serial")
    private class TestEntityWithLastJob extends BaseEntity implements LastJobContainer {
        @Override
        public void setLastJob(JobRecord lastJob) { }
        @Override
        public JobRecord getLastJob() {
            return null;
        }
    }

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    private EntityManager em;
    private EntityTransaction tx;
    private long existingEntityId = 1;
    private long existingJobId = 2;
    private long missingEntityId = 3;
    private long missingJobId = 4;

    private CompleteJobTransaction<TestEntityWithLastJob> target;

    private TestEntityWithLastJob testEntity;
    private JobRecord testLastJobRecord;

    private void init() throws Exception {
        this.em = mock(EntityManager.class);
        this.tx = mock(EntityTransaction.class);

        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);

        PowerMockito.mockStatic(HibernateUtil.class);
        Mockito.when(HibernateUtil.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(HibernateUtil.getTransactionControl()).thenReturn(this.txControl);

        this.txControl.setEntityManager(this.em);

        this.target= new CompleteJobTransaction<TestEntityWithLastJob>(TestEntityWithLastJob.class);
        this.testEntity = mock(TestEntityWithLastJob.class);
        this.testLastJobRecord = new JobRecord();

        when(this.em.find(TestEntityWithLastJob.class, this.existingEntityId)).thenReturn(this.testEntity);
        when(this.em.find(JobRecord.class, this.existingJobId)).thenReturn(this.testLastJobRecord);
        when(this.em.find(TestEntityWithLastJob.class, this.missingEntityId)).thenReturn(null);
        when(this.em.find(JobRecord.class, this.missingJobId)).thenReturn(null);
    }

    @Before
    public void setUp() throws Exception {
        init();
    }

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    public void testRun_WithValidInput_UpdatesEntityProperly() throws Exception {
        // Act.
        this.txControl.required(() ->
        this.target.run(this.em,new CompleteJobTransactionInput(this.existingEntityId, this.existingJobId)));

        // Assert.
        verify(this.testEntity, times(1)).setLastJob(this.testLastJobRecord);
    }

    @Test
    public void testRun_WithInvalidEntityId_ThrowsIllegalArgumentException() throws Exception {
        // Assert.
        this.expectException.expect(IllegalArgumentException.class);

        // Act.
        this.target.run(this.em,new CompleteJobTransactionInput(this.missingEntityId, this.existingJobId));
    }

    @Test
    public void testRun_WithInvalidJobId_ThrowsIllegalArgumentException() throws Exception {
        // Assert.
        this.expectException.expect(IllegalArgumentException.class);

        // Act.
        this.target.run(this.em,new CompleteJobTransactionInput(this.existingEntityId, this.missingJobId));
    }
}