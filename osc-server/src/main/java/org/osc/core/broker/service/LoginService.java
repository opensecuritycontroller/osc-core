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
import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.api.LoginServiceApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DatabaseUtils;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.LoginRequest;
import org.osc.core.broker.service.response.LoginResponse;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

public class LoginService extends ServiceDispatcher<LoginRequest, LoginResponse> implements LoginServiceApi {
    private static final Logger LOG = Logger.getLogger(LoginService.class);

    @Override
    public LoginResponse exec(LoginRequest request, EntityManager em) throws Exception {

        validate(em, request);
        // Initializing Entity Manager
        OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, em);
        User user = emgr.findByFieldName("loginName", request.getLoginName());
        if (user == null) {
            throw new VmidcException("Wrong username and/or password! Please try again.");
        } else if (!user.getRole().equals(RoleType.ADMIN)) {
            // Wrong user role
            throw new VmidcException("Wrong username and/or password! Please try again.");
        } else {
            VmidcException invalidUserPasswordException = new VmidcException("Wrong username and/or password! Please try again.");
            try {
                if (!EncryptionUtil.validateAESCTR(request.getPassword(), user.getPassword())) {
                    // Wrong password
                    throw invalidUserPasswordException;
                }
            } catch(EncryptionException e) {
                LOG.error("User/password validation failed", e);
                throw invalidUserPasswordException;
            }
        }
        // authentication successful sending user Id in the response
        LoginResponse response = new LoginResponse();
        response.setUserID(user.getId());
        response.setPasswordChangeNeeded(request.getPassword().equals(DatabaseUtils.DEFAULT_PASSWORD));
        return response;
    }

    void validate(EntityManager em, LoginRequest request) throws Exception {
        if (request.getLoginName() == null || request.getLoginName().isEmpty() || request.getPassword() == null
                || request.getPassword().isEmpty()) {
            throw new LoginException("Login ID or Password cannot be empty.");
        }
    }
}
