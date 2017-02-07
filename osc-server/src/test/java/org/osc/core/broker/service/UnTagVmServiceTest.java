package org.osc.core.broker.service;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.di.OSCTestFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

public class UnTagVmServiceTest extends BaseTagVmServiceTest {

    @InjectMocks
    UnTagVmService unTagVmService;

    @BeforeClass
    public static void configure(){
        OSC.setFactory(new OSCTestFactory());
    }

    @Test
    public void testExec_WithRequestWithoutVmUuid_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid VM Uuid.");

        // Act.
        this.unTagVmService.exec(REQUEST_WITH_TAG, this.session);
    }

    @Test
    public void testExec_WithRequestWithVmUuidWithoutVm_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("VM with Uuid '" + INVALID_VM_UUID + "' not found.");

        // Act.
        this.unTagVmService.exec(REQUEST_WITH_TAG_INVALID_VM_UUID, this.session);
    }

    @Test
    public void testExec_WithValidRequest_ExpectsSuccess() throws Exception {
        // Act.
        this.unTagVmService.exec(REQUEST_WITH_TAG_AND_VM_UUID, this.session);

        // Assert.
        Mockito.verify(this.securityTagApi, Mockito.times(1)).removeSecurityTagFromVM(REQUEST_WITH_TAG_AND_VM_UUID.getVmUuid(), BaseTagVmService.DEFAULT_OSC_SECURITY_TAG);
    }
}