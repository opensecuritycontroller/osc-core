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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.api.AddUserServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.UserEntityMgr;
import org.osc.core.broker.service.request.AddUserRequest;
import org.osc.core.broker.service.response.AddUserResponse;
import org.osc.core.broker.service.validator.DtoValidator;
import org.osc.core.broker.service.validator.UserDtoValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AddUserService extends ServiceDispatcher<AddUserRequest, AddUserResponse> implements AddUserServiceApi {
    private DtoValidator<UserDto, User> validator;

    @Reference
    private EncryptionApi encrypter;

    @Override
    protected AddUserResponse exec(AddUserRequest request, EntityManager em) throws Exception {
        // Initializing Entity Manager
        OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, em, this.txBroadcastUtil);

        if (this.validator == null) {
            this.validator = new UserDtoValidator(em, this.txBroadcastUtil);
        }

        this.validator.validateForCreate(request);

        User user = UserEntityMgr.createEntity(request, this.encrypter);

        // creating new entry in the db using entity manager object
        user = emgr.create(user);

        AddUserResponse response = new AddUserResponse();
        response.setId(user.getId());

        return response;
    }
}
