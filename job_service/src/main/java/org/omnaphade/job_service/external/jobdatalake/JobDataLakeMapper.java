package org.omnaphade.job_service.external.jobdatalake;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/**
 * Translates JobDataLake's JSON response shape into the provider-agnostic {@link ExternalJobDTO}.
 * JobDataLake is a bulk aggregator and its list endpoint carries no job description, so
 * {@link ExternalJobDTO#getDescription()} is always null for this source.
 */
public class JobDataLakeMapper {

    private JobDataLakeMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(JobDataLakeSearchResponse response) {
        if (response == null || response.getJobs() == null) {
            return Collections.emptyList();
        }
        return response.getJobs().stream()
                .map(JobDataLakeMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(JobDataLakeSearchResponse.JobDataLakeJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getId())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .location(toLocation(job))
                .salaryMin(job.getSalaryMinUsd())
                .salaryMax(job.getSalaryMaxUsd())
                .jobType(JobTypeNormalizer.normalize(job.getEmploymentType()))
                .externalUrl(job.getUrl())
                .build();
    }

    private static String toLocation(JobDataLakeSearchResponse.JobDataLakeJob job) {
        String joined = job.getLocations() == null ? null : String.join(", ", job.getLocations());
        return LocationResolver.resolve(joined, "fully_remote".equalsIgnoreCase(job.getRemoteType()));
    }

}
