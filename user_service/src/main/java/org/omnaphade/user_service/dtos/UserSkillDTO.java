package org.omnaphade.user_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSkillDTO {

    private Long userId;
    private Long skillId;
    private String skillName;
}