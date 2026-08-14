package org.omnaphade.job_service.service;

import org.omnaphade.job_service.dtos.JobCreateDTO;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.omnaphade.job_service.entities.JobStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IJobService {
    JobResponseDTO createJob(JobCreateDTO dto, Long createdBy);
    JobResponseDTO getJobById(Long id);
    Page<JobResponseDTO> getAllJobs(int page, int size, String sortBy, String sortDir);
    Page<JobResponseDTO> getJobsByCompany(Long companyId, int page, int size, String sortBy, String sortDir);
    JobResponseDTO updateJob(Long id, JobCreateDTO dto, Long requestingUserId);
    JobResponseDTO updateJobStatus(Long id, JobStatus status, Long requestingUserId);
    void deleteJob(Long id, Long requestingUserId);
    Page<JobResponseDTO> searchJobs(String keyword, String location, String jobType, Double minSalary, Integer maxExperience,
                                    int page, int size, String sortBy, String sortDir);
    void saveJob(Long userId, Long jobId);
    void unsaveJob(Long userId, Long jobId);
    Page<JobResponseDTO> getSavedJobs(Long userId, int page, int size);
    List<Long> getSavedJobIds(Long userId);
}
