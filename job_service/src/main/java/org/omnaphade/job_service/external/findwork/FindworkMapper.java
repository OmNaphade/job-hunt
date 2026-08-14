package org.omnaphade.job_service.external.findwork;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;
import org.omnaphade.job_service.external.LocationResolver;

import java.util.Collections;
import java.util.List;

/** Translates Findwork's JSON response shape into the provider-agnostic {@link ExternalJobDTO}. */
public class FindworkMapper {

    private FindworkMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(FindworkSearchResponse response) {
        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }
        return response.getResults().stream()
                .map(FindworkMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(FindworkSearchResponse.FindworkJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getId())
                .title(job.getRole())
                .description(job.getText())
                .companyName(job.getCompanyName())
                .location(toLocation(job))
                .jobType(toJobType(job.getEmploymentType()))
                .externalUrl(job.getUrl())
                .build();
    }

    private static String toLocation(FindworkSearchResponse.FindworkJob job) {
        return LocationResolver.resolve(job.getLocation(), Boolean.TRUE.equals(job.getRemote()));
    }

    static String toJobType(String employmentType) {
        return JobTypeNormalizer.normalize(employmentType);
    }

}
