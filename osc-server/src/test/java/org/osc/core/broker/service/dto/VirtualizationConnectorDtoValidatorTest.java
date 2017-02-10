package org.osc.core.broker.service.dto;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.VirtualizationConnectorDtoValidator;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.osc.core.broker.util.SessionStub;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SdnControllerApiFactory.class, VirtualizationConnectorEntityMgr.class, 
	VirtualizationConnectorUtil.class })
public class VirtualizationConnectorDtoValidatorTest {

    @Mock
    private Session sessionMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private VirtualizationConnectorDtoValidator dtoValidator;

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

    @Test
    public void testValidate_WhenVcRequest_ReturnsSuccessful() throws Exception {
        	        
        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.VMWARE_REQUEST.getDto());
        
        //Assert
        Assert.assertTrue(true);
    }
    
    @Test
    public void testValidate_WhenVcNameExistsRequest_ThrowsValidationException() throws Exception {
    	// Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtualization Connector Name: " + VirtualizationConnectorServiceData.VMWARE_NAME_ALREADY_EXISTS_REQUEST.getDto().getName() + " already exists.");
        
        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.VMWARE_NAME_ALREADY_EXISTS_REQUEST.getDto());
        
        
    }
    
    @Test
    public void testValidate_WhenControllerIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Controller IP Address: " + VirtualizationConnectorServiceData.CONTROLLER_IP_ALREADY_EXISTS_VMWARE_REQUEST.getDto().getControllerIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.CONTROLLER_IP_ALREADY_EXISTS_VMWARE_REQUEST.getDto());
    }
    
    @Test
    public void testValidate_WhenVmwareProviderIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Provider IP Address: " + VirtualizationConnectorServiceData.PROVIDER_IP_ALREADY_EXISTS_VMWARE_REQUEST.getDto().getProviderIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.PROVIDER_IP_ALREADY_EXISTS_VMWARE_REQUEST.getDto());
    }    

}