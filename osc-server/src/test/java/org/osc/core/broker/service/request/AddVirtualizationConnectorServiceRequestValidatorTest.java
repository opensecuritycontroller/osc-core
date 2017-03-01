package org.osc.core.broker.service.request;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.DtoValidator;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EncryptionUtil.class})
public class AddVirtualizationConnectorServiceRequestValidatorTest {

    @Mock
    private Session sessionMock;
    
    @Mock
    private DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> dtoValidator;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Mock
    private VirtualizationConnectorUtil virtualizationConnectorUtil;

    @InjectMocks
    private AddVirtualizationConnectorServiceRequestValidator validator;

    @Before
    public void testInitialize() throws EncryptionException {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(EncryptionUtil.class);
	    when(EncryptionUtil.encryptAESCTR(any(String.class))).thenReturn("Encrypted String");
    }

    @Test
    public void testValidate_WithValidVmwareRequest_ReturnsSuccess() throws Exception {
        // Arrange.
    	doNothing().when(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.VMWARE_REQUEST.getDto());
    	doNothing().when(virtualizationConnectorUtil).checkVmwareConnection( any(DryRunRequest.class), any(VirtualizationConnector.class));
    	
    	// Act.
        this.validator.validate(VirtualizationConnectorServiceData.VMWARE_REQUEST);
        
        // Assert.        
        verify(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.VMWARE_REQUEST.getDto());
        
    }
    
    @Test
    public void testValidate_WithValidOpenStackRequest_ReturnsSuccess() throws Exception {
        // Arrange.
    	doNothing().when(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST.getDto());
    	doNothing().when(virtualizationConnectorUtil).checkOpenstackConnection( any(DryRunRequest.class), any(VirtualizationConnector.class));        
        
        // Act.
        this.validator.validate(VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST);
        
        // Assert.        
        verify(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST.getDto());
    }
    
    @Test
    public void testValidate_WithNullRequest_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.validator.validate(null);
    }

    @Test
    public void testValidate_WithInvalidVmwareRequest_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        doThrow(VmidcBrokerValidationException.class).when(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.VMWARE_REQUEST.getDto());
        
        // Act.
        this.validator.validate(VirtualizationConnectorServiceData.VMWARE_REQUEST);
        
        // Assert.        
        verify(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.VMWARE_REQUEST.getDto());
    }
    
    @Test
    public void testValidate_WithInvalidOpenStackRequest_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        doThrow(VmidcBrokerValidationException.class).when(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST.getDto());
        
        // Act.
        this.validator.validate(VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST);
        
        // Assert.        
        verify(this.dtoValidator).validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_NSC_REQUEST.getDto());
    }
    
    @Test
    public void testValidate_WithValidateAndLoadRequest_ThrowsUnsupportedException() throws Exception {
        // Arrange.
        this.exception.expect(UnsupportedOperationException.class);

        // Act.
        this.validator.validateAndLoad(VirtualizationConnectorServiceData.VMWARE_REQUEST);
    }

}