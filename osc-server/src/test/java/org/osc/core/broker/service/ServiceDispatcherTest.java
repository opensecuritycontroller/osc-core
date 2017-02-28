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
package org.osc.core.broker.service;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;

public class ServiceDispatcherTest {

    private SessionFactory mockedSessionFactory;
    private Session mockedSession;
    private Transaction mockedTransaction;

    @Before
    public void setUp() {
        this.mockedSessionFactory = Mockito.mock(SessionFactory.class);
        this.mockedSession = Mockito.mock(Session.class);
        this.mockedTransaction = Mockito.mock(Transaction.class);
        Mockito.when(this.mockedSessionFactory.openSession()).thenReturn(this.mockedSession);
        Mockito.when(this.mockedSession.beginTransaction()).thenReturn(this.mockedTransaction);
    }

    @Test
    public void testExecuteValidRequest() throws Exception {
        ServiceDispatcher<?, ?> mockServiceDispatcher = new ServiceDispatcher<Request, Response>() {

            @Override
            public Response exec(Request request, Session session) throws Exception {
                return null;
            }

            @Override
            protected SessionFactory getSessionFactory() {
                return ServiceDispatcherTest.this.mockedSessionFactory;
            }

        };
        mockServiceDispatcher.dispatch(null);
        Mockito.verify(this.mockedTransaction).commit();
    }

    @Test(expected = Exception.class)
    public void testExecuteInvalidRequest() throws Exception {
        ServiceDispatcher<?, ?> mockServiceDispatcher = new ServiceDispatcher<Request, Response>() {

            @Override
            protected SessionFactory getSessionFactory() {
                return ServiceDispatcherTest.this.mockedSessionFactory;
            }

            @Override
            public Response exec(Request request, Session session) throws Exception {
                throw new Exception("");
            }

        };
        mockServiceDispatcher.dispatch(null);
        Mockito.verify(this.mockedTransaction).rollback();
    }

}
