package org.osc.core.broker.service;

import java.util.Arrays;
import java.util.Set;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DistributedApplianceDtoValidator;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.util.SessionStub;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConformService.class, LockUtil.class, ManagerApiFactory.class})
public class UpdateDistributedApplianceServiceTest {
    private static long JOB_ID = 12345L;
    private static String NEW_APPLIANCE_SW_VERSION = "NEWVERSION";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Session sessionMock;

    @Mock(name = "daValidator")
    private DistributedApplianceDtoValidator validatorMock;

    @InjectMocks
    private UpdateDistributedApplianceService service;

    private BaseRequest<DistributedApplianceDto> request;
    private DistributedApplianceDto daDto;
    private DistributedAppliance da;
    private VirtualSystemDto vsDto;
    private VirtualSystemDto vsDtoToBeDeleted;
    private VirtualSystem vsToBeDeleted;
    private VirtualSystem vs;
    private DistributedApplianceInstance daInstance;
    private VirtualizationConnector vc;
    private Domain domain;
    private SessionStub sessionStub;
    private UnlockObjectMetaTask ult;

    private String invalidDaName = "invalidDaName";

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.vc = new VirtualizationConnector();
        this.vc.setId(1000L);
        this.vc.setVirtualizationType(VirtualizationType.VMWARE);
        this.vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");

        this.domain = new Domain();
        this.domain.setId(1001L);
        this.domain.setName("domainName");

        this.request = new BaseRequest<>();
        this.daDto = new DistributedApplianceDto();
        this.request.setDto(this.daDto);

        this.daDto.setName("daName");
        this.daDto.setId(1L);
        this.daDto.setApplianceId(2L);
        this.daDto.setApplianceSoftwareVersionName("softwareVersion");
        this.daDto.setMcId(3L);

        this.vsDto = new VirtualSystemDto();
        this.vsDto.setId(5L);
        this.vsDto.setVcId(this.vc.getId());
        this.vsDto.setDomainId(this.domain.getId());

        VirtualizationConnector vcToBeDeleted = new VirtualizationConnector();
        vcToBeDeleted.setId(2000L);
        vcToBeDeleted.setVirtualizationType(VirtualizationType.VMWARE);
        vcToBeDeleted.setVirtualizationSoftwareVersion("vcSoftwareVersion");

        this.vsDtoToBeDeleted = new VirtualSystemDto();
        this.vsDtoToBeDeleted.setId(7L);
        this.vsDtoToBeDeleted.setVcId(vcToBeDeleted.getId());
        this.vsDtoToBeDeleted.setDomainId(this.domain.getId());

        this.daDto.setVirtualizationSystems(Sets.newHashSet(this.vsDto, this.vsDtoToBeDeleted));

        this.da = new DistributedAppliance(new ApplianceManagerConnector());
        this.da.setId(this.daDto.getId());
        this.da.setName(this.daDto.getName());
        this.da.setApplianceVersion(this.daDto.getApplianceSoftwareVersionName());
        this.da.setAppliance(new Appliance());

        ApplianceSoftwareVersion softwareVersion = new ApplianceSoftwareVersion();
        softwareVersion.setApplianceSoftwareVersion(this.daDto.getApplianceSoftwareVersionName());

        this.vs = new VirtualSystem(this.da);
        this.da.addVirtualSystem(this.vs);

        this.vs.setId(this.vsDto.getId());
        this.vs.setApplianceSoftwareVersion(softwareVersion);
        this.vs.setDomain(this.domain);
        this.vs.setVirtualizationConnector(this.vc);
        this.vs.setMarkedForDeletion(false);

        this.daInstance = new DistributedApplianceInstance(new VirtualSystem(), AgentType.AGENT);
        this.daInstance.setApplianceConfig(new byte[3]);
        this.vs.addDistributedApplianceInstance(this.daInstance);

        this.vsToBeDeleted = new VirtualSystem(this.da);
        this.vsToBeDeleted.setId(this.vsDtoToBeDeleted.getId());
        this.vsToBeDeleted.setApplianceSoftwareVersion(softwareVersion);
        this.vsToBeDeleted.setDomain(this.domain);
        this.vsToBeDeleted.setVirtualizationConnector(this.vc);
        this.vsToBeDeleted.setMarkedForDeletion(false);
        this.da.addVirtualSystem(this.vsToBeDeleted);

        this.sessionStub = new SessionStub(this.sessionMock);
        this.ult = new UnlockObjectMetaTask(null);

        Mockito.when(this.sessionMock.get(Appliance.class, this.daDto.getApplianceId())).thenReturn(new Appliance());
        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, this.vsDto.getVcId())).thenReturn(this.vc);
        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, this.vsDtoToBeDeleted.getVcId())).thenReturn(vcToBeDeleted);
        Mockito.when(this.sessionMock.get(Domain.class, this.vsDto.getDomainId())).thenReturn(this.domain);
        Mockito.when(this.sessionMock.get(VirtualSystem.class, this.vsDto.getId())).thenReturn(this.vs);
        Mockito.when(this.sessionMock.get(VirtualSystem.class, this.vsDtoToBeDeleted.getId())).thenReturn(this.vsToBeDeleted);
        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, this.daDto.getMcId())).thenReturn(new ApplianceManagerConnector());

        Mockito.doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock)
        .validateForUpdate(Mockito.argThat(new DistributedApplianceDtoMatcher(this.invalidDaName)));
        Mockito.doReturn(this.da).when(this.validatorMock)
        .validateForUpdate(Mockito.argThat(new DistributedApplianceDtoMatcher(this.daDto.getName())));

        this.sessionStub.stubIsExistingEntity(DistributedAppliance.class, "name", this.daDto.getName(), false);

        this.sessionStub.stubFindApplianceSoftwareVersion(this.daDto.getApplianceId(),
                this.daDto.getApplianceSoftwareVersionName(),
                this.vc.getVirtualizationType(),
                this.vc.getVirtualizationSoftwareVersion(),
                softwareVersion);
        this.sessionStub.stubFindApplianceSoftwareVersion(this.daDto.getApplianceId(),
                NEW_APPLIANCE_SW_VERSION,
                this.vc.getVirtualizationType(),
                this.vc.getVirtualizationSoftwareVersion(),
                softwareVersion);
        this.sessionStub.stubListByDaId(this.daDto.getId(), Arrays.asList(this.daDto.getId()));

        PowerMockito.mockStatic(ConformService.class);
        Mockito.when(ConformService.startDAConformJob(Mockito.any(Session.class),
                (DistributedAppliance)Mockito.argThat(new DistributedApplianceMatcher(this.da)),
                Mockito.any(UnlockObjectMetaTask.class))).thenReturn(JOB_ID);

        PowerMockito.mockStatic(LockUtil.class);
        Mockito.when(LockUtil.tryLockDA(this.da, this.da.getApplianceManagerConnector())).thenReturn(this.ult);

        PowerMockito.mockStatic(ManagerApiFactory.class);
        Mockito.when(ManagerApiFactory.createManagerDeviceApi(this.vs)).thenReturn(Mockito.mock(ManagerDeviceApi.class));
        Mockito.when(ManagerApiFactory.createManagerDeviceApi(this.vsToBeDeleted)).thenReturn(Mockito.mock(ManagerDeviceApi.class));
    }

    @Test
    public void testDispatch_WithNullRequest_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.service.dispatch(null);
    }

    @Test
    public void testDispatch_WhenDistributedApplianceValidationFails_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        this.daDto.setName(this.invalidDaName);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);

        // Act.
        this.service.dispatch(this.request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForUpdate(this.request.getDto());
    }

    @Test
    public void testDispatch_UsingExistingAppliance_ApplianceIsUpdated() throws Exception {
        // Act.
        BaseJobResponse response = this.service.dispatch(this.request);

        // Assert.
        assertDaUpdated(response);
    }

    @Test
    public void testDispatch_RemovingVirtualSystem_VirtualSystemIsDeleted() throws Exception {
        // Arrange.
        // If the da virtual system is not present on the correspondent daDto then it needs to be deleted.
        Set<VirtualSystemDto> virtualSystems = this.daDto.getVirtualizationSystems();
        virtualSystems.remove(this.vsDtoToBeDeleted);
        this.daDto.setVirtualizationSystems(virtualSystems);

        // Act.
        BaseJobResponse response = this.service.dispatch(this.request);

        // Assert.
        assertDaUpdated(response);
        Mockito.verify(this.sessionMock).update(this.vsToBeDeleted);
        Assert.assertTrue(this.vsToBeDeleted.getMarkedForDeletion());
    }

    @Test
    public void testDispatch_ChangingTheSwVersion_DaInstanceIsUpdated() throws Exception {
        // Arrange.
        // If the da version is changing then the da instance must be updated.
        this.daDto.setApplianceSoftwareVersionName(NEW_APPLIANCE_SW_VERSION);

        // Act.
        BaseJobResponse response = this.service.dispatch(this.request);

        // Assert.
        assertDaUpdated(response);
        Mockito.verify(this.sessionMock).update(this.daInstance);
        Assert.assertNull("The da instance confi should be null.", this.daInstance.getApplianceConfig());
    }

    private void assertDaUpdated(BaseJobResponse response) throws Exception {
        Mockito.verify(this.validatorMock).validateForUpdate(this.request.getDto());
        Mockito.verify(this.sessionMock).update(this.da);

        PowerMockito.verifyStatic();
        LockUtil.tryLockDA(this.da, this.da.getApplianceManagerConnector());

        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertEquals("The job id was different than expected.", JOB_ID, response.getJobId().longValue());
        Assert.assertNull("The response id should be null.", response.getId());
    }

    private class DistributedApplianceDtoMatcher extends ArgumentMatcher<DistributedApplianceDto> {
        private String daDtoName;

        public DistributedApplianceDtoMatcher(String daDtoName) {
            this.daDtoName = daDtoName;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceDto)) {
                return false;
            }
            return this.daDtoName.equals(UpdateDistributedApplianceServiceTest.this.daDto.getName());
        }
    }

    private class DistributedApplianceMatcher extends ArgumentMatcher<Object> {
        private DistributedAppliance da;

        public DistributedApplianceMatcher(DistributedAppliance da) {
            this.da = da;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedAppliance)) {
                return false;
            }
            DistributedAppliance da = (DistributedAppliance)object;
            return this.da.getId() == da.getId() || this.da.getName().equals(da.getName());
        }
    }
}
