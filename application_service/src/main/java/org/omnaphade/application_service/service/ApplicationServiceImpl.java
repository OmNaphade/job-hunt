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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements IApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventProducer eventProducer;

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
