package org.omnaphade.auth_service.mapper;

import lombok.Getter;
import lombok.Setter;
import org.omnaphade.auth_service.dtos.UserDTO;
import org.omnaphade.auth_service.entities.User;

@Setter
@Getter
public class UserMapper {

    public static UserDTO toDTO(User user) {

        UserDTO dto = new UserDTO();

        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus());

        return dto;
    }

}