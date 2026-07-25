package org.omnaphade.auth_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDTO {

    private Long id;
    private String email;
    private String role;
    private String status;

}
