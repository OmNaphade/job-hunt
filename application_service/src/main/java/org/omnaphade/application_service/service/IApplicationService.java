package org.omnaphade.application_service.service;

import org.omnaphade.application_service.dtos.ApplicationCreateDTO;
import org.omnaphade.application_service.dtos.ApplicationResponseDTO;
import org.omnaphade.application_service.entities.ApplicationStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IApplicationService {
    ApplicationResponseDTO applyForJob(ApplicationCreateDTO dto);
    ApplicationResponseDTO getApplicationById(Long id);
    List<ApplicationResponseDTO> getApplicationsByUser(Long userId);
    List<ApplicationResponseDTO> getApplicationsByJob(Long jobId);
    ApplicationResponseDTO updateStatus(Long id, ApplicationStatus status);
    void withdrawApplication(Long id, Long userId);
    ApplicationResponseDTO uploadResume(Long id, Long requestingUserId, MultipartFile file);
    String getResumePath(Long id, Long requestingUserId, boolean privileged);
}
