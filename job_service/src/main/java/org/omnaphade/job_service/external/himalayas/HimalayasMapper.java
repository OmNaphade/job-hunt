package org.omnaphade.job_service.external.himalayas;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;

import java.util.Collections;
import java.util.List;

/** Translates Himalayas' JSON response shape into the provider-agnostic {@link ExternalJobDTO}. */
public class HimalayasMapper {

    private HimalayasMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(HimalayasSearchResponse response) {
        if (response == null || response.getJobs() == null) {
            return Collections.emptyList();
        }
        return response.getJobs().stream()
                .map(HimalayasMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(HimalayasSearchResponse.HimalayasJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getGuid())
                .title(job.getTitle())
                .description(job.getDescription() != null ? job.getDescription() : job.getExcerpt())
                .companyName(job.getCompanyName())
                .location(toLocation(job.getLocationRestrictions()))
                .salaryMin(job.getMinSalary())
                .salaryMax(job.getMaxSalary())
                .jobType(toJobType(job.getEmploymentType()))
                .externalUrl(job.getApplicationLink())
                .build();
    }

    private static String toLocation(List<String> locationRestrictions) {
        if (locationRestrictions == null || locationRestrictions.isEmpty()) {
            return "Remote (worldwide)";
        }
        return String.join(", ", locationRestrictions);
    }

    static String toJobType(String employmentType) {
        return JobTypeNormalizer.normalize(employmentType);
    }

}
