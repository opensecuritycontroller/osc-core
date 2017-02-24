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
