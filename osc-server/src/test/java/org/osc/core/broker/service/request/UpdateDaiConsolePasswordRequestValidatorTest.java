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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.SessionStub;

import com.google.common.collect.Sets;

public class UpdateDaiConsolePasswordRequestValidatorTest {

    private static final String PASSWORD = "password";

    private static final String DA_NAME = "daName";
    private static final String DAI_NAME_WITHOUT_VS = "daiNameWithoutVS";
    private static final String DAI_NAME_WITH_OTHER_VS = "daiNameWithOtherVs";
    private static final String DAI_NAME_WITH_VS = "daiNameWithVs";

    private static final Long VALID_VS_ID = 1L;
    private static final Long INVALID_VS_ID = 2L;

    private static final String VALID_VS_NAME = "vsName_" + VALID_VS_ID;
    private static final String NON_EXISTENT_VS_NAME = "nonExistentName_" + INVALID_VS_ID;
    private static final String INVALID_VS_NAME = "invalidName";

    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_DAI_WITHOUT_VS = createRequest(PASSWORD, VALID_VS_NAME, Sets.newHashSet(DAI_NAME_WITHOUT_VS));
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_DAI_WITH_OTHER_VS = createRequest(PASSWORD, VALID_VS_NAME, Sets.newHashSet(DAI_NAME_WITH_OTHER_VS));
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_DAI_WITH_VS = createRequest(PASSWORD, VALID_VS_NAME, Sets.newHashSet(DAI_NAME_WITH_VS));
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITHOUT_DAI = createRequest(PASSWORD, VALID_VS_NAME, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITHOUT_VS_NAME = createRequest(PASSWORD, null, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITH_VS_NAME_WITHOUT_VS = createRequest(PASSWORD, NON_EXISTENT_VS_NAME, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITH_INVALID_VS_NAME = createRequest(PASSWORD, INVALID_VS_NAME, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITHOUT_NEW_PASSWORD = createRequest(null, VALID_VS_NAME, null);

    private static final VirtualSystem VIRTUAL_SYSTEM = createVirtualSystem();

    private static final DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_NULL = null;
    private static final DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS = new DistributedApplianceInstance(new VirtualSystem());
    private static final DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS = new DistributedApplianceInstance(VIRTUAL_SYSTEM);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    Session session;

    private UpdateDaiConsolePasswordRequestValidator validator;
    private List<DistributedApplianceInstance> daiList;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        SessionStub sessionStub = new SessionStub(this.session);
        this.validator = new UpdateDaiConsolePasswordRequestValidator(this.session);

        Mockito.when(this.session.get(VirtualSystem.class, VALID_VS_ID)).thenReturn(VIRTUAL_SYSTEM);

        sessionStub.stubFindByFieldName("name", DAI_NAME_WITHOUT_VS, DISTRIBUTED_APPLIANCE_INSTANCE_NULL);
        sessionStub.stubFindByFieldName("name", DAI_NAME_WITH_OTHER_VS, DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS);
        sessionStub.stubFindByFieldName("name", DAI_NAME_WITH_VS, DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS);

        this.daiList = new ArrayList<>();
        sessionStub.stubListByVsId(VALID_VS_ID, this.daiList);
    }

    @Test
    public void testValidate_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange
        this.exception.expect(UnsupportedOperationException.class);

        // Act
        this.validator.validate(VALID_REQUEST_WITH_DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange
        this.exception.expect(UnsupportedOperationException.class);

        // Act
        this.validator.validateAndLoad(VALID_REQUEST_WITH_DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithoutVsName_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid Virtual System Name.");

        // Act
        this.validator.validateAndLoadList(REQUEST_WITHOUT_VS_NAME);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithVsNameWithoutVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtual System with ID: " + INVALID_VS_ID + " not found.");

        // Act
        this.validator.validateAndLoadList(REQUEST_WITH_VS_NAME_WITHOUT_VS);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithInvalidVsName_NumberFormatException() throws Exception {
        // Arrange
        this.exception.expect(NumberFormatException.class);

        // Act
        this.validator.validateAndLoadList(REQUEST_WITH_INVALID_VS_NAME);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithoutNewPassword_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid password.");

        // Act
        this.validator.validateAndLoadList(REQUEST_WITHOUT_NEW_PASSWORD);
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithDaiWithOtherVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("DAI '" + DAI_NAME_WITH_OTHER_VS + "' is not a member of VSS '" + DA_NAME + "-" + VALID_VS_ID + "'.");

        // Act
        this.validator.validateAndLoadList(VALID_REQUEST_WITH_DAI_WITH_OTHER_VS);
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithDaiWithVs_ExpectsSuccess() throws Exception {
        // Act
        List<DistributedApplianceInstance> distributedApplianceInstances = this.validator.validateAndLoadList(VALID_REQUEST_WITH_DAI_WITH_VS);

        // Assert
        Assert.assertEquals("The received list size is different than expected in test with DAI.", 1, distributedApplianceInstances.size());
        Assert.assertEquals("The received list element is different than expected in test with DAI.", DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS, distributedApplianceInstances.get(0));
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithDaiWithoutVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("DAI '" + DAI_NAME_WITHOUT_VS + "' not found.");

        // Act
        this.validator.validateAndLoadList(VALID_REQUEST_WITH_DAI_WITHOUT_VS);
    }


    @Test
    public void testValidateAndLoad_WithValidRequestWithoutDaiAndDaiNotListedByVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("VSS '" + DA_NAME + "-" + VALID_VS_ID + "' does not have members.");

        // Act
        this.validator.validateAndLoadList(VALID_REQUEST_WITHOUT_DAI);
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithoutDaiAndDaiListedByVs_ExpectsCorrectDais() throws Exception {
        // Arrange
        DistributedApplianceInstance distributedApplianceInstance = new DistributedApplianceInstance(new VirtualSystem());
        this.daiList.add(distributedApplianceInstance);

        // Act
        List<DistributedApplianceInstance> distributedApplianceInstances = this.validator.validateAndLoadList(VALID_REQUEST_WITHOUT_DAI);

        // Assert
        Assert.assertEquals("The received list size is different than expected in test without DAI.", 1, distributedApplianceInstances.size());
        Assert.assertEquals("The received list element is different than expected in test without DAI.", distributedApplianceInstance, distributedApplianceInstances.get(0));
    }

    private static UpdateDaiConsolePasswordRequest createRequest(String newPassword, String vsName, Set<String> daiSet) {
        UpdateDaiConsolePasswordRequest request = new UpdateDaiConsolePasswordRequest();
        request.setNewPassword(newPassword);
        request.setVsName(vsName);
        request.setDaiList(daiSet);
        return request;
    }

    private static VirtualSystem createVirtualSystem() {
        DistributedAppliance distributedAppliance = new DistributedAppliance();
        distributedAppliance.setName(DA_NAME);
        VirtualSystem virtualSystem = new VirtualSystem(distributedAppliance);
        virtualSystem.setId(VALID_VS_ID);
        return virtualSystem;
    }
}