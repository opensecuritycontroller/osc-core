/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import static org.osc.core.test.util.ErrorMessageConstants.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.RoleType;

public class UserDtoValidatorTestData {
    static Object[] getInvalidRoleTestData() {
        UserDto user = createUserDto();
        user.setRole(null);

        return new Object[] {user, "role " + EMPTY_VALUE_ERROR_MESSAGE};
    }

    static List<Object[]> getInvalidEmailTestData() {
        String[] invalidEmails = new String[]{"email",
                "email.com",
                "email@@email.com",
                "email_mail",
                "@email.com",
                "email@email",
                "email.email",
                "email foo@email.com",
                StringUtils.leftPad("email@email.com", 156, "e")};

        List<Object[]> result = new ArrayList<Object[]>();
        String invalidEmaielMessage = "Email: {0} has invalid format.";

        for(String invalidEmail : invalidEmails) {
            UserDto user = createUserDto();
            user.setEmail(invalidEmail);
            String errorMessage = invalidEmail.length() > 155 ?
                    "Email " + INVALID_FIELD_LENGTH_ERROR_MESSAGE :
                        MessageFormat.format(invalidEmaielMessage, invalidEmail);

            result.add(new Object[] {user,  errorMessage});
        }

        return result;
    }

    static List<Object[]> getInvalidPasswordTestData() {
        String[] invalidPasswords = new String[]{null, "", "123", "abc", "ABC", "123abc", "ab#", "Ad123#!"};
        List<Object[]> result = new ArrayList<Object[]>();
        for(String invalidPassword : invalidPasswords) {
            UserDto user = createUserDto();
            user.setPassword(invalidPassword);
            String errorMessage = invalidPassword == null || invalidPassword == "" ?
                    "password " + EMPTY_VALUE_ERROR_MESSAGE :
                        "Password must be at least 8 characters but not more than 155 characters and contain at least one lower case letter, one upper case letter, one digit and one special character (!@#$%^&+=_) with no white spaces allowed.";

            result.add(new Object[] {user,  errorMessage});
        }

        return result;
    }

    static List<Object[]> getInvalidLoginNameTestData() {
        String[] invalidLoginNames = new String[]{null, "", "ab#", "ab c", "abc9_ "};
        List<Object[]> result = new ArrayList<Object[]>();

        String invalidloginNameMessage = "Username: {0} has invalid format.";

        for(String invalidLoginName : invalidLoginNames) {
            UserDto user = createUserDto();
            user.setLoginName(invalidLoginName);

            String errorMessage = invalidLoginName == null || invalidLoginName == "" ?
                    "User Name " + EMPTY_VALUE_ERROR_MESSAGE :
                        MessageFormat.format(invalidloginNameMessage, invalidLoginName);

            result.add(new Object[] {user,  errorMessage});
        }

        return result;
    }

    static Object[] getInvalidFirstNameTestData() {
        UserDto user = createUserDto();
        user.setFirstName(StringUtils.rightPad("FirstName", 156, 'e'));

        return new Object[] {user, "First Name " + INVALID_FIELD_LENGTH_ERROR_MESSAGE};
    }

    static Object[] getInvalidLastNameTestData() {
        UserDto user = createUserDto();
        user.setLastName(StringUtils.rightPad("LastName", 156, 'e'));

        return new Object[] {user, "Last Name " + INVALID_FIELD_LENGTH_ERROR_MESSAGE};
    }

    static UserDto createUserDto() {
        return createUserDto("loginName");
    }

    static UserDto createUserDto(String loginName) {
        UserDto user = new UserDto();
        user.setLoginName(loginName);
        user.setPassword("Admin123#!");
        user.setRole(RoleType.ADMIN);
        user.setFirstName("FirstName");
        user.setLastName("LastName");
        user.setEmail("myemail@email.com");

        return user;
    }
}
