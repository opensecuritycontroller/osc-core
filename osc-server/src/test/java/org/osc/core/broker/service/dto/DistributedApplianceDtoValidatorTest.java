package org.osc.core.broker.service.dto;

import static org.osc.core.broker.service.dto.DistributedApplianceDtoValidatorTestData.*;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ManagerApiFactory.class})
public class DistributedApplianceDtoValidatorTest extends DistributedApplianceDtoValidatorBaseTest{
    private DistributedAppliance existingDa;
    private DistributedAppliance mismatchingMcDa;

    @Override
    @Before
    public void testInitialize() throws Exception{
        super.testInitialize();

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setId(MC_ID_VALID_MC);
        this.existingDa = new DistributedAppliance(mc);
        this.existingDa.setId(DA_ID_EXISTING_DA);

        ApplianceManagerConnector mistmatchingMc = new ApplianceManagerConnector();
        mistmatchingMc.setId(450L);
        this.mismatchingMcDa = new DistributedAppliance(mistmatchingMc);

        Mockito.when(this.sessionMock.get(Appliance.class, APPLIANCE_ID_EXISTING)).thenReturn(new Appliance());
        Mockito.when(this.sessionMock.get(DistributedAppliance.class, DA_ID_EXISTING_DA)).thenReturn(this.existingDa);
        Mockito.when(this.sessionMock.get(DistributedAppliance.class, DA_ID_MISMATCHING_MC)).thenReturn(this.mismatchingMcDa);

        this.sessionStub.stubIsExistingEntity(DistributedAppliance.class, "name", DA_NAME_EXISTING_DA, true);
        this.sessionStub.stubIsExistingEntity(DistributedAppliance.class, "name", DA_NAME_NEW_DA, false);

        this.sessionStub.stubFindVirtualSystem(DA_ID_EXISTING_VC, VC_ID_OPENSTACK, new VirtualSystem());
    }

    @Test
    public void testValidateForCreate_WhenDistributedApplianceExists_ThrowsValidationException() throws Exception {
        // Arrange.
        DistributedApplianceDto existingDaDto= createDistributedApplianceDto();
        existingDaDto.setName(DA_NAME_EXISTING_DA);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(MessageFormat.format("Distributed Appliance Name: {0} already exists.", DA_NAME_EXISTING_DA));

        // Act.
        this.validator.validateForCreate(existingDaDto);
    }

    @Test
    public void testValidateForCreate_WhenDistributedApplianceIsValid_ValidationSucceeds() throws Exception {
        // Arrange.
        DistributedApplianceDto newDaDto= createDistributedApplianceDto();
        newDaDto.setName(DA_NAME_NEW_DA);

        // Act.
        this.validator.validateForCreate(newDaDto);
    }

    @Test
    public void testValidateForUpdate_WithNullDaId_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        DistributedApplianceDto daDto = createDistributedApplianceDto();
        daDto.setId(null);

        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage("Id " + EMPTY_VALUE_ERROR_MESSAGE);

        // Act.
        this.validator.validateForUpdate(daDto);
    }

    @Test
    public void testValidateForUpdate_WhenDistributedApplianceNotFound_ThrowsValidationException() throws Exception {
        // Arrange.
        Long notFoundDaId = 2000L;
        DistributedApplianceDto daDto = createDistributedApplianceDto();
        daDto.setId(notFoundDaId);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(MessageFormat.format("Distributed Appliance entry with name: {0}) is not found.", daDto.getName()));

        // Act.
        this.validator.validateForUpdate(daDto);
    }

    @Test
    public void testValidateForUpdate_WhenManagerConnectorIdMismatches_ThrowsValidationException() throws Exception {
        // Arrange.
        DistributedApplianceDto daDto = createDistributedApplianceDto();
        daDto.setId(DA_ID_MISMATCHING_MC);
        daDto.setMcId(this.mismatchingMcDa.getApplianceManagerConnector().getId() + 1);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Appliance Manager Connector change is not allowed.");

        // Act.
        this.validator.validateForUpdate(daDto);
    }

    @Test
    public void testValidateForUpdate_WhenDistributedApplianceExists_ExpectsCorrespondentDa() throws Exception {
        // Arrange.
        DistributedApplianceDto daDto = createDistributedApplianceDto();
        daDto.setId(DA_ID_EXISTING_DA);

        // Act.
        DistributedAppliance da = this.validator.validateForUpdate(daDto);

        // Assert.
        Assert.assertNotNull("The returned da should not be null.",  da);
        Assert.assertEquals("The id of the returned da was different than expected.", this.existingDa.getId(), da.getId());
    }
}
