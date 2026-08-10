package org.omnaphade.job_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.entities.JobSource;
import org.omnaphade.job_service.entities.JobStatus;
import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.ExternalJobProvider;
import org.omnaphade.job_service.repository.JobRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalJobImportServiceImplTest {

    @Mock
    private ExternalJobProvider provider;

    @Mock
    private JobRepository jobRepository;

    private ExternalJobImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        importService = new ExternalJobImportServiceImpl(List.of(provider), jobRepository);
        lenient().when(provider.getSource()).thenReturn(JobSource.ADZUNA);
        lenient().when(provider.getMaxPagesPerRun()).thenReturn(1);
    }

    private ExternalJobDTO sampleDto(String externalId) {
        return ExternalJobDTO.builder()
                .externalId(externalId)
                .title("Backend Engineer")
                .description("Build things")
                .companyName("Acme Corp")
                .location("Remote")
                .salaryMin(50000.0)
                .salaryMax(80000.0)
                .jobType("FULL_TIME")
                .externalUrl("https://example.com/job/" + externalId)
                .build();
    }

    @Test
    void createsNewJobWhenExternalIdNotSeenBefore() {
        when(provider.fetchJobs(1)).thenReturn(List.of(sampleDto("adzuna-1")));
        when(jobRepository.findBySourceAndExternalId(JobSource.ADZUNA, "adzuna-1")).thenReturn(Optional.empty());
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportSummary summary = importService.importAll();

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("adzuna-1");
        assertThat(saved.getSource()).isEqualTo(JobSource.ADZUNA);
        assertThat(saved.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(saved.getTitle()).isEqualTo("Backend Engineer");

        assertThat(summary.fetched()).isEqualTo(1);
        assertThat(summary.created()).isEqualTo(1);
        assertThat(summary.updated()).isEqualTo(0);
        assertThat(summary.skipped()).isEqualTo(0);
    }

    @Test
    void updatesExistingJobInPlace() {
        Job existing = Job.builder()
                .id(10L)
                .title("Old Title")
                .status(JobStatus.OPEN)
                .source(JobSource.ADZUNA)
                .externalId("adzuna-2")
                .build();
        when(provider.fetchJobs(1)).thenReturn(List.of(sampleDto("adzuna-2")));
        when(jobRepository.findBySourceAndExternalId(JobSource.ADZUNA, "adzuna-2")).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportSummary summary = importService.importAll();

        assertThat(existing.getTitle()).isEqualTo("Backend Engineer");
        assertThat(summary.created()).isEqualTo(0);
        assertThat(summary.updated()).isEqualTo(1);
    }

    @Test
    void doesNotReopenAJobAnAdminManuallyClosed() {
        Job existing = Job.builder()
                .id(11L)
                .title("Old Title")
                .status(JobStatus.CLOSED)
                .source(JobSource.ADZUNA)
                .externalId("adzuna-3")
                .build();
        when(provider.fetchJobs(1)).thenReturn(List.of(sampleDto("adzuna-3")));
        when(jobRepository.findBySourceAndExternalId(JobSource.ADZUNA, "adzuna-3")).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        importService.importAll();

        assertThat(existing.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void skipsMalformedEntriesMissingIdOrTitle() {
        ExternalJobDTO missingId = sampleDto(null);
        ExternalJobDTO missingTitle = sampleDto("adzuna-4");
        missingTitle.setTitle(null);
        when(provider.fetchJobs(1)).thenReturn(List.of(missingId, missingTitle));

        ImportSummary summary = importService.importAll();

        verify(jobRepository, never()).save(any());
        assertThat(summary.fetched()).isEqualTo(2);
        assertThat(summary.skipped()).isEqualTo(2);
    }

}
