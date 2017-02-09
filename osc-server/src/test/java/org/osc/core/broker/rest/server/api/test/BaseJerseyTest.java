package org.osc.core.broker.rest.server.api.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.validation.ValidationConfig;
import org.glassfish.jersey.server.validation.internal.InjectingConstraintValidatorFactory;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.di.OSCTestFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.ServiceMocker;
import org.osc.core.broker.util.api.ApiUtil;

import javax.validation.ParameterNameProvider;
import javax.validation.Validation;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

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
                .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(ValidationConfigurationContextResolver.class)
                .property(ServerProperties.BV_FEATURE_DISABLE, false)
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

    public static class ValidationConfigurationContextResolver implements ContextResolver<ValidationConfig> {

        @Context
        private ResourceContext resourceContext;

        @Override
        public ValidationConfig getContext(final Class<?> type) {
            return new ValidationConfig()
                    .constraintValidatorFactory(resourceContext.getResource(InjectingConstraintValidatorFactory.class))
                    .parameterNameProvider(new CustomParameterNameProvider());
        }

        /**
         * See ContactCardTest#testAddInvalidContact.
         */
        private class CustomParameterNameProvider implements ParameterNameProvider {

            private final ParameterNameProvider nameProvider;

            public CustomParameterNameProvider() {
                nameProvider = Validation.byDefaultProvider().configure().getDefaultParameterNameProvider();
            }

            @Override
            public List<String> getParameterNames(final Constructor<?> constructor) {
                return nameProvider.getParameterNames(constructor);
            }

            @Override
            public List<String> getParameterNames(final Method method) {
                // See ContactCardTest#testAddInvalidContact.
                if ("addContact".equals(method.getName())) {
                    return Arrays.asList("contact");
                }
                return nameProvider.getParameterNames(method);
            }
        }
    }
}

