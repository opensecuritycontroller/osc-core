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
package org.osc.core.broker.service.request;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.validator.AddVirtualizationConnectorServiceRequestValidator;
import org.osc.core.broker.service.validator.DtoValidator;
import org.osc.core.broker.service.validator.VirtualizationConnectorDtoValidatorTestData;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ StaticRegistry.class})
public class AddVirtualizationConnectorServiceRequestValidatorTest {
    @Mock
    private EntityManager em;

    @Mock
    private DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> dtoValidator;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private VirtualizationConnectorUtil virtualizationConnectorUtil;

    @Mock
    private EncryptionApi encryption;

    @InjectMocks
    private AddVirtualizationConnectorServiceRequestValidator validator;

    private DryRunRequest<VirtualizationConnectorRequest> request =
            new DryRunRequest<VirtualizationConnectorRequest>((VirtualizationConnectorRequest)VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithSDN());

    @Before
    public void testInitialize() throws EncryptionException {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(StaticRegistry.class);
        when(StaticRegistry.encryptionApi()).thenReturn(this.encryption);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidate_WithValidOpenStackRequest_ReturnsSuccess() throws Exception {
        // Arrange.
        doNothing().when(this.dtoValidator).validateForCreate(this.request.getDto());
        doNothing().when(this.virtualizationConnectorUtil).checkConnection(any(DryRunRequest.class), any(VirtualizationConnector.class));

        // Act.
        this.validator.validate(this.request);

        // Assert.
        verify(this.dtoValidator).validateForCreate(this.request.getDto());
    }

    @Test
    public void testValidate_WithNullRequest_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.validator.validate(null);
    }

    @Test
    public void testValidate_WithInvalidOpenStackRequest_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        doThrow(VmidcBrokerValidationException.class).when(this.dtoValidator).validateForCreate(this.request.getDto());

        // Act.
        this.validator.validate(this.request);

        // Assert.
        verify(this.dtoValidator).validateForCreate(this.request.getDto());
    }

    @Test
    public void testValidate_WithValidateAndLoadRequest_ThrowsUnsupportedException() throws Exception {
        // Arrange.
        this.exception.expect(UnsupportedOperationException.class);

        // Act.
        this.validator.validateAndLoad(this.request);
    }

}