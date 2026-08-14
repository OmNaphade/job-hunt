package org.omnaphade.job_service.external;

import org.omnaphade.job_service.entities.Job;
import org.omnaphade.job_service.entities.JobSource;
import org.omnaphade.job_service.entities.JobStatus;

/**
 * Maps a provider-agnostic {@link ExternalJobDTO} onto the {@link Job} entity. Kept separate from
 * {@code ExternalJobImportServiceImpl} so the orchestration logic (fetch, look up, decide create-vs-update,
 * save) doesn't also carry field-mapping/truncation concerns.
 */
public class ExternalJobMapper {

    private static final int DESCRIPTION_MAX_LENGTH = 3000;

    private ExternalJobMapper() {
    }

    public static Job toNewEntity(ExternalJobDTO dto, JobSource source) {
        return Job.builder()
                .title(dto.getTitle())
                .description(truncate(HtmlTextExtractor.toPlainText(dto.getDescription())))
                .location(dto.getLocation())
                .companyName(dto.getCompanyName())
                .salaryMin(dto.getSalaryMin() != null ? dto.getSalaryMin() : 0.0)
                .salaryMax(dto.getSalaryMax() != null ? dto.getSalaryMax() : 0.0)
                .jobType(dto.getJobType())
                .experienceRequired(0)
                .status(JobStatus.OPEN)
                .source(source)
                .externalId(dto.getExternalId())
                .externalUrl(dto.getExternalUrl())
                .build();
    }

    /**
     * Updates an existing imported job in place. Deliberately does NOT touch {@link Job#getStatus()} —
     * if an admin manually closed this job via {@code PATCH /api/jobs/{id}/status}, a later re-import of
     * the same externalId must not silently reopen it.
     */
    public static void updateExisting(Job job, ExternalJobDTO dto) {
        job.setTitle(dto.getTitle());
        job.setDescription(truncate(HtmlTextExtractor.toPlainText(dto.getDescription())));
        job.setLocation(dto.getLocation());
        job.setCompanyName(dto.getCompanyName());
        if (dto.getSalaryMin() != null) job.setSalaryMin(dto.getSalaryMin());
        if (dto.getSalaryMax() != null) job.setSalaryMax(dto.getSalaryMax());
        job.setJobType(dto.getJobType());
        job.setExternalUrl(dto.getExternalUrl());
    }

    private static String truncate(String description) {
        if (description == null) return null;
        if (description.length() <= DESCRIPTION_MAX_LENGTH) return description;
        return description.substring(0, DESCRIPTION_MAX_LENGTH - 3) + "...";
    }

}
