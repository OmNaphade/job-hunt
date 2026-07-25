package org.omnaphade.user_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.user_service.dtos.ProfileCreateDTO;
import org.omnaphade.user_service.dtos.ProfileResponseDTO;
import org.omnaphade.user_service.dtos.SkillDTO;
import org.omnaphade.user_service.dtos.UserSkillDTO;
import org.omnaphade.user_service.entities.Profile;
import org.omnaphade.user_service.entities.Skill;
import org.omnaphade.user_service.entities.UserSkill;
import org.omnaphade.user_service.exception.DuplicateResourceException;
import org.omnaphade.user_service.exception.ResourceNotFoundException;
import org.omnaphade.user_service.mapper.ProfileMapper;
import org.omnaphade.user_service.mapper.SkillMapper;
import org.omnaphade.user_service.repository.SkillRepository;
import org.omnaphade.user_service.repository.UserRepository;
import org.omnaphade.user_service.repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    private final UserSkillRepository userSkillRepository;

    private final SkillRepository skillRepository;

    @Override
    public List<ProfileResponseDTO> getAllUsers() {

        List<Profile> profiles = userRepository.findAll();
        List<ProfileResponseDTO> response = new ArrayList<>();

        for (Profile profile : profiles) {
            response.add(ProfileMapper.toResponseDTO(profile));
        }

        return response;
    }

    @Override
    public ProfileResponseDTO createUserProfile(Long userId, ProfileCreateDTO dto) {

        if (userRepository.findByUserId(userId).isPresent()) {
            throw new DuplicateResourceException("Profile already exists for userId: " + userId);
        }

        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setHeadline(dto.getHeadline());
        profile.setSummary(dto.getSummary());
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setCurrentLocation(dto.getCurrentLocation());

        Profile savedProfile = userRepository.save(profile);

        return ProfileMapper.toResponseDTO(savedProfile);
    }

    @Override
    public ProfileResponseDTO getUserProfile(Long userId) {

        Profile profile = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + userId));

        return ProfileMapper.toResponseDTO(profile);
    }

    @Override
    public ProfileResponseDTO updateUserProfile(Long userId, ProfileCreateDTO dto) {

        Profile profile = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + userId));

        profile.setHeadline(dto.getHeadline());
        profile.setSummary(dto.getSummary());
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setCurrentLocation(dto.getCurrentLocation());

        Profile updatedProfile = userRepository.save(profile);

        return ProfileMapper.toResponseDTO(updatedProfile);
    }

    @Override
    public List<SkillDTO> getUserSkills(Long userId) {

        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);
        List<Long> skillIds = userSkills.stream().map(UserSkill::getSkillId).toList();
        List<Skill> skills = skillRepository.findAllById(skillIds);

        return skills.stream().map(SkillMapper::toDTO).toList();
    }

    @Override
    public void addSkillToUser(Long userId, Long skillId) {

        UserSkill existing = userSkillRepository.findByUserIdAndSkillId(userId, skillId);

        if (existing != null) {
            throw new DuplicateResourceException("Skill already assigned to user");
        }

        UserSkill userSkill = new UserSkill();
        userSkill.setUserId(userId);
        userSkill.setSkillId(skillId);

        userSkillRepository.save(userSkill);
    }

    @Override
    public SkillDTO addSkillToUserByName(Long userId, String skillName) {
        Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                .orElseGet(() -> {
                    Skill s = new Skill();
                    s.setName(skillName);
                    return skillRepository.save(s);
                });

        if (userSkillRepository.findByUserIdAndSkillId(userId, skill.getId()) == null) {
            UserSkill us = new UserSkill();
            us.setUserId(userId);
            us.setSkillId(skill.getId());
            userSkillRepository.save(us);
        }
        return SkillMapper.toDTO(skill);
    }

    @Override
    public SkillDTO addSkill(Long userId, UserSkillDTO dto) {
        if (dto.getSkillName() != null && !dto.getSkillName().isBlank()) {
            return addSkillToUserByName(userId, dto.getSkillName());
        }
        if (dto.getSkillId() != null) {
            addSkillToUser(userId, dto.getSkillId());
            return skillRepository.findById(dto.getSkillId())
                    .map(SkillMapper::toDTO)
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + dto.getSkillId()));
        }
        throw new IllegalArgumentException("Either skillName or skillId must be provided");
    }

    @Override
    public List<SkillDTO> getAllSkills() {
        return skillRepository.findAll().stream().map(SkillMapper::toDTO).toList();
    }

    @Override
    public SkillDTO createSkill(String name) {
        Skill skill = skillRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Skill s = new Skill();
            s.setName(name);
            return skillRepository.save(s);
        });
        return SkillMapper.toDTO(skill);
    }

    @Override
    public void removeSkillFromUser(Long userId, Long skillId) {

        UserSkill userSkill = userSkillRepository.findByUserIdAndSkillId(userId, skillId);

        if (userSkill == null) {
            throw new ResourceNotFoundException("Skill not found for user");
        }

        userSkillRepository.delete(userSkill);
    }
}