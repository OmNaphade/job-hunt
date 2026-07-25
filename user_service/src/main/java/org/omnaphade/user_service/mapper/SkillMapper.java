package org.omnaphade.user_service.mapper;

import org.omnaphade.user_service.dtos.SkillDTO;
import org.omnaphade.user_service.entities.Skill;

public class SkillMapper {

    public static SkillDTO toDTO(Skill skill) {

        SkillDTO dto = new SkillDTO();

        dto.setId(skill.getId());
        dto.setName(skill.getName());

        return dto;
    }

    public static Skill toEntity(SkillDTO dto) {

        Skill skill = new Skill();

        skill.setName(dto.getName());

        return skill;
    }

}