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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.SessionStub;

public class AgentRegisterServiceRequestValidatorTest {
    @Mock
    private Session sessionMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private AgentRegisterServiceRequestValidator validator;
    private static DistributedApplianceInstance DAI_WITH_VS = createDai(new VirtualSystem());

    private static AgentRegisterServiceRequest REQUEST_WITH_NAME =
            createRequest("REQUEST_NAME", "REQUEST_WITH_NAME_IP", 1L);

    private static AgentRegisterServiceRequest REQUEST_DAI_FOUND_BY_IP =
            createRequest("REQUEST_DAI_FOUND_BY_IP_NAME", "REQUEST_WITHOUT_NAME_IP", 2L);

    private static AgentRegisterServiceRequest REQUEST_NO_NAME_DAI_FOUND_BY_IP =
            createRequest(null, "REQUEST_WITHOUT_NAME_IP", 3L);

    private static AgentRegisterServiceRequest REQUEST_DAI_WITHOUT_VS =
            createRequest("REQUEST_DAI_WITHOUT_VS_NAME", "REQUEST_DAI_WITHOUT_VS_IP", 4L);

    private static AgentRegisterServiceRequest REQUEST_VS_FOUND =
            createRequest("REQUEST_DAI_VS_FOUND_NAME", "REQUEST_DAI_VS_FOUND_IP", 5L);

    private static AgentRegisterServiceRequest REQUEST_VS_NOT_FOUND =
            createRequest("REQUEST_VS_NOT_FOUND_NAME", "REQUEST_VS_NOT_FOUND_IP", 6L);

    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);
        SessionStub sessionStub = new SessionStub(this.sessionMock);

        this.validator = new AgentRegisterServiceRequestValidator(this.sessionMock);

        sessionStub.stubFindByFieldName("name", REQUEST_WITH_NAME.getName(), DAI_WITH_VS);
        sessionStub.stubFindByFieldName("ipAddress", REQUEST_DAI_FOUND_BY_IP.getApplianceIp(), DAI_WITH_VS);

        when(this.sessionMock.get(VirtualSystem.class, REQUEST_VS_FOUND.getVsId())).thenReturn(new VirtualSystem());
    }

    @Test
    public void testValidateAndLoad_WithNullRequest_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.validator.validateAndLoad(null);
    }

    @Test
    public void testValidateAndLoad_WithoutIpAddress_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Missing agent IP address.");

        // Act.
        this.validator.validateAndLoad(new AgentRegisterServiceRequest());
    }

    @Test
    public void testValidateAndLoad_WithoutVirtualSystemId_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid virtual system identifier.");

        AgentRegisterServiceRequest request = new AgentRegisterServiceRequest();
        request.setApplianceIp("10.1.1.1");

        // Act.
        this.validator.validateAndLoad(request);
    }

    @Test
    public void testValidateAndLoad_WhenDaiFoundByName_ExpectsRespectiveDai() throws Exception {
        testValidateAndLoand_ExpectsRespectiveDai(REQUEST_WITH_NAME, DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WhenDaiFoundByIp_ExpectsRespectiveDai() throws Exception {
        testValidateAndLoand_ExpectsRespectiveDai(REQUEST_DAI_FOUND_BY_IP, DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WithoutNameDaiFoundByIp_ExpectsRespectiveDai() throws Exception {
        testValidateAndLoand_ExpectsRespectiveDai(REQUEST_NO_NAME_DAI_FOUND_BY_IP, DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_DaiWithoutVs_ThrowsValidationException() throws Exception {
        testValidateAndLoad_NoVs(REQUEST_DAI_WITHOUT_VS);
    }

    @Test
    public void testValidateAndLoad_WhenVsNotFound_ThrowsValidationException() throws Exception {
        testValidateAndLoad_NoVs(REQUEST_VS_NOT_FOUND);
    }

    private void testValidateAndLoad_NoVs(AgentRegisterServiceRequest request) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("VS ID " + request.getVsId() + " not found.");

        // Act.
        this.validator.validateAndLoad(request);
    }

    private void testValidateAndLoand_ExpectsRespectiveDai(AgentRegisterServiceRequest request, DistributedApplianceInstance expectedDai) throws Exception {
        // Act.
        DistributedApplianceInstance dai = this.validator.validateAndLoad(request);

        // Assert.
        assertThat(dai).as("dai").isNotNull();
        assertThat(dai).isEqualTo(expectedDai);

        verify(this.sessionMock, times(0)).get(any(Class.class), any(Serializable.class));
    }

    private static AgentRegisterServiceRequest createRequest(String name, String ipAddress, Long vsId) {
        AgentRegisterServiceRequest request = new AgentRegisterServiceRequest();
        request.setName(name);
        request.setApplianceIp(ipAddress);
        request.setVirtualSystemId(vsId);

        return request;
    }

    private static DistributedApplianceInstance createDai(VirtualSystem vs) {
        DistributedApplianceInstance dai = new DistributedApplianceInstance(vs);
        return dai;
    }
}
