package org.omnaphade.job_service.external.aijobsco;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/**
 * Translates artificialintelligencejobs.co's JSON response shape into the provider-agnostic
 * {@link ExternalJobDTO}. The API exposes no numeric/UUID job id, so {@link #toExternalId} derives a
 * stable one from the trailing slug of the job's own listing URL (e.g. {@code .../jobs/some-role-abcd1234}).
 */
public class AiJobsCoMapper {

    private AiJobsCoMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(AiJobsCoSearchResponse response) {
        if (response == null || response.getJobs() == null) {
            return Collections.emptyList();
        }
        return response.getJobs().stream()
                .map(AiJobsCoMapper::toExternalJobDto)
                .filter(dto -> dto.getExternalId() != null)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(AiJobsCoSearchResponse.AiJobsCoJob job) {
        return ExternalJobDTO.builder()
                .externalId(toExternalId(job.getUrl()))
                .title(job.getTitle())
                .companyName(job.getCompany())
                .location(toLocation(job))
                .jobType("FULL_TIME")
                .externalUrl(job.getApplyUrl() != null && !job.getApplyUrl().isBlank() ? job.getApplyUrl() : job.getUrl())
                .build();
    }

    private static String toLocation(AiJobsCoSearchResponse.AiJobsCoJob job) {
        return LocationResolver.resolve(job.getLocation(), Boolean.TRUE.equals(job.getRemote()));
    }

    static String toExternalId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        String trimmed = url.substring(0, end);
        int lastSlash = trimmed.lastIndexOf('/');
        String slug = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        return slug.isBlank() ? null : slug;
    }

}
