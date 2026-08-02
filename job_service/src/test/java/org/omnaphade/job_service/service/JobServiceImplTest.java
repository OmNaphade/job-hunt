package org.omnaphade.job_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omnaphade.job_service.dtos.JobCreateDTO;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.entities.JobSkill;
import org.omnaphade.job_service.entities.JobStatus;
import org.omnaphade.job_service.exception.ResourceNotFoundException;
import org.omnaphade.job_service.repository.JobRepository;
import org.omnaphade.job_service.repository.JobSkillRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private JobServiceImpl jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(jobRepository, jobSkillRepository);
        lenient().when(jobSkillRepository.findByJobId(anyLong())).thenReturn(List.of());
    }

    private JobCreateDTO sampleCreateDTO() {
        JobCreateDTO dto = new JobCreateDTO();
        dto.setTitle("Senior Backend Engineer");
        dto.setDescription("5+ years Java, Spring Boot");
        dto.setCompanyId(1L);
        dto.setLocation("Bangalore, India");
        dto.setMinSalary(120000);
        dto.setMaxSalary(180000);
        dto.setJobType("FULL_TIME");
        dto.setExperienceRequired(5);
        dto.setSkills(List.of("Java", "Spring Boot", "Kafka"));
        return dto;
    }

    private Job savedJob() {
        return Job.builder()
                .id(5L)
                .title("Senior Backend Engineer")
                .description("5+ years Java, Spring Boot")
                .companyId(1L)
                .location("Bangalore, India")
                .salaryMin(120000)
                .salaryMax(180000)
                .jobType("FULL_TIME")
                .experienceRequired(5)
                .status(JobStatus.OPEN)
                .createdBy(9L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createJob_setsStatusOpenAndCreatedBy_persistsSkills() {
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob());

        JobResponseDTO result = jobService.createJob(sampleCreateDTO(), 9L);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job persisted = jobCaptor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(persisted.getCreatedBy()).isEqualTo(9L);

        verify(jobSkillRepository, times(3)).save(any(JobSkill.class));
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void createJob_withNoSkills_doesNotTouchSkillRepository() {
        JobCreateDTO dto = sampleCreateDTO();
        dto.setSkills(null);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob());

        jobService.createJob(dto, 9L);

        verify(jobSkillRepository, never()).save(any(JobSkill.class));
    }

    @Test
    void createJob_skipsBlankSkillNames() {
        JobCreateDTO dto = sampleCreateDTO();
        dto.setSkills(List.of("Java", "  ", "", "Kafka"));
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob());

        jobService.createJob(dto, 9L);

        verify(jobSkillRepository, times(2)).save(any(JobSkill.class));
    }

    @Test
    void getJobById_notFound_throwsResourceNotFoundException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getJobById_found_attachesSkills() {
        when(jobRepository.findById(5L)).thenReturn(Optional.of(savedJob()));
        when(jobSkillRepository.findByJobId(5L)).thenReturn(List.of(
                JobSkill.builder().id(1L).jobId(5L).skillName("Java").build(),
                JobSkill.builder().id(2L).jobId(5L).skillName("Kafka").build()
        ));

        JobResponseDTO result = jobService.getJobById(5L);

        assertThat(result.getSkills()).containsExactly("Java", "Kafka");
    }

    @Test
    void getAllJobs_appliesDefaultSortAndDirection() {
        when(jobRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(savedJob())));

        jobService.getAllJobs(0, 20, "createdAt", "desc");

        verify(jobRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllJobs_withUnknownSortField_fallsBackToCreatedAt() {
        when(jobRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        jobService.getAllJobs(0, 20, "salary; DROP TABLE jobs", "asc");

        verify(jobRepository).findAll(pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllJobs_withNegativePageAndSize_clampsToValidValues() {
        when(jobRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        jobService.getAllJobs(-5, -10, "createdAt", "desc");

        verify(jobRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(1);
    }

    @Test
    void searchJobs_passesAllFiltersAndOnlySearchesOpenJobs() {
        Page<Job> page = new PageImpl<>(List.of(savedJob()));
        when(jobRepository.searchJobs(eq(JobStatus.OPEN), eq("Java"), eq("Bangalore"), eq("FULL_TIME"),
                eq(100000.0), eq(5), any(Pageable.class))).thenReturn(page);

        Page<JobResponseDTO> result = jobService.searchJobs(
                "Java", "Bangalore", "FULL_TIME", 100000.0, 5, 0, 20, "createdAt", "desc");

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(jobRepository).searchJobs(eq(JobStatus.OPEN), eq("Java"), eq("Bangalore"), eq("FULL_TIME"),
                eq(100000.0), eq(5), any(Pageable.class));
    }

    @Test
    void updateJob_whenSkillsProvided_replacesExistingSkills() {
        Job existing = savedJob();
        when(jobRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenReturn(existing);

        JobCreateDTO dto = sampleCreateDTO();
        dto.setSkills(List.of("Python"));
        jobService.updateJob(5L, dto, 9L);

        verify(jobSkillRepository).deleteByJobId(5L);
        verify(jobSkillRepository, times(1)).save(any(JobSkill.class));
    }

    @Test
    void updateJob_whenSkillsNull_doesNotTouchExistingSkills() {
        Job existing = savedJob();
        when(jobRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenReturn(existing);

        JobCreateDTO dto = sampleCreateDTO();
        dto.setSkills(null);
        jobService.updateJob(5L, dto, 9L);

        verify(jobSkillRepository, never()).deleteByJobId(anyLong());
        verify(jobSkillRepository, never()).save(any(JobSkill.class));
    }

    @Test
    void updateJobStatus_persistsNewStatus() {
        Job existing = savedJob();
        when(jobRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponseDTO result = jobService.updateJobStatus(5L, JobStatus.CLOSED, 9L);

        assertThat(result.getStatus()).isEqualTo("CLOSED");
        assertThat(existing.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void deleteJob_removesSkillsBeforeDeletingJob() {
        Job existing = savedJob();
        when(jobRepository.findById(5L)).thenReturn(Optional.of(existing));

        jobService.deleteJob(5L, 9L);

        var inOrder = inOrder(jobSkillRepository, jobRepository);
        inOrder.verify(jobSkillRepository).deleteByJobId(5L);
        inOrder.verify(jobRepository).delete(existing);
    }

    @Test
    void deleteJob_notFound_throwsAndNeverDeletesSkills() {
        when(jobRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.deleteJob(42L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobSkillRepository, never()).deleteByJobId(anyLong());
    }
}
