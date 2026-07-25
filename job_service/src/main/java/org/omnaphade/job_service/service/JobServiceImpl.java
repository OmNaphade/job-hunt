package org.omnaphade.job_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.job_service.dtos.JobCreateDTO;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.entities.JobSkill;
import org.omnaphade.job_service.entities.JobStatus;
import org.omnaphade.job_service.exception.ResourceNotFoundException;
import org.omnaphade.job_service.mapper.JobMapper;
import org.omnaphade.job_service.repository.JobRepository;
import org.omnaphade.job_service.repository.JobSkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements IJobService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "title", "salaryMin", "salaryMax", "experienceRequired"
    );

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;

    @Override
    public JobResponseDTO createJob(JobCreateDTO dto, Long createdBy) {
        Job job = JobMapper.toEntity(dto);
        job.setCreatedBy(createdBy);
        job.setStatus(JobStatus.OPEN);
        Job saved = jobRepository.save(job);
        saveSkills(saved.getId(), dto.getSkills());
        return withSkills(JobMapper.toDTO(saved));
    }

    @Override
    public JobResponseDTO getJobById(Long id) {
        return withSkills(JobMapper.toDTO(findById(id)));
    }

    @Override
    public Page<JobResponseDTO> getAllJobs(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return jobRepository.findAll(pageable)
                .map(job -> withSkills(JobMapper.toDTO(job)));
    }

    @Override
    public Page<JobResponseDTO> getJobsByCompany(Long companyId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return jobRepository.findByCompanyId(companyId, pageable)
                .map(job -> withSkills(JobMapper.toDTO(job)));
    }

    @Override
    public JobResponseDTO updateJob(Long id, JobCreateDTO dto, Long requestingUserId) {
        Job job = findById(id);
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setLocation(dto.getLocation());
        job.setSalaryMin(dto.getMinSalary());
        job.setSalaryMax(dto.getMaxSalary());
        job.setJobType(dto.getJobType());
        job.setExperienceRequired(dto.getExperienceRequired());
        Job saved = jobRepository.save(job);
        if (dto.getSkills() != null) {
            jobSkillRepository.deleteByJobId(saved.getId());
            saveSkills(saved.getId(), dto.getSkills());
        }
        return withSkills(JobMapper.toDTO(saved));
    }

    @Override
    public JobResponseDTO updateJobStatus(Long id, JobStatus status, Long requestingUserId) {
        Job job = findById(id);
        job.setStatus(status);
        return withSkills(JobMapper.toDTO(jobRepository.save(job)));
    }

    @Override
    public void deleteJob(Long id, Long requestingUserId) {
        Job job = findById(id);
        jobSkillRepository.deleteByJobId(id);
        jobRepository.delete(job);
    }

    @Override
    public Page<JobResponseDTO> searchJobs(String keyword, String location, String jobType, Double minSalary, Integer maxExperience,
                                           int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return jobRepository.searchJobs(JobStatus.OPEN, keyword, location, jobType, minSalary, maxExperience, pageable)
                .map(job -> withSkills(JobMapper.toDTO(job)));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        String normalizedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, normalizedSortBy));
    }

    private Job findById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    private void saveSkills(Long jobId, List<String> skills) {
        if (skills == null || skills.isEmpty()) return;
        for (String skillName : skills) {
            if (skillName != null && !skillName.isBlank()) {
                jobSkillRepository.save(JobSkill.builder()
                        .jobId(jobId)
                        .skillName(skillName.trim())
                        .build());
            }
        }
    }

    private JobResponseDTO withSkills(JobResponseDTO dto) {
        List<String> skills = jobSkillRepository.findByJobId(dto.getId()).stream()
                .map(JobSkill::getSkillName)
                .toList();
        dto.setSkills(skills);
        return dto;
    }
}