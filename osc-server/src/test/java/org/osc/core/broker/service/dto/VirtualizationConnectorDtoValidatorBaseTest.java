package org.osc.core.broker.service.dto;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.request.VirtualizationConnectorDtoValidator;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.osc.core.broker.util.SessionStub;
import org.powermock.modules.junit4.PowerMockRunner;

import junitparams.JUnitParamsRunner;

/**
 * The base class for the {@link VirtualizationConnectorDtoValidator} unit tests.
 * The unit tests for {@link VirtualizationConnectorDtoValidator} have been split in two test classes.
 * The reason is because the runner {@link Parameterized} only supports data driven tests to be within the test class,
 * other non data driven tests need to go on a different test class.
 * We could optionally use the {@link JUnitParamsRunner}, which supports mixing data driven and non data driven
 * tests on the same class (as it was before) but this runner is not compatible with {@link PowerMockRunner} now needed for these tests.
 */
public class VirtualizationConnectorDtoValidatorBaseTest {

    @Mock
    private Session sessionMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    protected VirtualizationConnectorDtoValidator dtoValidator;

    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);
        
        SessionStub sessionStub = new SessionStub(this.sessionMock);

        sessionStub.stubIsExistingEntity(VirtualizationConnector.class, "name",
                VirtualizationConnectorServiceData.VMWARE_NAME_ALREADY_EXISTS, true);
        sessionStub.stubIsExistingEntity(VirtualizationConnector.class, "name",
                VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS, true);
        sessionStub.stubIsExistingEntity(VirtualizationConnector.class, "controllerIpAddress",
                VirtualizationConnectorServiceData.CONTROLLER_IP_ALREADY_EXISTS, true);
        sessionStub.stubIsExistingEntity(VirtualizationConnector.class, "providerIpAddress",
                VirtualizationConnectorServiceData.PROVIDER_IP_ALREADY_EXISTS, true);
        
    }
}
