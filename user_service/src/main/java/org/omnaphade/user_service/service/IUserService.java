package org.omnaphade.user_service.service;

import org.omnaphade.user_service.dtos.ProfileCreateDTO;
import org.omnaphade.user_service.dtos.ProfileResponseDTO;
import org.omnaphade.user_service.dtos.SkillDTO;
import org.omnaphade.user_service.dtos.UserSkillDTO;

import java.util.List;

public interface IUserService {

    List<ProfileResponseDTO> getAllUsers();

    ProfileResponseDTO createUserProfile(Long userId, ProfileCreateDTO profileCreateDTO);

    ProfileResponseDTO getUserProfile(Long userId);

    ProfileResponseDTO updateUserProfile(Long userId, ProfileCreateDTO profileCreateDTO);

    List<SkillDTO> getUserSkills(Long userId);

    void addSkillToUser(Long userId, Long skillId);

    SkillDTO addSkillToUserByName(Long userId, String skillName);

    SkillDTO addSkill(Long userId, UserSkillDTO dto);

    void removeSkillFromUser(Long userId, Long skillId);

    List<SkillDTO> getAllSkills();

    SkillDTO createSkill(String name);
}