package org.osc.core.broker.service;

import org.hibernate.SessionFactory;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ServiceMocker {
    public static void mockSessionFactory(ServiceDispatcher service, SessionFactory sessionFactoryMock) {
        Mockito.when(service.getSessionFactory()).thenReturn(sessionFactoryMock);
    }

    public static void callRealExec(ServiceDispatcher service) throws Exception {
        when(service.dispatch(any())).thenCallRealMethod();
        when(service.exec(any(), any())).thenCallRealMethod();
    }
}
