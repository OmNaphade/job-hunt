package org.omnaphade.job_service.external.remotive;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/**
 * Translates Remotive's JSON response shape into the provider-agnostic {@link ExternalJobDTO}. Remotive
 * is a remote-only board, so {@link #candidateRequiredLocation} always falls back to "Remote" rather than
 * "Not specified". Its {@code salary} field is free text (e.g. "$70k - $90k"), not numeric, so it's
 * dropped rather than mis-parsed into {@link ExternalJobDTO#getSalaryMin()}/{@code salaryMax}.
 */
public class RemotiveMapper {

    private RemotiveMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(RemotiveSearchResponse response) {
        if (response == null || response.getJobs() == null) {
            return Collections.emptyList();
        }
        return response.getJobs().stream()
                .map(RemotiveMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(RemotiveSearchResponse.RemotiveJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getId() == null ? null : String.valueOf(job.getId()))
                .title(job.getTitle())
                .description(job.getDescription())
                .companyName(job.getCompanyName())
                .location(LocationResolver.resolve(job.getCandidateRequiredLocation(), true))
                .jobType(JobTypeNormalizer.normalize(job.getJobType()))
                .externalUrl(job.getUrl())
                .build();
    }

}
