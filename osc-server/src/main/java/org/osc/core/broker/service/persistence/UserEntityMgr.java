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
package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.UserDto;

public class UserEntityMgr {

    public static User createEntity(UserDto dto, EncryptionApi encryption) throws EncryptionException {
        User user = new User();
        toEntity(user, dto, encryption);
        return user;
    }

    public static void toEntity(User user, UserDto dto, EncryptionApi encryption) throws EncryptionException {

        // transform from dto to entity
        user.setId(dto.getId());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setLoginName(dto.getLoginName());
        user.setPassword(encryption.encryptAESCTR(dto.getPassword()));
        user.setEmail(dto.getEmail());
        //default value for phone
        user.setPhone("");
        String role = dto.getRole();
        user.setRole(role == null ? null : RoleType.valueOf(role));
    }

    public static void fromEntity(User user, UserDto dto, EncryptionApi encryption) throws EncryptionException {

        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setLoginName(user.getLoginName());
        dto.setPassword(encryption.decryptAESCTR(user.getPassword()));
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());

    }
}
