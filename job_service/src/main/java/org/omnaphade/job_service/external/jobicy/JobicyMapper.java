package org.omnaphade.job_service.external.jobicy;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/**
 * Translates Jobicy's JSON response shape into the provider-agnostic {@link ExternalJobDTO}. Jobicy is a
 * remote-only board, so {@link #jobGeo} always falls back to "Remote" rather than "Not specified".
 * {@code jobType} arrives as an array (e.g. {@code ["Full-time"]}); only the first entry is used since
 * this app models a single job type per listing.
 */
public class JobicyMapper {

    private JobicyMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(JobicySearchResponse response) {
        if (response == null || response.getJobs() == null) {
            return Collections.emptyList();
        }
        return response.getJobs().stream()
                .map(JobicyMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(JobicySearchResponse.JobicyJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getId() == null ? null : String.valueOf(job.getId()))
                .title(job.getJobTitle())
                .description(job.getJobDescription() != null ? job.getJobDescription() : job.getJobExcerpt())
                .companyName(job.getCompanyName())
                .location(LocationResolver.resolve(job.getJobGeo(), true))
                .jobType(JobTypeNormalizer.normalize(firstJobType(job.getJobType())))
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .externalUrl(job.getUrl())
                .build();
    }

    private static String firstJobType(List<String> jobTypes) {
        return jobTypes == null || jobTypes.isEmpty() ? null : jobTypes.get(0);
    }

}
