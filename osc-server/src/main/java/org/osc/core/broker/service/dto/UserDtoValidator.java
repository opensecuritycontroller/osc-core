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

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.ValidateUtil;

public class UserDtoValidator implements DtoValidator<UserDto, User> {
    EntityManager em;

    public UserDtoValidator(EntityManager em) {
        this.em = em;
    }

    @Override
    public void validateForCreate(UserDto dto) throws Exception {
        validate(dto);

        OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, this.em);

        if (emgr.isExisting("loginName", dto.getLoginName())) {
            throw new VmidcBrokerValidationException("User Login Name: " + dto.getLoginName() + " already exists.");
        }
    }

    @Override
    public User validateForUpdate(UserDto dto) throws Exception {
        BaseDto.checkForNullId(dto);

        validate(dto);

        OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, this.em);

        User user = emgr.findByPrimaryKey(dto.getId());

        if (user == null) {
            throw new VmidcBrokerValidationException("User entry with name " + dto.getLoginName() + " is not found.");
        }

        return user;
    }

    void validate(UserDto dto) throws Exception {
        UserDto.checkForNullFields(dto);
        UserDto.checkFieldLength(dto);

        //validating email address formatting
        if (!StringUtils.isBlank(dto.getEmail())) {
            ValidateUtil.checkForValidEmailAddress(dto.getEmail());
        }

        //check for validity of username format
        ValidateUtil.checkForValidUsername(dto.getLoginName());

        //check for validity of password format
        ValidateUtil.checkForValidPassword(dto.getPassword());
    }
}