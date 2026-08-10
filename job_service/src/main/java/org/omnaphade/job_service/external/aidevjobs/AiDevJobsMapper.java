package org.omnaphade.job_service.external.aidevjobs;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/** Translates AI Dev Jobs' JSON response shape into the provider-agnostic {@link ExternalJobDTO}. */
public class AiDevJobsMapper {

    private AiDevJobsMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(AiDevJobsSearchResponse response) {
        if (response == null || response.getJobs() == null) {
            return Collections.emptyList();
        }
        return response.getJobs().stream()
                .map(AiDevJobsMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(AiDevJobsSearchResponse.AiDevJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .companyName(job.getCompanyName())
                .location(toLocation(job))
                .salaryMin(job.getSalaryMin() != null ? job.getSalaryMin().doubleValue() : null)
                .salaryMax(job.getSalaryMax() != null ? job.getSalaryMax().doubleValue() : null)
                .jobType(toJobType(job.getJobType()))
                .externalUrl(toUrl(job))
                .build();
    }

    private static String toLocation(AiDevJobsSearchResponse.AiDevJob job) {
        return LocationResolver.resolve(job.getLocation(), "remote".equalsIgnoreCase(job.getWorkplace()));
    }

    private static String toUrl(AiDevJobsSearchResponse.AiDevJob job) {
        if (job.getApplyUrl() != null && !job.getApplyUrl().isBlank()) {
            return job.getApplyUrl();
        }
        return job.getId() != null ? "https://aidevboard.com/job/" + job.getId() : null;
    }

    static String toJobType(String jobType) {
        return JobTypeNormalizer.normalize(jobType);
    }

}
