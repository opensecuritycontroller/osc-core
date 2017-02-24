package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.UserEntityMgr;
import org.osc.core.broker.service.request.ListUserRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListUserService extends ServiceDispatcher<ListUserRequest, ListResponse<UserDto>> {

    @Override
    public ListResponse<UserDto> exec(ListUserRequest request, Session session) throws Exception {
        // Initializing Entity Manager
        EntityManager<User> emgr = new EntityManager<User>(User.class, session);
        // to do mapping
        List<UserDto> userList = new ArrayList<UserDto>();

        // mapping all the User objects to user dto objects
        for (User user : emgr.listAll(new Order[] { Order.asc("loginName") })) {
            UserDto dto = new UserDto();
            UserEntityMgr.fromEntity(user, dto);
            userList.add(dto);
        }
        ListResponse<UserDto> response = new ListResponse<UserDto>();
        response.setList(userList);
        return response;
    }

}
