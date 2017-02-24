package org.osc.core.broker.service.transactions;

import static org.mockito.Mockito.*;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;

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

    private Session sessionMock;
    private long existingEntityId = 1;
    private long existingJobId = 2;
    private long missingEntityId = 3;
    private long missingJobId = 4;

    private CompleteJobTransaction<TestEntityWithLastJob> target;

    private TestEntityWithLastJob testEntity;
    private JobRecord testLastJobRecord;

    private void init() {
        this.sessionMock = mock(Session.class);
        this.target= new CompleteJobTransaction<TestEntityWithLastJob>(TestEntityWithLastJob.class);
        this.testEntity = mock(TestEntityWithLastJob.class);
        this.testLastJobRecord = new JobRecord();

        when(this.sessionMock.get(TestEntityWithLastJob.class, this.existingEntityId)).thenReturn(this.testEntity);
        when(this.sessionMock.get(JobRecord.class, this.existingJobId)).thenReturn(this.testLastJobRecord);
        when(this.sessionMock.get(TestEntityWithLastJob.class, this.missingEntityId)).thenReturn(null);
        when(this.sessionMock.get(JobRecord.class, this.missingJobId)).thenReturn(null);
    }

    @Before
    public void setUp() {
        init();
    }

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    public void testRun_WithValidInput_UpdatesEntityProperly() throws Exception {
        // Act.
        this.target.run(this.sessionMock,new CompleteJobTransactionInput(this.existingEntityId, this.existingJobId));

        // Assert.
        verify(this.testEntity, times(1)).setLastJob(this.testLastJobRecord);
    }

    @Test
    public void testRun_WithInvalidEntityId_ThrowsIllegalArgumentException() throws Exception {
        // Assert.
        this.expectException.expect(IllegalArgumentException.class);

        // Act.
        this.target.run(this.sessionMock,new CompleteJobTransactionInput(this.missingEntityId, this.existingJobId));
    }

    @Test
    public void testRun_WithInvalidJobId_ThrowsIllegalArgumentException() throws Exception {
        // Assert.
        this.expectException.expect(IllegalArgumentException.class);

        // Act.
        this.target.run(this.sessionMock,new CompleteJobTransactionInput(this.existingEntityId, this.missingJobId));
    }
}