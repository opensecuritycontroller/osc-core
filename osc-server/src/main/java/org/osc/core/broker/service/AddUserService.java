package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.dto.DtoValidator;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.dto.UserDtoValidator;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.UserEntityMgr;
import org.osc.core.broker.service.request.AddUserRequest;
import org.osc.core.broker.service.response.AddUserResponse;

public class AddUserService extends ServiceDispatcher<AddUserRequest, AddUserResponse> {
    private DtoValidator<UserDto, User> validator;

    @Override
    protected AddUserResponse exec(AddUserRequest request, Session session) throws Exception {
        // Initializing Entity Manager
        EntityManager<User> emgr = new EntityManager<User>(User.class, session);

        if (this.validator == null) {
            this.validator = new UserDtoValidator(session);
        }

        this.validator.validateForCreate(request);

        User user = UserEntityMgr.createEntity(request);

        // creating new entry in the db using entity manager object
        user = emgr.create(user);

        AddUserResponse response = new AddUserResponse();
        response.setId(user.getId());

        return response;
    }
}
