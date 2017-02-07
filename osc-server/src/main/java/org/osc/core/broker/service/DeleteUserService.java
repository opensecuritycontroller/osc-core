package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.OscAuthFilter;
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
        if (user.getLoginName().equals(OscAuthFilter.OSC_DEFAULT_LOGIN)) {

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
