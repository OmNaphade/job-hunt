package org.omnaphade.user_service.mapper;

import org.omnaphade.user_service.dtos.ProfileCreateDTO;
import org.omnaphade.user_service.dtos.ProfileResponseDTO;
import org.omnaphade.user_service.entities.Profile;

public class ProfileMapper {

    public static Profile toEntity(ProfileCreateDTO dto) {

        Profile profile = new Profile();

        profile.setUserId(dto.getUserId());
        profile.setHeadline(dto.getHeadline());
        profile.setSummary(dto.getSummary());
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setCurrentLocation(dto.getCurrentLocation());

        return profile;
    }

    public static ProfileResponseDTO toResponseDTO(Profile profile) {

        ProfileResponseDTO dto = new ProfileResponseDTO();

        dto.setId(profile.getId());
        dto.setUserId(profile.getUserId());
        dto.setHeadline(profile.getHeadline());
        dto.setSummary(profile.getSummary());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setCurrentLocation(profile.getCurrentLocation());

        return dto;
    }
}