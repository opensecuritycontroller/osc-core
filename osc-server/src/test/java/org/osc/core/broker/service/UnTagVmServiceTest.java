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

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

public class UnTagVmServiceTest extends BaseTagVmServiceTest {

    @InjectMocks
    UnTagVmService unTagVmService;

    @Test
    public void testExec_WithRequestWithoutVmUuid_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid VM Uuid.");

        // Act.
        this.unTagVmService.exec(REQUEST_WITH_TAG, this.em);
    }

    @Test
    public void testExec_WithRequestWithVmUuidWithoutVm_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("VM with Uuid '" + INVALID_VM_UUID + "' not found.");

        // Act.
        this.unTagVmService.exec(REQUEST_WITH_TAG_INVALID_VM_UUID, this.em);
    }

    @Test
    public void testExec_WithValidRequest_ExpectsSuccess() throws Exception {
        // Act.
        this.unTagVmService.exec(REQUEST_WITH_TAG_AND_VM_UUID, this.em);

        // Assert.
        Mockito.verify(this.securityTagApi, Mockito.times(1)).removeSecurityTagFromVM(REQUEST_WITH_TAG_AND_VM_UUID.getVmUuid(), BaseTagVmService.DEFAULT_OSC_SECURITY_TAG);
    }
}