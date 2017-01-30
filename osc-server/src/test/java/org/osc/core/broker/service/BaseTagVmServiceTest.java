package org.osc.core.broker.service;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.server.model.TagVmRequest;
import org.osc.core.broker.rest.server.model.TagVmRequestValidator;
import org.osc.core.broker.util.VimUtils;
import org.osc.sdk.sdn.api.SecurityTagApi;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.VirtualMachine;


public abstract class BaseTagVmServiceTest {
    static final String TAG = "tag";
    static final String INVALID_IP_ADDRESS = "500.200.1.0";
    private static final String VALID_IP_ADDRESS = "127.0.0.1";
    static final String INVALID_VM_UUID = "666";
    private static final String VALID_VM_UUID = "1";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String APPLIANCE_NAME = "appliance_name";

    static TagVmRequest REQUEST_WITH_TAG = createRequest(TAG, null, null);
    static TagVmRequest REQUEST_WITH_TAG_INVALID_VM_UUID = createRequest(TAG, null, INVALID_VM_UUID);
    static TagVmRequest REQUEST_WITH_TAG_AND_INVALID_IP_ADDRESS = createRequest(TAG, INVALID_IP_ADDRESS, null);
    static TagVmRequest REQUEST_WITH_IP_ADDRESS = createRequest(null, VALID_IP_ADDRESS, null);
    static TagVmRequest REQUEST_WITH_TAG_AND_IP_ADDRESS = createRequest(TAG, VALID_IP_ADDRESS, null);
    static TagVmRequest REQUEST_WITH_VM_UUID = createRequest(null, null, VALID_VM_UUID);
    static TagVmRequest REQUEST_WITH_TAG_AND_VM_UUID = createRequest(TAG, null, VALID_VM_UUID);
    static TagVmRequest REQUEST_WITH_TAG_AND_IP_ADDRESS_AND_VM_UUID = createRequest(TAG, VALID_IP_ADDRESS, VALID_VM_UUID);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    Session session;

    @Mock
    protected TagVmRequestValidator validatorMock;

    @Mock
    private VimUtils vimUtils;

    @Mock
    SecurityTagApi securityTagApi;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setProviderIpAddress(VALID_IP_ADDRESS);
        vc.setProviderUsername(USERNAME);
        vc.setProviderPassword(PASSWORD);

        VirtualSystem vs = new VirtualSystem();
        vs.setVirtualizationConnector(vc);

        DistributedApplianceInstance distributedApplianceInstance = new DistributedApplianceInstance(vs, AgentType.AGENT);

        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_TAG);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_TAG_INVALID_VM_UUID);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_TAG_AND_INVALID_IP_ADDRESS);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_IP_ADDRESS);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_TAG_AND_IP_ADDRESS);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_VM_UUID);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_TAG_AND_VM_UUID);
        Mockito.doReturn(distributedApplianceInstance).when(this.validatorMock).validateAndLoad(REQUEST_WITH_TAG_AND_IP_ADDRESS_AND_VM_UUID);

        ManagedObjectReference mor = new ManagedObjectReference();
        mor.setVal(VALID_VM_UUID);
        VirtualMachine vm = new VirtualMachine(null, mor);

        Mockito.when(this.vimUtils.findVmByInstanceUuid(VALID_VM_UUID)).thenReturn(vm);
        Mockito.when(this.vimUtils.findVmByIp(VALID_IP_ADDRESS)).thenReturn(vm);

        Mockito.doNothing().when(this.securityTagApi).addSecurityTagToVM(mor.getVal(), BaseTagVmService.DEFAULT_OSC_SECURITY_TAG);
    }

    private static TagVmRequest createRequest(String tag, String ipAddress, String vmUuid) {
        TagVmRequest request = new TagVmRequest();
        request.setApplianceInstanceName(APPLIANCE_NAME);
        request.setTag(tag);
        request.setIpAddress(ipAddress);
        request.setVmUuid(vmUuid);
        return request;
    }
}