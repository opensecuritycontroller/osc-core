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
import org.osc.core.broker.rest.RestConstants;
import org.osc.core.broker.service.api.DeleteUserServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.DeleteUserRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.util.PasswordUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DeleteUserService extends ServiceDispatcher<DeleteUserRequest, EmptySuccessResponse>
        implements DeleteUserServiceApi {

    @Reference
    private PasswordUtil passwordUtil;

    @Override
    public EmptySuccessResponse exec(DeleteUserRequest request, EntityManager em) throws Exception {

        User user = em.find(User.class, request.getId());

        validate(em, request, user);

        // If a user is deleted then all the sessions associated with
        // that user should be ended. The broadcast of the entity deletion
        // takes care of this
        OSCEntityManager.delete(em, user);

        return new EmptySuccessResponse();
    }

    void validate(EntityManager em, DeleteUserRequest request, User user) throws Exception {

        // entry must pre-exist in db
        if (user == null) {

            throw new VmidcBrokerValidationException("User entry with name " + request.getId() + " is not found.");
        }
        if (user.getLoginName().equals(RestConstants.OSC_DEFAULT_LOGIN)) {

            throw new VmidcBrokerValidationException("Cannot delete pre-configured 'admin' user.");
        }
        if (user.getLoginName().equals(RestConstants.VMIDC_AGENT_LOGIN)) {

            throw new VmidcBrokerValidationException("Cannot delete pre-configured 'agent' user.");
        }
        if (user.getLoginName().equals(RestConstants.VMIDC_NSX_LOGIN)) {

            throw new VmidcBrokerValidationException("Cannot delete pre-configured 'nsx' user.");
        }
    }
}
