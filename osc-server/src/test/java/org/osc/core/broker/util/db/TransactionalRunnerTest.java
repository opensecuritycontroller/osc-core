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
package org.osc.core.broker.util.db;

import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

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

    private EntityManager em;
    private EntityTransaction transactionMock;
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
        this.transactionMock = mock(EntityTransaction.class);
        this.em = mock(EntityManager.class);
        this.sessionHandlerMock = mock(SessionHandler.class);

        when(this.em.getTransaction()).thenReturn(this.transactionMock);
        when(this.sessionHandlerMock.getEntityManager()).thenReturn(this.em);

        this.target = new TransactionalRunner<>(this.sessionHandlerMock);
        this.logicMock = mock(TransactionalAction.class);
        this.failingLogicMock = mock(TransactionalAction.class);
        when(this.failingLogicMock.run(this.em, null)).thenThrow(Exception.class);

        this.givenRequest = new TestRequestClass();
        this.expectedResponse = new TestResponseClass();

        when(this.logicMock.run(this.em, this.givenRequest)).thenReturn(this.expectedResponse);
        when(this.logicMock.run(this.em, null)).thenReturn(this.expectedResponse);

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
        verify(this.logicMock, times(1)).run(this.em, null);
        // expected outputReturned
        Assert.assertEquals(this.expectedResponse, actualResponse);
    }

    @Test
    public void testExec_WithValidParametrizedTransactionalLogic_TransactionalLogicCalled() throws Exception {
        // Act.
        TestResponseClass actualResponse = this.target.exec(this.logicMock, this.givenRequest);

        // Assert.
        // transaction logic called
        verify(this.logicMock, times(1)).run(this.em, this.givenRequest);
        // expected outputReturned
        Assert.assertEquals(this.expectedResponse, actualResponse);
    }

    @Test
    public void testExec_WithValidTransactionalLogic_ExpectsSessionRecycled() throws Exception {
        // Act.
        this.target.exec(this.logicMock);

        // Assert.
        // session obtained once
        verify(this.sessionHandlerMock, times(1)).getEntityManager();
        // session recycled once
        verify(this.sessionHandlerMock, times(1)).closeSession(this.em);
    }

    @Test
    public void testExec_WithValidTransactionalLogic_ExpectsTransactionCommit() {
        // Act.
        this.target.exec(this.logicMock);

        // Assert.
        // transaction opened
        verify(this.transactionMock, times(1)).begin();
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
        verify(this.listenerMock, times(1)).afterCommit(this.em);
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
        verify(this.listenerMock, times(1)).afterRollback(this.em);
        // no "after commit" event invocation
        verify(this.listenerMock, times(0)).afterCommit(this.em);
    }

    @Test
    public void testExec_WithFailingLogic_ExpectsSessionRecycled() throws Exception {
        // Act.
        this.target.exec(this.failingLogicMock);

        // Assert.
        // "after rollback" event invoked
        verify(this.sessionHandlerMock, times(1)).getEntityManager();
        // no "after commit" event invocation
        verify(this.sessionHandlerMock, times(1)).closeSession(this.em);
    }

    @Test
    public void testExec_WithFailingLogicAndErrorHandlerAttached_HandlesException() throws Exception {
        // Act.
        this.target.withErrorHandling(this.errorHandlerMock).exec(this.failingLogicMock);

        // Assert.
        verify(this.errorHandlerMock, times(1)).handleError(Matchers.any(Exception.class));
    }
}