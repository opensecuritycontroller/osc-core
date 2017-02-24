package org.osc.core.broker.service;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DatabaseUtils;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.LoginRequest;
import org.osc.core.broker.service.response.LoginResponse;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

public class LoginService extends ServiceDispatcher<LoginRequest, LoginResponse> {
    private static final Logger LOG = Logger.getLogger(LoginService.class);

    @Override
    public LoginResponse exec(LoginRequest request, Session session) throws Exception {

        validate(session, request);
        // Initializing Entity Manager
        EntityManager<User> emgr = new EntityManager<User>(User.class, session);
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

    void validate(Session session, LoginRequest request) throws Exception {
        if (request.getLoginName() == null || request.getLoginName().isEmpty() || request.getPassword() == null
                || request.getPassword().isEmpty()) {
            throw new LoginException("Login ID or Password cannot be empty.");
        }
    }
}
