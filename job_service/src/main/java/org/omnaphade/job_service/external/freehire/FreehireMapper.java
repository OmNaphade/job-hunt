package org.omnaphade.job_service.external.freehire;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/**
 * Translates freehire's JSON response shape into the provider-agnostic {@link ExternalJobDTO}.
 * {@code public_slug} (not {@code external_id}, which is only present for ATS-sourced jobs) is used as
 * the externalId since it's the one field the API guarantees on every job, manually-added or not.
 */
public class FreehireMapper {

    private FreehireMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(FreehireSearchResponse response) {
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData().stream()
                .map(FreehireMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(FreehireSearchResponse.FreehireJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getPublicSlug())
                .title(job.getTitle())
                .description(job.getDescription())
                .companyName(job.getCompany())
                .location(toLocation(job.getLocation(), job.getWorkMode()))
                .jobType("FULL_TIME")
                .externalUrl(job.getUrl())
                .build();
    }

    private static String toLocation(String location, String workMode) {
        return LocationResolver.resolve(location, "remote".equalsIgnoreCase(workMode));
    }

}
