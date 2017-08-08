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

import static org.osc.core.broker.service.validator.DistributedApplianceDtoValidatorTestData.*;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DistributedApplianceDtoValidatorTest extends DistributedApplianceDtoValidatorBaseTest{
//    private DistributedAppliance existingDa;

    @Override
    @Before
    public void testInitialize() throws Exception{
        super.testInitialize();

        ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);
        Mockito.when(apiFactoryService.syncsPolicyMapping("NSM")).thenReturn(true);
    }

    @Test
    public void testValidateForCreate_WhenDistributedApplianceExists_ThrowsValidationException() throws Exception {
        // Arrange.
        DistributedApplianceDto existingDaDto= createDistributedApplianceDto();
        existingDaDto.setName(DA_NAME_EXISTING_DA);
        existingDaDto.setApplianceId(this.app.getId());
        existingDaDto.setMcId(this.amc.getId());

        for (VirtualSystemDto vsDto : existingDaDto.getVirtualizationSystems()) {
            vsDto.setVcId(this.vc.getId());
            vsDto.setDomainId(this.domain.getId());
        }

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
        newDaDto.setApplianceId(this.app.getId());
        newDaDto.setMcId(this.amc.getId());

        for (VirtualSystemDto vsDto : newDaDto.getVirtualizationSystems()) {
            vsDto.setVcId(this.vc.getId());
            vsDto.setDomainId(this.domain.getId());
        }

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
        daDto.setId(this.da.getId());
        daDto.setName(DA_NAME_EXISTING_DA);
        daDto.setApplianceId(this.app.getId());
        daDto.setMcId(this.amc.getId() + 1);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Appliance Manager Connector change is not allowed.");

        // Act.
        this.validator.validateForUpdate(daDto);
    }

    /**
     * This test is currently ignored because it does not make sense when using
     * a real database. The returned value will always have the right id because it
     * is loaded by id (its primary key).
     * @throws Exception
     */
    @Test
    @Ignore
    public void testValidateForUpdate_WhenDistributedApplianceExists_ExpectsCorrespondentDa() throws Exception {
        // Arrange.
        DistributedApplianceDto daDto = createDistributedApplianceDto();
        daDto.setId(DA_ID_EXISTING_DA);

        // Act.
        DistributedAppliance da = this.validator.validateForUpdate(daDto);

        // Assert.
        Assert.assertNotNull("The returned da should not be null.",  da);
        Assert.assertEquals("The id of the returned da was different than expected.", this.da.getId(), da.getId());
    }
}
