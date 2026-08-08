package org.omnaphade.application_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.application_service.dtos.ApplicationCreateDTO;
import org.omnaphade.application_service.dtos.ApplicationResponseDTO;
import org.omnaphade.application_service.entities.Application;
import org.omnaphade.application_service.entities.ApplicationStatus;
import org.omnaphade.application_service.exception.BadRequestException;
import org.omnaphade.application_service.exception.ResourceNotFoundException;
import org.omnaphade.application_service.kafka.ApplicationEventProducer;
import org.omnaphade.application_service.mapper.ApplicationMapper;
import org.omnaphade.application_service.repository.ApplicationRepository;
import org.omnaphade.application_service.storage.FileStorageService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements IApplicationService {

    private static final Set<String> ALLOWED_RESUME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventProducer eventProducer;
    private final FileStorageService fileStorageService;

    @Override
    public ApplicationResponseDTO applyForJob(ApplicationCreateDTO dto) {
        if (applicationRepository.existsByJobIdAndUserId(dto.getJobId(), dto.getUserId())) {
            throw new BadRequestException("You have already applied for this job");
        }
        Application application = ApplicationMapper.toEntity(dto);
        application.setStatus(ApplicationStatus.APPLIED);
        return ApplicationMapper.toDTO(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponseDTO getApplicationById(Long id) {
        return ApplicationMapper.toDTO(findById(id));
    }

    @Override
    public List<ApplicationResponseDTO> getApplicationsByUser(Long userId) {
        return applicationRepository.findByUserId(userId).stream()
                .map(ApplicationMapper::toDTO).toList();
    }

    @Override
    public List<ApplicationResponseDTO> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .map(ApplicationMapper::toDTO).toList();
    }

    @Override
    public ApplicationResponseDTO updateStatus(Long id, ApplicationStatus status) {
        Application application = findById(id);
        validateStatusTransition(application.getStatus(), status);
        application.setStatus(status);
        ApplicationResponseDTO result = ApplicationMapper.toDTO(applicationRepository.save(application));
        eventProducer.publishStatusChanged(
            application.getUserId(), application.getJobId(),
            status.name(), application.getId()
        );
        return result;
    }

    @Override
    public void withdrawApplication(Long id, Long userId) {
        Application application = findById(id);
        if (!application.getUserId().equals(userId)) {
            throw new BadRequestException("You can only withdraw your own applications");
        }
        if (application.getStatus() == ApplicationStatus.HIRED) {
            throw new BadRequestException("Cannot withdraw a hired application");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);
    }

    @Override
    public ApplicationResponseDTO uploadResume(Long id, Long requestingUserId, MultipartFile file) {
        Application application = findById(id);
        if (!application.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("You can only upload a resume for your own application");
        }
        validateResumeFile(file);
        String storedPath = fileStorageService.store(file, "resumes");
        application.setResumeUrl(storedPath);
        return ApplicationMapper.toDTO(applicationRepository.save(application));
    }

    @Override
    public String getResumePath(Long id, Long requestingUserId, boolean privileged) {
        Application application = findById(id);
        if (!privileged && !application.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("You are not authorized to view this resume");
        }
        if (application.getResumeUrl() == null) {
            throw new ResourceNotFoundException("No resume uploaded for this application");
        }
        return application.getResumeUrl();
    }

    private void validateResumeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Resume file is required");
        }
        if (!ALLOWED_RESUME_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only PDF and Word documents are allowed for resumes");
        }
    }

    private void validateStatusTransition(ApplicationStatus current, ApplicationStatus next) {
        if (!current.canTransitionTo(next)) {
            throw new BadRequestException(
                "Cannot transition from " + current + " to " + next +
                ". Allowed: " + current.getAllowedTransitions()
            );
        }
    }

    private Application findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }
}
