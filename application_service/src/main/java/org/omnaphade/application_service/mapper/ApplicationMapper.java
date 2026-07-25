package org.omnaphade.application_service.mapper;

import org.omnaphade.application_service.dtos.ApplicationCreateDTO;
import org.omnaphade.application_service.dtos.ApplicationResponseDTO;
import org.omnaphade.application_service.entities.Application;

public class ApplicationMapper {

    public static ApplicationResponseDTO toDTO(Application application) {

        ApplicationResponseDTO dto = new ApplicationResponseDTO();

        dto.setId(application.getId());
        dto.setJobId(application.getJobId());
        dto.setUserId(application.getUserId());
        dto.setStatus(application.getStatus().name());
        dto.setAppliedAt(application.getAppliedAt());

        return dto;
    }

    public static Application toEntity(ApplicationCreateDTO dto) {

        Application application = new Application();

        application.setJobId(dto.getJobId());
        application.setUserId(dto.getUserId());

        return application;
    }

}