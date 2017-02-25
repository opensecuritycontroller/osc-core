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

import static org.osc.core.broker.service.dto.UserDtoValidatorTestData.*;
import static org.osc.core.test.util.ErrorMessageConstants.EMPTY_VALUE_ERROR_MESSAGE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.dto.UserDtoValidator;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.SessionStub;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class UserDtoValidatorTest {
    @Mock
    private Session sessionMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private UserDto existingUserDto = createUserDto("existingUserName");
    private UserDto newUserDto = createUserDto("newUserName");
    private User existingUser;

    private UserDtoValidator validator;

    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);
        this.validator = new UserDtoValidator(this.sessionMock);

        this.existingUserDto.setId(45L);
        this.existingUser = new User();
        this.existingUser.setId(this.existingUserDto.getId());

        SessionStub mockedSession = new SessionStub(this.sessionMock);

        mockedSession.stubIsExistingEntity(User.class, "loginName", this.existingUserDto.getLoginName(), true);

        mockedSession.stubIsExistingEntity(User.class, "loginName", this.newUserDto.getLoginName(), false);

        Mockito.when(this.sessionMock.get(User.class, this.existingUserDto.getId())).thenReturn(this.existingUser);
    }

    @Test
    @Parameters(method = "getInvalidFieldsTestData")
    public void testValidate_UsingInvalidField_ThrowsInvalidEntryException(UserDto dto, String expectedErrorMessage) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(expectedErrorMessage);

        // Act.
        this.validator.validate(dto);
    }

    @Test
    public void testValidateForCreate_WithExistingUser_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(MessageFormat.format("User Login Name: {0} already exists.", this.existingUserDto.getLoginName()));

        // Act.
        this.validator.validateForCreate(this.existingUserDto);
    }

    @Test
    public void testValidateForCreate_WithValidNewUser_ValidationSucceeds() throws Exception {
        // Act.
        this.validator.validateForCreate(this.newUserDto);
    }

    @Test
    public void testValidateForUpdate_WithValidExistingUser_ReturnsExistingUser() throws Exception {
        // Arrange.
        User response = null;

        // Act.
        response = this.validator.validateForUpdate(this.existingUserDto);

        // Assert.
        Assert.assertNotNull("The validation response should not be null.", response);
        Assert.assertEquals("The user id was different than expected.", this.existingUserDto.getId(), response.getId());
    }

    @Test
    public void testValidateForUpdate_WithUserMissingId_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage("Id " + EMPTY_VALUE_ERROR_MESSAGE);

        // Act.
        this.validator.validateForUpdate(this.newUserDto);
    }

    @Test
    public void testValidateForUpdate_WhenUserNotFound_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        Long idNotFound = 10L;
        this.existingUserDto.setId(idNotFound);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(MessageFormat.format("User entry with name {0} is not found.", this.existingUserDto.getLoginName()));

        // Act.
        this.validator.validateForUpdate(this.existingUserDto);
    }

    public Object[] getInvalidFieldsTestData() {
        List<Object[]> result = new ArrayList<Object[]>();
        result.addAll(getInvalidPasswordTestData());
        result.addAll(getInvalidLoginNameTestData());
        result.addAll(getInvalidEmailTestData());
        result.add(getInvalidRoleTestData());
        result.add(getInvalidFirstNameTestData());
        result.add(getInvalidLastNameTestData());
        return result.toArray();
    }
}
