package org.omnaphade.job_service.mapper;

import org.omnaphade.job_service.dtos.JobCreateDTO;
import org.omnaphade.job_service.dtos.JobResponseDTO;
import org.omnaphade.job_service.entities.Job;

public class JobMapper {

    private JobMapper() {
    }

    public static JobResponseDTO toDTO(Job job) {

        JobResponseDTO dto = new JobResponseDTO();

        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setCompanyId(job.getCompanyId());
        dto.setLocation(job.getLocation());
        dto.setMinSalary(job.getSalaryMin());
        dto.setMaxSalary(job.getSalaryMax());
        dto.setJobType(job.getJobType());
        dto.setExperienceRequired(job.getExperienceRequired());
        dto.setStatus(job.getStatus().name());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setSource(job.getSource() != null ? job.getSource().name() : null);
        dto.setExternalUrl(job.getExternalUrl());
        dto.setCompanyName(job.getCompanyName());

        return dto;
    }

    public static Job toEntity(JobCreateDTO dto) {

        Job job = new Job();

        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setCompanyId(dto.getCompanyId());
        job.setLocation(dto.getLocation());
        job.setSalaryMin(dto.getMinSalary());
        job.setSalaryMax(dto.getMaxSalary());
        job.setJobType(dto.getJobType());
        job.setExperienceRequired(dto.getExperienceRequired());

        return job;
    }

}