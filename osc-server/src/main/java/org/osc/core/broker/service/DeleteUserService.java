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
package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.DeleteUserRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

import com.mcafee.vmidc.server.Server;

public class DeleteUserService extends ServiceDispatcher<DeleteUserRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(DeleteUserRequest request, Session session) throws Exception {

        User user = (User) session.get(User.class, request.getId());

        validate(session, request, user);

        EntityManager.delete(session, user);

        // If a user is deleted itself, all the sessions associated with that user should be ended
        Server.closeUserVaadinSessions(user.getLoginName());

        return new EmptySuccessResponse();
    }

    void validate(Session session, DeleteUserRequest request, User user) throws Exception {

        // entry must pre-exist in db
        if (user == null) {

            throw new VmidcBrokerValidationException("User entry with name " + request.getId() + " is not found.");
        }
        if (user.getLoginName().equals(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN)) {

            throw new VmidcBrokerValidationException("Cannot delete pre-configured 'admin' user.");
        }
        if (user.getLoginName().equals(AgentAuthFilter.VMIDC_AGENT_LOGIN)) {

            throw new VmidcBrokerValidationException("Cannot delete pre-configured 'agent' user.");
        }
        if (user.getLoginName().equals(NsxAuthFilter.VMIDC_NSX_LOGIN)) {

            throw new VmidcBrokerValidationException("Cannot delete pre-configured 'nsx' user.");
        }
    }
}
