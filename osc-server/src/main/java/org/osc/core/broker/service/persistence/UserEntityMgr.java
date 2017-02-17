package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

public class UserEntityMgr {

    public static User createEntity(UserDto dto) throws EncryptionException {
        User user = new User();
        toEntity(user, dto);
        return user;
    }

    public static void toEntity(User user, UserDto dto) throws EncryptionException {

        // transform from dto to entity
        user.setId(dto.getId());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setLoginName(dto.getLoginName());
        user.setPassword(EncryptionUtil.encryptAESCTR(dto.getPassword()));
        user.setEmail(dto.getEmail());
        //default value for phone
        user.setPhone("");
        user.setRole(dto.getRole());
    }

    public static void fromEntity(User user, UserDto dto) throws EncryptionException {

        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setLoginName(user.getLoginName());
        dto.setPassword(EncryptionUtil.decryptAESCTR(user.getPassword()));
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());

    }
}
