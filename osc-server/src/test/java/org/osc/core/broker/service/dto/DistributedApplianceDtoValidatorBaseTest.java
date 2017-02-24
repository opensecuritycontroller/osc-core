/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service.dto;

import static org.osc.core.broker.service.dto.DistributedApplianceDtoValidatorTestData.*;

import org.hibernate.Session;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.DistributedApplianceDtoValidator;
import org.osc.core.broker.util.SessionStub;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import junitparams.JUnitParamsRunner;

/**
 * The base class for the {@link DistributedApplianceDtoValidator} unit tests.
 * The unit tests for {@link DistributedApplianceDtoValidator} have been split in two test classes.
 * The reason is because the runner {@link Parameterized} only supports data driven tests to be within the test class,
 * other non data driven tests need to go on a different test class.
 * We could optionally use the {@link JUnitParamsRunner}, which supports mixing data driven and non data driven
 * tests on the same class (as it was before) but this runner is not compatible with {@link PowerMockRunner} now needed for these tests.
 */
public class DistributedApplianceDtoValidatorBaseTest {
    @Mock
    protected Session sessionMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected DistributedAppliance mismatchingMcDa;

    protected SessionStub sessionStub;

    protected DistributedApplianceDtoValidator validator;

    protected void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        this.sessionStub = new SessionStub(this.sessionMock);

        this.validator = new DistributedApplianceDtoValidator(this.sessionMock);

        VirtualizationConnector openStackVc = new VirtualizationConnector();
        openStackVc.setVirtualizationType(VirtualizationType.OPENSTACK);
        openStackVc.setName(VC_NAME_OPENSTACK);

        Domain validDomain = new Domain();
        validDomain.setName("domainName");

        ApplianceManagerConnector mcPolicyMappingSupported = new ApplianceManagerConnector();
        ManagerType mgrTypePolicyMappingSupported = ManagerType.NSM;
        mcPolicyMappingSupported.setManagerType(mgrTypePolicyMappingSupported);

        Mockito.when(this.sessionMock.get(Appliance.class, APPLIANCE_ID_EXISTING)).thenReturn(new Appliance());
        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, MC_ID_VALID_MC)).thenReturn(mcPolicyMappingSupported);
        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, VC_ID_OPENSTACK)).thenReturn(openStackVc);
        Mockito.when(this.sessionMock.get(Domain.class, DOMAIN_ID_VALID_NAME)).thenReturn(validDomain);

        this.sessionStub.stubFindApplianceSoftwareVersion(daVcAlreadyExistsDto.getApplianceId(),
                SW_VERSION_EXISTING_VC,
                openStackVc.getVirtualizationType(),
                openStackVc.getVirtualizationSoftwareVersion(),
                new ApplianceSoftwareVersion());

        this.sessionStub.stubFindVirtualSystem(DA_ID_EXISTING_VC, VC_ID_OPENSTACK, new VirtualSystem());

        ApplianceManagerApi applianceMgrPolicyMappingSupported = Mockito.mock(ApplianceManagerApi.class);
        Mockito.when(applianceMgrPolicyMappingSupported.isPolicyMappingSupported()).thenReturn(true);

        PowerMockito.mockStatic(ManagerApiFactory.class);
        Mockito.when(ManagerApiFactory.createApplianceManagerApi(mgrTypePolicyMappingSupported)).thenReturn(applianceMgrPolicyMappingSupported);
    }
}
