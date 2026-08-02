package org.omnaphade.application_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omnaphade.application_service.dtos.ApplicationCreateDTO;
import org.omnaphade.application_service.dtos.ApplicationResponseDTO;
import org.omnaphade.application_service.entities.Application;
import org.omnaphade.application_service.entities.ApplicationStatus;
import org.omnaphade.application_service.exception.BadRequestException;
import org.omnaphade.application_service.exception.ResourceNotFoundException;
import org.omnaphade.application_service.kafka.ApplicationEventProducer;
import org.omnaphade.application_service.repository.ApplicationRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationEventProducer eventProducer;

    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(applicationRepository, eventProducer);
    }

    private Application application(Long id, Long jobId, Long userId, ApplicationStatus status) {
        return Application.builder()
                .id(id).jobId(jobId).userId(userId).status(status)
                .build();
    }

    @Test
    void applyForJob_whenAlreadyApplied_throwsBadRequestException_andNeverSaves() {
        ApplicationCreateDTO dto = new ApplicationCreateDTO();
        dto.setJobId(1L);
        dto.setUserId(4L);
        when(applicationRepository.existsByJobIdAndUserId(1L, 4L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyForJob(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyForJob_setsStatusApplied_andPersists() {
        ApplicationCreateDTO dto = new ApplicationCreateDTO();
        dto.setJobId(1L);
        dto.setUserId(4L);
        when(applicationRepository.existsByJobIdAndUserId(1L, 4L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        ApplicationResponseDTO result = applicationService.applyForJob(dto);

        assertThat(result.getStatus()).isEqualTo("APPLIED");
        assertThat(result.getId()).isEqualTo(10L);
        verify(eventProducer, never()).publishStatusChanged(any(), any(), any(), any());
    }

    @Test
    void getApplicationById_notFound_throwsResourceNotFoundException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_validTransition_savesAndPublishesKafkaEvent() {
        Application existing = application(1L, 5L, 4L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponseDTO result = applicationService.updateStatus(1L, ApplicationStatus.SHORTLISTED);

        assertThat(result.getStatus()).isEqualTo("SHORTLISTED");
        verify(eventProducer).publishStatusChanged(4L, 5L, "SHORTLISTED", 1L);
    }

    @Test
    void updateStatus_invalidTransition_throwsBadRequestException_andNeverPublishes() {
        Application existing = application(1L, 5L, 4L, ApplicationStatus.HIRED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.updateStatus(1L, ApplicationStatus.SHORTLISTED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition");

        verify(applicationRepository, never()).save(any());
        verify(eventProducer, never()).publishStatusChanged(any(), any(), any(), any());
    }

    @Test
    void updateStatus_appliedDirectlyToHired_isRejected() {
        Application existing = application(1L, 5L, 4L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.updateStatus(1L, ApplicationStatus.HIRED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void withdrawApplication_owner_setsWithdrawnStatus() {
        Application existing = application(1L, 5L, 4L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));

        applicationService.withdrawApplication(1L, 4L);

        assertThat(existing.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        verify(applicationRepository).save(existing);
    }

    @Test
    void withdrawApplication_notOwner_throwsBadRequestException() {
        Application existing = application(1L, 5L, 4L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.withdrawApplication(1L, 999L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own applications");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void withdrawApplication_alreadyHired_throwsBadRequestException() {
        Application existing = application(1L, 5L, 4L, ApplicationStatus.HIRED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.withdrawApplication(1L, 4L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hired");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void getApplicationsByUser_mapsAllToDto() {
        when(applicationRepository.findByUserId(4L)).thenReturn(List.of(
                application(1L, 5L, 4L, ApplicationStatus.APPLIED),
                application(2L, 6L, 4L, ApplicationStatus.SHORTLISTED)
        ));

        List<ApplicationResponseDTO> result = applicationService.getApplicationsByUser(4L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApplicationResponseDTO::getStatus)
                .containsExactly("APPLIED", "SHORTLISTED");
    }
}
