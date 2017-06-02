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
package org.osc.core.broker.service.validator;
//TODO Hailee: Commented code
import static org.osc.core.broker.service.validator.DistributedApplianceDtoValidatorTestData.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class DistributedApplianceDtoValidatorParameterizedTest extends DistributedApplianceDtoValidatorBaseTest {
    DistributedApplianceDto dtoParam;
    Class<Throwable> exceptionTypeParam;
    String expectedErrorMessageParam;
    private VirtualizationConnector vmWareVc;
    private ApplianceManagerConnector mcPolicyMappingNotSupported;
    private Domain invalidDomain;

    public DistributedApplianceDtoValidatorParameterizedTest(DistributedApplianceDto dto, Class<Throwable> exceptionType, String expectedErrorMessage) {
        this.dtoParam = dto;
        this.exceptionTypeParam = exceptionType;
        this.expectedErrorMessageParam = expectedErrorMessage;
    }

    @Override
    @Before
    public void testInitialize() throws Exception{
        super.testInitialize();

        populateDatabase();

        ManagerType mgrTypePolicyMappingNotSupported = ManagerType.SMC;
        ManagerType.addType(ManagerType.SMC.getValue());

//        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, MC_ID_NOT_FOUND)).thenReturn(null);
//        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, MC_ID_POLICY_MAPPING_NOT_SUPPORTED_MC)).thenReturn(mcPolicyMappingNotSupported);
//        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, VC_ID_NOT_FOUND)).thenReturn(null);
//        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, VC_ID_VMWARE)).thenReturn(vmWareVc);
//        Mockito.when(this.sessionMock.get(Domain.class, DOMAIN_ID_INVALID_NAME)).thenReturn(invalidDomain);
//
//        this.sessionStub.stubFindApplianceSoftwareVersion(applianceSwVersionNotFoundDto.getApplianceId(),
//                SW_VERSION_NOT_FOUND,
//                vmWareVc.getVirtualizationType(),
//                vmWareVc.getVirtualizationSoftwareVersion(),
//                null);
//
//        this.sessionStub.stubFindVirtualSystem(DA_ID_EXISTING_VC, VC_ID_OPENSTACK, new VirtualSystem());

        ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);

        Mockito.when(apiFactoryService.syncsPolicyMapping(mgrTypePolicyMappingNotSupported)).thenReturn(false);
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        this.vmWareVc = new VirtualizationConnector();
        //this.vmWareVc.setVirtualizationType(VirtualizationType.VMWARE);
        this.vmWareVc.setVirtualizationSoftwareVersion("softwareVersion");

        this.vmWareVc.setName(VC_NAME_VMWARE);
        this.vmWareVc.setProviderIpAddress("127.0.0.2");
        this.vmWareVc.setProviderUsername("Natasha");
        this.vmWareVc.setProviderPassword("********");

        this.em.persist(this.vmWareVc);

        this.invalidDomain = new Domain(this.amc);
        this.invalidDomain.setName(StringUtils.rightPad("invalidName", 156, 'e'));

        this.em.persist(this.invalidDomain);

        this.mcPolicyMappingNotSupported = new ApplianceManagerConnector();
        this.mcPolicyMappingNotSupported.setManagerType(ManagerType.SMC.getValue());
        this.mcPolicyMappingNotSupported.setIpAddress("127.0.0.4");
        this.mcPolicyMappingNotSupported.setName("mappingNotSupported");
        this.mcPolicyMappingNotSupported.setServiceType("foobar");

        this.em.persist(this.mcPolicyMappingNotSupported);

        VirtualSystem vs = new VirtualSystem(this.da);
        vs.setApplianceSoftwareVersion(this.asv);
        vs.setDomain(this.domain);
        vs.setVirtualizationConnector(this.vc);
        vs.setMarkedForDeletion(false);
        vs.setName("vsName");

        this.em.persist(vs);

        this.em.getTransaction().commit();
    }

    @Test
    public void testValidate_UsingInvalidField_ThrowsExpectedException() throws Exception {
        // Arrange.
        this.exception.expect(this.exceptionTypeParam);
        this.exception.expectMessage(this.expectedErrorMessageParam);
        if(this.dtoParam.getApplianceId() == null) {
            this.dtoParam.setApplianceId(this.app.getId());
        }

        if(this.dtoParam.getMcId() == null) {
            this.dtoParam.setMcId(this.amc.getId());
        }

        Set<VirtualSystemDto> virtualizationSystems = this.dtoParam.getVirtualizationSystems();

        if(virtualizationSystems != null) {
            for(VirtualSystemDto vsDto : virtualizationSystems){
                if(vsDto.getVcId() == null) {
                    vsDto.setVcId(this.vc.getId());
                } else if (REPLACE_WITH_NULL.equals(vsDto.getVcId())) {
                    vsDto.setVcId(null);
                } else if (VC_ID_VMWARE.equals(vsDto.getVcId())) {
                    vsDto.setVcId(this.vmWareVc.getId());
                } else if (VC_ID_OPENSTACK.equals(vsDto.getVcId())) {
                    vsDto.setVcId(this.vc.getId());
                }

                if(REPLACE_WITH_DOMAIN_ID.equals(vsDto.getDomainId())) {
                    vsDto.setDomainId(this.domain.getId());
                } else if (DOMAIN_ID_INVALID_NAME.equals(vsDto.getDomainId())) {
                    vsDto.setDomainId(this.invalidDomain.getId());
                }
            }
        }

        if(MC_ID_POLICY_MAPPING_NOT_SUPPORTED_MC.equals(this.dtoParam.getMcId())) {
            this.dtoParam.setMcId(this.mcPolicyMappingNotSupported.getId());
        }

        if(DA_ID_EXISTING_VC.equals(this.dtoParam.getId())) {
            this.dtoParam.setId(this.da.getId());
        }

        // Act.
        this.validator.validate(this.dtoParam);
    }

    @Parameters()
    public static Collection<Object[]> getInvalidFieldsTestData() {
        List<Object[]> result = new ArrayList<Object[]>();
        result.addAll(getInvalidSwVersionTestData());
        result.addAll(getInvalidSecretKeyTestData());
        result.addAll(getInvalidNameTestData());
        result.addAll(getInvalidVirtualizationSystemsCollectionData());
        result.add(getInvalidApplianceIdTestData());
        result.add(getInvalidMngrConnectorIdTestData());
        result.addAll(getInvalidVcIdTestData());
        result.addAll(getInvalidEncapsulationTypeTestData());
        result.add(getInvalidApplianceSoftwareVersionTestData());
        result.add(getDaVcAlreadyExistsTestData());
        result.addAll(getInvalidDomainTestData());
        return result;
    }
}
