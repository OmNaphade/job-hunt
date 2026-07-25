package org.omnaphade.auth_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequestDTO {
    private String refreshToken;
}
