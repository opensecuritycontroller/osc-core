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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.response.TagVmResponse;

public class TagVmServiceTest extends BaseTagVmServiceTest {
    @InjectMocks
    private TagVmService tagVmService;

    @Test
    public void testExec_WithRequestWithoutVmUuidAndIpAddress_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Missing IP Address or VM Uuid input.");

        // Act.
        this.tagVmService.exec(REQUEST_WITH_TAG, this.session);
    }

    @Test
    public void testExec_WithRequestWithVmUuidAndIpAddress_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Input must be either IP Address or VM Uuid but not both.");

        // Act.
        this.tagVmService.exec(REQUEST_WITH_TAG_AND_IP_ADDRESS_AND_VM_UUID, this.session);
    }

    @Test
    public void testExec_WithRequestWithVmUuidWithoutVm_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("VM with Uuid '" + INVALID_VM_UUID + "' not found.");

        // Act.
        this.tagVmService.exec(REQUEST_WITH_TAG_INVALID_VM_UUID, this.session);
    }

    @Test
    public void testExec_WithRequestWithVmUuid_ExpectsSuccess() throws Exception {
        // Act.
        TagVmResponse response = this.tagVmService.exec(REQUEST_WITH_VM_UUID, this.session);

        // Assert.
        Assert.assertEquals("The tag in request with VmUuid and default tag is different than expected.", BaseTagVmService.DEFAULT_OSC_SECURITY_TAG, response.getVmTag());
    }

    @Test
    public void testExec_WithRequestWithVmUuidAndTag_ExpectsSuccess() throws Exception {
        // Act.
        TagVmResponse response = this.tagVmService.exec(REQUEST_WITH_TAG_AND_VM_UUID, this.session);

        // Assert.
        Assert.assertEquals("The tag in request with VmUuid and custom tag is different than expected.", TAG, response.getVmTag());
    }

    @Test
    public void testExec_WithRequestWithIpAddressWithoutVm_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("VM with IP address '" + INVALID_IP_ADDRESS + "' not found.");

        // Act.
        this.tagVmService.exec(REQUEST_WITH_TAG_AND_INVALID_IP_ADDRESS, this.session);
    }

    @Test
    public void testExec_WithRequestWithIpAddress_ExpectsSuccess() throws Exception {
        // Act.
        TagVmResponse response = this.tagVmService.exec(REQUEST_WITH_IP_ADDRESS, this.session);

        // Assert..
        Assert.assertEquals("The tag in request with IpAddress and default tag is different than expected.", BaseTagVmService.DEFAULT_OSC_SECURITY_TAG, response.getVmTag());
    }

    @Test
    public void testExec_WithRequestWithIpAddressAndTag_ExpectsSuccess() throws Exception {
        // Act.
        TagVmResponse response = this.tagVmService.exec(REQUEST_WITH_TAG_AND_IP_ADDRESS, this.session);

        // Assert.
        Assert.assertEquals("The tag in request with IpAddress and custom tag is different than expected.", TAG, response.getVmTag());
    }
}