package org.osc.core.broker.rest.server.api.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.di.OSCTestFactory;
import org.osc.core.broker.rest.server.exception.ConstraintViolationExceptionMapper;
import org.osc.core.broker.rest.server.exception.JsonProcessingExceptionMapper;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.ServiceMocker;
import org.osc.core.broker.util.api.ApiUtil;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseJerseyTest extends JerseyTest {

    protected OSCTestFactory testFactory;

    protected Session sessionMock;

    private SessionFactory sessionFactoryMock;

    public BaseJerseyTest() {
        sessionMock = mock(Session.class);
        sessionFactoryMock = mock(SessionFactory.class);
        when(sessionFactoryMock.openSession()).thenReturn(sessionMock);
    }

    protected void baseTestConfiguration(){
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        testFactory = new OSCTestFactory();
        OSC.setFactory(testFactory);
    }

    protected ResourceConfig getBaseResourceConfiguration() {
        return new ResourceConfig()
                .register(com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.class)
                .register(ConstraintViolationExceptionMapper.class)
                .register(JsonProcessingExceptionMapper.class)
                .packages("org.osc.core.broker.rest.server.exception");
    }

    protected String getString155(){
        StringBuilder sb = new StringBuilder();
        Arrays.stream(new int[155]).forEach(s -> sb.append("1"));
        return sb.toString();
    }

    protected void mockSessionFactory(ServiceDispatcher service) {
        ServiceMocker.mockSessionFactory(service, sessionFactoryMock);
    }

    protected void callRealMethods(ServiceDispatcher service) throws Exception {
        ServiceMocker.callRealExec(service);
    }

    protected void callRealMethods(ApiUtil apiUtil) throws Exception {
        when(apiUtil.submitBaseRequestToService(any(), any())).thenCallRealMethod();
        when(apiUtil.submitRequestToService(any(), any())).thenCallRealMethod();
    }
}
