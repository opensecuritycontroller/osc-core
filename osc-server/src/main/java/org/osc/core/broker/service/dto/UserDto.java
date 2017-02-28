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

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.util.ValidateUtil;

public class UserDto extends BaseDto {

    private String firstName;
    private String lastName;
    private String loginName;
    private String password;
    private String email;
    private RoleType role;

    @Override
    public String toString() {
        return "UserDto [id=" + getId() + ", firstName=" + firstName + ", lastName=" + lastName + ", loginName=" + loginName
                + ", password=******, email=" + email + ", role=" + role + "]";
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public static void checkForNullFields(UserDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("User Name", dto.getLoginName());
        map.put("password", dto.getPassword());
        map.put("role", dto.getRole());

        ValidateUtil.checkForNullFields(map);

    }

    public static void checkFieldLength(UserDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("First Name", dto.getFirstName());
        map.put("Last Name", dto.getLastName());
        map.put("Email", dto.getEmail());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);

    }
}
