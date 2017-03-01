/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service;

import java.text.MessageFormat;
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
import org.mockito.internal.util.collections.Sets;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DistributedApplianceDtoValidator;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;
import org.osc.core.broker.util.SessionStub;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConformService.class)
public class AddDistributedApplianceServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Session sessionMock;

    @Mock
    private DistributedApplianceDtoValidator validatorMock;

    @InjectMocks
    private AddDistributedApplianceService service;

    private BaseRequest<DistributedApplianceDto> request;
    private DistributedApplianceDto daDto;
    private DistributedAppliance da;
    private VirtualSystemDto vsDto;
    private VirtualSystem vs;
    private VirtualizationConnector vc;
    private Domain domain;
    private SessionStub sessionStub;

    private String invalidDaName = "invalidDaName";

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.request = new BaseRequest<DistributedApplianceDto>();
        this.daDto = new DistributedApplianceDto();
        this.daDto.setName("daName");
        this.request.setDto(this.daDto);

        this.daDto.setApplianceId(1L);
        this.daDto.setApplianceSoftwareVersionName("softwareVersion");
        this.daDto.setMcId(2L);

        this.domain = new Domain();
        this.domain.setId(3L);
        this.domain.setName("domainName");

        this.vsDto = new VirtualSystemDto();
        this.vsDto.setId(4L);
        this.vsDto.setVcId(5L);
        this.vsDto.setDomainId(this.domain.getId());
        this.daDto.setVirtualizationSystems(Sets.newSet(this.vsDto));

        this.da = new DistributedAppliance();
        this.da.setId(this.daDto.getId());
        this.da.setName(this.daDto.getName());

        this.vs = new VirtualSystem(this.da);
        ApplianceSoftwareVersion softwareVersion = new ApplianceSoftwareVersion();
        softwareVersion.setApplianceSoftwareVersion(this.daDto.getApplianceSoftwareVersionName());
        this.vs.setId(6L);

        this.vc = new VirtualizationConnector();
        this.vc.setId(7L);
        this.vc.setVirtualizationType(VirtualizationType.VMWARE);
        this.vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");

        this.vs.setApplianceSoftwareVersion(softwareVersion);
        this.vs.setDomain(this.domain);
        this.vs.setVirtualizationConnector(this.vc);

        this.sessionStub = new SessionStub(this.sessionMock);

        Mockito.when(this.sessionMock.get(Appliance.class, this.daDto.getApplianceId())).thenReturn(new Appliance());
        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, this.vsDto.getVcId())).thenReturn(this.vc);
        Mockito.when(this.sessionMock.get(Domain.class, this.vsDto.getDomainId())).thenReturn(this.domain);

        Mockito.doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock)
        .validateForCreate(Mockito.argThat(new DistributedApplianceDtoMatcher(this.invalidDaName)));

        this.sessionStub.stubIsExistingEntity(DistributedAppliance.class, "name", this.daDto.getName(), false);

        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, this.daDto.getMcId())).thenReturn(new ApplianceManagerConnector());

        this.sessionStub.stubFindApplianceSoftwareVersion(this.daDto.getApplianceId(),
                this.daDto.getApplianceSoftwareVersionName(),
                this.vc.getVirtualizationType(),
                this.vc.getVirtualizationSoftwareVersion(),
                softwareVersion);
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
        Mockito.verify(this.validatorMock).validateForCreate(this.request.getDto());
    }

    @Test
    public void testDispatch_AddingNewAppliance_ExpectsValidResponse() throws Exception {
        // Arrange.
        Long jobId = new Long(1234L);
        Long daId = new Long(145L);

        this.sessionStub.stubSaveEntity(new VirtualSystemMatcher(this.vs), 123L);
        this.sessionStub.stubSaveEntity(new DistributedApplianceMatcher(this.da), daId);
        PowerMockito.mockStatic(ConformService.class);
        Mockito.when(ConformService.startDAConformJob(Mockito.any(Session.class), (DistributedAppliance)Mockito.argThat(new DistributedApplianceMatcher(this.da)))).thenReturn(jobId);

        // Act.
        AddDistributedApplianceResponse response = this.service.dispatch(this.request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(this.request.getDto());
        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertNotNull("The secret key should not be null when the request is not "
                + "orginated from the REST API.", response.getSecretKey());
        Assert.assertEquals("The job id was different than expected.", jobId, response.getJobId());
        Assert.assertEquals("The response id was different than expected.", daId, response.getId());
        Assert.assertEquals("The response name was different than expected.", this.daDto.getName(), response.getName());
        Assert.assertEquals("The count of virtualization systems was different than expected.", this.daDto.getVirtualizationSystems().size(), response.getVirtualizationSystems().size());
        for (VirtualSystemDto vs: this.daDto.getVirtualizationSystems()) {
            vs.setId(123L);
            Assert.assertTrue(MessageFormat.format("The expected vs with id {0} was not found.", vs.getId()),
                    Contains(response.getVirtualizationSystems(), vs));
        }
    }

    private boolean Contains(Set<VirtualSystemDto> vsDtos, VirtualSystemDto expectedVsDto) {
        if (vsDtos == null) {
            return false;
        }

        for (VirtualSystemDto vsDto : vsDtos) {
            if (vsDto.getId() == expectedVsDto.getId() &&
                    vsDto.getDomainId() == this.vs.getDomain().getId() &&
                    vsDto.getVcId() == this.vs.getVirtualizationConnector().getId()) {
                return true;
            }
        }

        return false;
    }

    private class VirtualSystemMatcher extends ArgumentMatcher<Object> {
        private VirtualSystem vs;

        VirtualSystemMatcher(VirtualSystem vs) {
            this.vs = vs;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof VirtualSystem)) {
                return false;
            }

            VirtualSystem providedVs = (VirtualSystem) object;

            // Matching by the virtual system id or the appliance id since for mocked session.save calls
            // the vs id is not provided as part of the input.
            return this.vs.getId() == providedVs.getId() ||
                    this.vs.getDistributedAppliance().getId() == this.vs.getDistributedAppliance().getId();
        }
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
            return this.daDtoName.equals(AddDistributedApplianceServiceTest.this.daDto.getName());
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
