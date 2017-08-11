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
//TODO: Hailee
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class VirtualizationConnectorDtoValidatorTest extends VirtualizationConnectorDtoValidatorBaseTest{

    @Test
    public void testValidate_WhenVcRequest_ReturnsSuccessful() throws Exception {
        // Arrange.
        VirtualizationConnectorRequest dto = VirtualizationConnectorServiceData.OPENSTACK_NOCONTROLLER_REQUEST.getDto();

        // Act.
        this.dtoValidator.validateForCreate(dto);

        //Assert
        Assert.assertTrue(true);
    }

    @Test
    public void testValidate_WhenVcNameExistsRequest_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtualization Connector Name: " + VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_REQUEST.getDto().getName() + " already exists.");

        VirtualizationConnectorRequest dto = VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_REQUEST.getDto();

        // Act.
        this.dtoValidator.validateForCreate(dto);
    }

    @Test
    public void testValidate_WhenControllerIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        VirtualizationConnectorRequest dto = VirtualizationConnectorServiceData.OPENSTACK_CONTROLLER_IP_ALREADY_EXISTS_REQUEST.getDto();
        this.exception.expectMessage("Controller IP Address: " + dto.getControllerIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(dto);
    }

    @Test
    public void testValidate_WhenProviderIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        VirtualizationConnectorRequest dto = VirtualizationConnectorServiceData.PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_REQUEST.getDto();
        this.exception.expectMessage("Provider IP Address: " + dto.getProviderIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(dto);
    }

}