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
package org.osc.core.broker.service.dto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.vc.VirtualizationConnectorServiceData;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SdnControllerApiFactory.class })
public class VirtualizationConnectorDtoValidatorTest extends VirtualizationConnectorDtoValidatorBaseTest{

    final String controllerTypeNSC = "NSC";

    @Override
    @Before
    public void testInitialize() throws Exception {
        super.testInitialize();

        ControllerType.addType(this.controllerTypeNSC);

        PowerMockito.spy(SdnControllerApiFactory.class);
        PowerMockito.doReturn(false).when(SdnControllerApiFactory.class, "usesProviderCreds",
                ControllerType.fromText(this.controllerTypeNSC));
    }

    @Test
    public void testValidate_WhenVcRequest_ReturnsSuccessful() throws Exception {

        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_NOCONTROLLER_REQUEST.getDto());

        //Assert
        Assert.assertTrue(true);
    }

    @Test
    public void testValidate_WhenVcNameExistsRequest_ThrowsValidationException() throws Exception {
    	// Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtualization Connector Name: " + VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_REQUEST.getDto().getName() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_REQUEST.getDto());
    }

    @Test
    public void testValidate_WhenControllerIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Controller IP Address: " + VirtualizationConnectorServiceData.OPENSTACK_CONTROLLER_IP_ALREADY_EXISTS_REQUEST.getDto().getControllerIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.OPENSTACK_CONTROLLER_IP_ALREADY_EXISTS_REQUEST.getDto());
    }

    @Test
    public void testValidate_WhenProviderIpExists_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Provider IP Address: " + VirtualizationConnectorServiceData.PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_REQUEST.getDto().getProviderIP() + " already exists.");

        // Act.
        this.dtoValidator.validateForCreate(VirtualizationConnectorServiceData.PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_REQUEST.getDto());
    }

}