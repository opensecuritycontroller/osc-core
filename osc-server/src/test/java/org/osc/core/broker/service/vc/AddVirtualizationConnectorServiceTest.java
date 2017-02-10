package org.osc.core.broker.service.vc;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.AddVirtualizationConnectorServiceRequestValidator;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.util.SessionStub;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

public class AddVirtualizationConnectorServiceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Session sessionMock;

    @Mock
    private AddVirtualizationConnectorServiceRequestValidator validatorMock;

    @InjectMocks
    private AddVirtualizationConnectorService service;

    private SessionStub sessionStub;

    private static final String NAME_ALREADY_EXISTS = "Name already exists in the System";

    private final Long id_100 = Long.valueOf(100l);
    private final Long id_200 = Long.valueOf(200l);
    private final Long id_300 = Long.valueOf(300l);

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.sessionStub = new SessionStub(this.sessionMock);

    }

    @Test
    public void testDispatch_WhenVmWareRequest_ReturnsResponse() throws Exception {

        // Arrange.
        doNothing().when(this.validatorMock).validate(VirtualizationConnectorServiceData.VMWARE_REQUEST);
        this.sessionStub.stubSaveEntity(new VirtualizationConnectorMatcher(this.id_100), this.id_100);

        // Act.
        BaseResponse response = this.service.dispatch(VirtualizationConnectorServiceData.VMWARE_REQUEST);

        // Assert.
        validateResponse(response, this.id_100);
        verify(this.validatorMock).validate(VirtualizationConnectorServiceData.VMWARE_REQUEST);
        verify(this.sessionMock, times(1)).save(any());
        verify(this.sessionMock, times(1)).update(any());
        verify(this.sessionMock, times(0)).delete(any());

    }
    
    @Test
    public void testDispatch_WhenVcNameAlreadyExists_ThrowsValidationException() throws Exception {

        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        doThrow(new VmidcBrokerValidationException(NAME_ALREADY_EXISTS)).when(this.validatorMock)
                .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Act.
        this.service.dispatch(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Assert.
        verify(this.validatorMock)
                .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);
    }

    @Test
    public void testDispatch_WhenOpenStackNameAlreadyExists_ThrowsSslCertificateException() throws Exception {

        // Arrange.
        this.exception.expect(SslCertificatesExtendedException.class);
        ErrorTypeException exception = new ErrorTypeException("Error Thrown", ErrorType.CONTROLLER_EXCEPTION);
        doThrow(new SslCertificatesExtendedException( exception, new ArrayList<CertificateResolverModel>())).when(this.validatorMock)
                .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);
        service.setForceAddSSLCertificates(true);
        
        // Act.
        this.service.dispatch(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Assert.
        verify(this.validatorMock)
                .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);
        
        // clean up
        service.setForceAddSSLCertificates(false);
    }

    private void validateResponse(BaseResponse response, Long id) {

        Assert.assertNotNull("Response shouldn't be null", response);
        Assert.assertEquals("Both VC id's should be equal", id, response.getId());
    }

    private class VirtualizationConnectorMatcher extends ArgumentMatcher<Object> {

        private Long id;

        public VirtualizationConnectorMatcher(Long id) {
            this.id = id;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof VirtualizationConnector)) {
                return false;
            }

            VirtualizationConnector vc = (VirtualizationConnector) object;
            return this.id != vc.getId();
        }
    }

}