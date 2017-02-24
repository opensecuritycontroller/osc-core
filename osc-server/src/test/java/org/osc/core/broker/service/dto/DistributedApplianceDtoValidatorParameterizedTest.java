package org.osc.core.broker.service.dto;

import static org.osc.core.broker.service.dto.DistributedApplianceDtoValidatorTestData.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ManagerApiFactory.class})
public class DistributedApplianceDtoValidatorParameterizedTest extends DistributedApplianceDtoValidatorBaseTest {
    DistributedApplianceDto dtoParam;
    Class<Throwable> exceptionTypeParam;
    String expectedErrorMessageParam;

    public DistributedApplianceDtoValidatorParameterizedTest(DistributedApplianceDto dto, Class<Throwable> exceptionType, String expectedErrorMessage) {
        this.dtoParam = dto;
        this.exceptionTypeParam = exceptionType;
        this.expectedErrorMessageParam = expectedErrorMessage;
    }

    @Override
    @Before
    public void testInitialize() throws Exception{
        super.testInitialize();

        VirtualizationConnector vmWareVc = new VirtualizationConnector();
        vmWareVc.setVirtualizationType(VirtualizationType.VMWARE);
        vmWareVc.setVirtualizationSoftwareVersion("softwareVersion");

        Domain invalidDomain = new Domain();
        invalidDomain.setName(StringUtils.rightPad("invalidName", 156, 'e'));

        ApplianceManagerConnector mcPolicyMappingNotSupported = new ApplianceManagerConnector();
        ManagerType mgrTypePolicyMappingNotSupported = ManagerType.SMC;
        mcPolicyMappingNotSupported.setManagerType(mgrTypePolicyMappingNotSupported);

        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, MC_ID_NOT_FOUND)).thenReturn(null);
        Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, MC_ID_POLICY_MAPPING_NOT_SUPPORTED_MC)).thenReturn(mcPolicyMappingNotSupported);
        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, VC_ID_NOT_FOUND)).thenReturn(null);
        Mockito.when(this.sessionMock.get(VirtualizationConnector.class, VC_ID_VMWARE)).thenReturn(vmWareVc);
        Mockito.when(this.sessionMock.get(Domain.class, DOMAIN_ID_INVALID_NAME)).thenReturn(invalidDomain);

        this.sessionStub.stubFindApplianceSoftwareVersion(applianceSwVersionNotFoundDto.getApplianceId(),
                SW_VERSION_NOT_FOUND,
                vmWareVc.getVirtualizationType(),
                vmWareVc.getVirtualizationSoftwareVersion(),
                null);

        this.sessionStub.stubFindVirtualSystem(DA_ID_EXISTING_VC, VC_ID_OPENSTACK, new VirtualSystem());

        ApplianceManagerApi applianceMgrPolicyMappingNotSupported = Mockito.mock(ApplianceManagerApi.class);
        Mockito.when(applianceMgrPolicyMappingNotSupported.isPolicyMappingSupported()).thenReturn(false);

        Mockito.when(ManagerApiFactory.createApplianceManagerApi(mgrTypePolicyMappingNotSupported)).thenReturn(applianceMgrPolicyMappingNotSupported);
    }

    @Test
    public void testValidate_UsingInvalidField_ThrowsExpectedException() throws Exception {
        // Arrange.
        this.exception.expect(this.exceptionTypeParam);
        this.exception.expectMessage(this.expectedErrorMessageParam);

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
