package org.osc.core.broker.rest.server.model;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.SessionStub;

public class TagVmRequestValidatorTest {

    private static final String VALID_APPLIANCE_NAME = "appliance_name";
    private static final String INVALID_APPLIANCE_NAME = "other_applicance_name";

    private static final TagVmRequest VALID_REQUEST = createRequest(VALID_APPLIANCE_NAME);
    private static final TagVmRequest INVALID_REQUEST = createRequest(INVALID_APPLIANCE_NAME);

    private static final DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE = new DistributedApplianceInstance( createVirtualSystem(), AgentType.AGENT);

    @Mock
    Session session;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private TagVmRequestValidator validator;
    private SessionStub sessionStub;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.sessionStub = new SessionStub(this.session);

        this.validator = new TagVmRequestValidator(this.session);

        this.sessionStub.stubFindByFieldName("name", INVALID_APPLIANCE_NAME, null);
        this.sessionStub.stubFindByFieldName("name", VALID_APPLIANCE_NAME, DISTRIBUTED_APPLIANCE_INSTANCE);
    }

    private static VirtualSystem createVirtualSystem() {

        VirtualSystem virtualSystem = new VirtualSystem();
        virtualSystem.setId(1L);
        return virtualSystem;
    }

    @Test
    public void testValidate_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange.
        this.exception.expect(UnsupportedOperationException.class);

        // Act.
        this.validator.validate(VALID_REQUEST);
    }

    @Test
    public void testValidateAndLoad_WithNullRequest_ThrowsVmidcBrokerValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Null request or invalid Appliance Instance Name.");

        // Act.
        this.validator.validateAndLoad(null);
    }

    @Test
    public void testValidateAndLoad_WithNullSession_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);
        TagVmRequestValidator nullInitializedValidator = new TagVmRequestValidator(null);

        // Act.
        nullInitializedValidator.validateAndLoad(VALID_REQUEST);
    }

    @Test
    public void testExec_WhenDaiNotFound_ThrowsVmidcBrokerValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Appliance Instance Name '" + INVALID_APPLIANCE_NAME + "' not found.");

        // Act.
        this.validator.validateAndLoad(INVALID_REQUEST);
    }

    @Test
    public void testExec_WithValidRequest_ExpectsSuccess() throws Exception {
        // Act.
        DistributedApplianceInstance loadedDistributedApplianceInstance = this.validator.validateAndLoad(VALID_REQUEST);

        // Assert.
        Assert.assertEquals("The received DistributedApplianceInstance is different than expected.", DISTRIBUTED_APPLIANCE_INSTANCE, loadedDistributedApplianceInstance);
    }

    private static TagVmRequest createRequest(String applianceName) {
        TagVmRequest request = new TagVmRequest();
        request.setApplianceInstanceName(applianceName);
        return request;
    }
}