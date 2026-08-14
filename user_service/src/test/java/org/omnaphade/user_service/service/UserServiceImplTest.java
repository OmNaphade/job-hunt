package org.omnaphade.user_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omnaphade.user_service.dtos.ProfileCreateDTO;
import org.omnaphade.user_service.dtos.ProfileResponseDTO;
import org.omnaphade.user_service.dtos.SkillDTO;
import org.omnaphade.user_service.dtos.UserSkillDTO;
import org.omnaphade.user_service.entities.Profile;
import org.omnaphade.user_service.entities.Skill;
import org.omnaphade.user_service.entities.UserSkill;
import org.omnaphade.user_service.exception.BadRequestException;
import org.omnaphade.user_service.exception.DuplicateResourceException;
import org.omnaphade.user_service.exception.ResourceNotFoundException;
import org.omnaphade.user_service.repository.SkillRepository;
import org.omnaphade.user_service.repository.UserRepository;
import org.omnaphade.user_service.repository.UserSkillRepository;
import org.omnaphade.user_service.storage.FileStorageService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private FileStorageService fileStorageService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userSkillRepository, skillRepository, fileStorageService);
    }

    private ProfileCreateDTO sampleCreateDTO() {
        ProfileCreateDTO dto = new ProfileCreateDTO();
        dto.setHeadline("Backend Engineer");
        dto.setSummary("5 years of Java experience");
        dto.setExperienceYears(5);
        dto.setCurrentLocation("Pune, India");
        return dto;
    }

    private Profile savedProfile() {
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setUserId(9L);
        profile.setHeadline("Backend Engineer");
        profile.setSummary("5 years of Java experience");
        profile.setExperienceYears(5);
        profile.setCurrentLocation("Pune, India");
        return profile;
    }

    @Test
    void createUserProfile_whenNoneExists_persistsAndReturnsProfile() {
        when(userRepository.findByUserId(9L)).thenReturn(Optional.empty());
        when(userRepository.save(any(Profile.class))).thenReturn(savedProfile());

        ProfileResponseDTO result = userService.createUserProfile(9L, sampleCreateDTO());

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getHeadline()).isEqualTo("Backend Engineer");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void createUserProfile_whenProfileAlreadyExists_throwsDuplicateResourceException() {
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(savedProfile()));

        assertThatThrownBy(() -> userService.createUserProfile(9L, sampleCreateDTO()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("9");

        verify(userRepository, never()).save(any(Profile.class));
    }

    @Test
    void getUserProfile_found_returnsMappedProfile() {
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(savedProfile()));

        ProfileResponseDTO result = userService.getUserProfile(9L);

        assertThat(result.getUserId()).isEqualTo(9L);
        assertThat(result.getCurrentLocation()).isEqualTo("Pune, India");
    }

    @Test
    void getUserProfile_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUserProfile_whenFound_overwritesFieldsAndSaves() {
        Profile existing = savedProfile();
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileCreateDTO dto = sampleCreateDTO();
        dto.setHeadline("Staff Engineer");
        dto.setExperienceYears(8);

        ProfileResponseDTO result = userService.updateUserProfile(9L, dto);

        assertThat(result.getHeadline()).isEqualTo("Staff Engineer");
        assertThat(result.getExperienceYears()).isEqualTo(8);
        assertThat(existing.getHeadline()).isEqualTo("Staff Engineer");
    }

    @Test
    void updateUserProfile_whenNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserProfile(99L, sampleCreateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserSkills_returnsMappedSkillsForUser() {
        when(userSkillRepository.findByUserId(9L)).thenReturn(List.of(
                skill(101L, 1L), skill(102L, 2L)
        ));
        when(skillRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                namedSkill(1L, "Java"), namedSkill(2L, "Kafka")
        ));

        List<SkillDTO> result = userService.getUserSkills(9L);

        assertThat(result).extracting(SkillDTO::getName).containsExactlyInAnyOrder("Java", "Kafka");
    }

    private UserSkill skill(Long id, Long skillId) {
        UserSkill us = new UserSkill();
        us.setId(id);
        us.setUserId(9L);
        us.setSkillId(skillId);
        return us;
    }

    private Skill namedSkill(Long id, String name) {
        Skill s = new Skill();
        s.setId(id);
        s.setName(name);
        return s;
    }

    @Test
    void addSkillToUser_whenNotAlreadyAssigned_persistsAssociation() {
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(null);

        userService.addSkillToUser(9L, 1L);

        ArgumentCaptor<UserSkill> captor = ArgumentCaptor.forClass(UserSkill.class);
        verify(userSkillRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getSkillId()).isEqualTo(1L);
    }

    @Test
    void addSkillToUser_whenAlreadyAssigned_throwsDuplicateResourceException() {
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(skill(5L, 1L));

        assertThatThrownBy(() -> userService.addSkillToUser(9L, 1L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userSkillRepository, never()).save(any(UserSkill.class));
    }

    @Test
    void addSkillToUserByName_whenSkillDoesNotExist_createsSkillAndAssociation() {
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(namedSkill(1L, "Java"));
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(null);

        SkillDTO result = userService.addSkillToUserByName(9L, "Java");

        assertThat(result.getName()).isEqualTo("Java");
        verify(skillRepository).save(any(Skill.class));
        verify(userSkillRepository).save(any(UserSkill.class));
    }

    @Test
    void addSkillToUserByName_whenSkillExistsAndAlreadyAssigned_doesNotDuplicateAssociation() {
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(namedSkill(1L, "Java")));
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(skill(5L, 1L));

        userService.addSkillToUserByName(9L, "Java");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(userSkillRepository, never()).save(any(UserSkill.class));
    }

    @Test
    void addSkill_withSkillName_delegatesToAddSkillToUserByName() {
        UserSkillDTO dto = new UserSkillDTO();
        dto.setSkillName("Python");
        when(skillRepository.findByNameIgnoreCase("Python")).thenReturn(Optional.of(namedSkill(3L, "Python")));
        when(userSkillRepository.findByUserIdAndSkillId(9L, 3L)).thenReturn(null);

        SkillDTO result = userService.addSkill(9L, dto);

        assertThat(result.getName()).isEqualTo("Python");
    }

    @Test
    void addSkill_withSkillId_delegatesToAddSkillToUser() {
        UserSkillDTO dto = new UserSkillDTO();
        dto.setSkillId(1L);
        Long skillId = 1L;
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(null);
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(namedSkill(1L, "Java")));

        SkillDTO result = userService.addSkill(9L, dto);

        assertThat(result.getName()).isEqualTo("Java");
        verify(userSkillRepository).save(any(UserSkill.class));
    }

    @Test
    void addSkill_withNeitherNameNorId_throwsIllegalArgumentException() {
        UserSkillDTO dto = new UserSkillDTO();

        assertThatThrownBy(() -> userService.addSkill(9L, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeSkillFromUser_whenAssigned_deletesAssociation() {
        UserSkill existing = skill(5L, 1L);
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(existing);

        userService.removeSkillFromUser(9L, 1L);

        verify(userSkillRepository).delete(existing);
    }

    @Test
    void removeSkillFromUser_whenNotAssigned_throwsResourceNotFoundException() {
        when(userSkillRepository.findByUserIdAndSkillId(9L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> userService.removeSkillFromUser(9L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userSkillRepository, never()).delete(any(UserSkill.class));
    }

    @Test
    void getAllSkills_returnsAllMappedSkills() {
        when(skillRepository.findAll()).thenReturn(List.of(namedSkill(1L, "Java"), namedSkill(2L, "Kafka")));

        List<SkillDTO> result = userService.getAllSkills();

        assertThat(result).extracting(SkillDTO::getName).containsExactly("Java", "Kafka");
    }

    @Test
    void createSkill_whenNameDoesNotExist_createsNewSkill() {
        when(skillRepository.findByNameIgnoreCase("Rust")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(namedSkill(4L, "Rust"));

        SkillDTO result = userService.createSkill("Rust");

        assertThat(result.getName()).isEqualTo("Rust");
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    void createSkill_whenNameAlreadyExists_returnsExistingSkillWithoutSaving() {
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(namedSkill(1L, "Java")));

        SkillDTO result = userService.createSkill("Java");

        assertThat(result.getId()).isEqualTo(1L);
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void uploadAvatar_whenProfileExistsAndValidImage_storesFileAndUpdatesAvatarUrl() {
        Profile existing = savedProfile();
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "content".getBytes());
        when(fileStorageService.store(file, "avatars")).thenReturn("avatars/generated-name.png");

        ProfileResponseDTO result = userService.uploadAvatar(9L, file);

        assertThat(result.getAvatarUrl()).isEqualTo("avatars/generated-name.png");
    }

    @Test
    void uploadAvatar_whenProfileDoesNotExist_throwsResourceNotFoundException() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "content".getBytes());

        assertThatThrownBy(() -> userService.uploadAvatar(99L, file))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void uploadAvatar_disallowedFileType_throwsBadRequestException() {
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(savedProfile()));
        MultipartFile file = new MockMultipartFile("file", "avatar.gif", "image/gif", "content".getBytes());

        assertThatThrownBy(() -> userService.uploadAvatar(9L, file))
                .isInstanceOf(BadRequestException.class);

        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void uploadAvatar_emptyFile_throwsBadRequestException() {
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(savedProfile()));
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> userService.uploadAvatar(9L, file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getAvatarPath_whenAvatarExists_returnsPath() {
        Profile existing = savedProfile();
        existing.setAvatarUrl("avatars/generated-name.png");
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(existing));

        String path = userService.getAvatarPath(9L);

        assertThat(path).isEqualTo("avatars/generated-name.png");
    }

    @Test
    void getAvatarPath_whenNoAvatarUploaded_throwsResourceNotFoundException() {
        when(userRepository.findByUserId(9L)).thenReturn(Optional.of(savedProfile()));

        assertThatThrownBy(() -> userService.getAvatarPath(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAvatarPath_whenProfileDoesNotExist_throwsResourceNotFoundException() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAvatarPath(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
