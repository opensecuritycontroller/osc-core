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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class VirtualizationConnectorDtoValidatorTest extends VirtualizationConnectorDtoValidatorBaseTest{
    @Test
    public void testValidateForCreate_WhenVcRequest_ReturnsSuccessful() throws Exception {
        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorDtoValidatorTestData.OPENSTACK_NOCONTROLLER_VC);

        //Assert
        Assert.assertTrue(true);
    }

    @Test
    public void testValidateForCreate_WhenVcNameExistsRequest_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        VirtualizationConnectorDto dto = VirtualizationConnectorDtoValidatorTestData.OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_VC;
        this.exception.expectMessage("Virtualization Connector Name: " + dto.getName() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(dto);
    }

    @Test
    public void testValidateForCreate_WhenControllerIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        VirtualizationConnectorDto dto = VirtualizationConnectorDtoValidatorTestData.OPENSTACK_CONTROLLER_IP_ALREADY_EXISTS_VC;
        this.exception.expectMessage("Controller IP Address: " + dto.getControllerIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(dto);
    }

    @Test
    public void testValidateForCreate_WhenProviderIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        VirtualizationConnectorDto dto = VirtualizationConnectorDtoValidatorTestData.PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_VC;
        this.exception.expectMessage("Provider IP Address: " + dto.getProviderIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(dto);
    }
}