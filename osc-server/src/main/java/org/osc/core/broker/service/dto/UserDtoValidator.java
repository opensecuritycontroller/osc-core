package org.osc.core.broker.service.dto;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.ValidateUtil;

public class UserDtoValidator implements DtoValidator<UserDto, User> {
    Session session;

    public UserDtoValidator(Session session) {
        this.session = session;
    }

    @Override
    public void validateForCreate(UserDto dto) throws Exception {
        validate(dto);

        EntityManager<User> emgr = new EntityManager<User>(User.class, this.session);

        if (emgr.isExisting("loginName", dto.getLoginName())) {
            throw new VmidcBrokerValidationException("User Login Name: " + dto.getLoginName() + " already exists.");
        }
    }

    @Override
    public User validateForUpdate(UserDto dto) throws Exception {
        BaseDto.checkForNullId(dto);

        validate(dto);

        EntityManager<User> emgr = new EntityManager<User>(User.class, this.session);

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