package org.osc.core.broker.util.db;

import static org.mockito.Mockito.*;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.osc.core.broker.util.db.TransactionalRunner.ErrorHandler;
import org.osc.core.broker.util.db.TransactionalRunner.SessionHandler;
import org.osc.core.broker.util.db.TransactionalRunner.TransactionalAction;
import org.osc.core.broker.util.db.TransactionalRunner.TransactionalListener;

public class TransactionalRunnerTest {
    private class TestResponseClass{}
    private class TestRequestClass{}

    private Session sessionMock;
    private Transaction transactionMock;
    private SessionHandler sessionHandlerMock;
    private TransactionalRunner<TestResponseClass, TestRequestClass> target;
    private TransactionalAction<TestResponseClass, TestRequestClass> logicMock;
    private TransactionalAction<TestResponseClass, TestRequestClass> failingLogicMock;
    private TestRequestClass givenRequest;
    private TestResponseClass expectedResponse;
    private TransactionalListener listenerMock;
    private ErrorHandler errorHandlerMock;

    @SuppressWarnings("unchecked")
    private void init() throws Exception {
        this.transactionMock = mock(Transaction.class);
        this.sessionMock = mock(Session.class);
        this.sessionHandlerMock = mock(SessionHandler.class);

        when(this.sessionMock.beginTransaction()).thenReturn(this.transactionMock);
        when(this.sessionHandlerMock.getSession()).thenReturn(this.sessionMock);

        this.target = new TransactionalRunner<>(this.sessionHandlerMock);
        this.logicMock = mock(TransactionalAction.class);
        this.failingLogicMock = mock(TransactionalAction.class);
        when(this.failingLogicMock.run(this.sessionMock, null)).thenThrow(Exception.class);

        this.givenRequest = new TestRequestClass();
        this.expectedResponse = new TestResponseClass();

        when(this.logicMock.run(this.sessionMock, this.givenRequest)).thenReturn(this.expectedResponse);
        when(this.logicMock.run(this.sessionMock, null)).thenReturn(this.expectedResponse);

        this.listenerMock = mock(TransactionalListener.class);

        this.errorHandlerMock = mock(ErrorHandler.class);

    }

    @Before
    public void setUp() throws Exception {
         init();
    }

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    public void testExec_WithValidParameterlessTransactionalLogic_TransactionalLogicCalled() throws Exception {
        // Act.
        TestResponseClass actualResponse = this.target.exec(this.logicMock);

        // Assert.
        // transaction logic called
        verify(this.logicMock, times(1)).run(this.sessionMock, null);
        // expected outputReturned
        Assert.assertEquals(this.expectedResponse, actualResponse);
    }

    @Test
    public void testExec_WithValidParametrizedTransactionalLogic_TransactionalLogicCalled() throws Exception {
        // Act.
        TestResponseClass actualResponse = this.target.exec(this.logicMock, this.givenRequest);

        // Assert.
        // transaction logic called
        verify(this.logicMock, times(1)).run(this.sessionMock, this.givenRequest);
        // expected outputReturned
        Assert.assertEquals(this.expectedResponse, actualResponse);
    }

    @Test
    public void testExec_WithValidTransactionalLogic_ExpectsSessionRecycled() {
        // Act.
        this.target.exec(this.logicMock);

        // Assert.
        // session obtained once
        verify(this.sessionHandlerMock, times(1)).getSession();
        // session recycled once
        verify(this.sessionHandlerMock, times(1)).closeSession(this.sessionMock);
    }

    @Test
    public void testExec_WithValidTransactionalLogic_ExpectsTransactionCommit() {
        // Act.
        this.target.exec(this.logicMock);

        // Assert.
        // transaction opened
        verify(this.sessionMock, times(1)).beginTransaction();
        // transaction closed
        verify(this.transactionMock, times(1)).commit();
        // no rollback called
        verify(this.transactionMock, times(0)).rollback();
    }

    @Test
    public void testExec_WithValidTransactionalLogic_ExpectsFilterAfterCommit() {
        // Act.
        this.target.withTransactionalListener(this.listenerMock).exec(this.logicMock);

        // Assert.
        // transaction opened
        verify(this.listenerMock, times(1)).afterCommit(this.sessionMock);
    }

    @Test
    public void testExec_WithFailingLogic_DoesntThrowException() throws Exception {
        // Act.
        TestResponseClass actualResponse = this.target.exec(this.failingLogicMock);

        // Assert.
        // no exceptions expected (defined by rule)
        Assert.assertEquals(null, actualResponse);
    }

    @Test
    public void testExec_WithFailingLogic_RollsBackTransaction() throws Exception {
        // Act.
        this.target.exec(this.failingLogicMock);

        // Assert.
        // no transaction commit
        verify(this.transactionMock, times(0)).commit();
        // transaction rolled back
        verify(this.transactionMock, times(1)).rollback();
    }

    @Test
    public void testExec_WithFailingLogic_ExpectsFilterAfterRollback() throws Exception {
        // Act.
        this.target.withTransactionalListener(this.listenerMock).exec(this.failingLogicMock);

        // Assert.
        // "after rollback" event invoked
        verify(this.listenerMock, times(1)).afterRollback(this.sessionMock);
        // no "after commit" event invocation
        verify(this.listenerMock, times(0)).afterCommit(this.sessionMock);
    }

    @Test
    public void testExec_WithFailingLogic_ExpectsSessionRecycled() throws Exception {
        // Act.
        this.target.exec(this.failingLogicMock);

        // Assert.
        // "after rollback" event invoked
        verify(this.sessionHandlerMock, times(1)).getSession();
        // no "after commit" event invocation
        verify(this.sessionHandlerMock, times(1)).closeSession(this.sessionMock);
    }

    @Test
    public void testExec_WithFailingLogicAndErrorHandlerAttached_HandlesException() throws Exception {
        // Act.
        this.target.withErrorHandling(this.errorHandlerMock).exec(this.failingLogicMock);

        // Assert.
        verify(this.errorHandlerMock, times(1)).handleError(Matchers.any(Exception.class));
    }
}