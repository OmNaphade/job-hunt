package org.omnaphade.job_service.external.adzuna;

import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.Collections;
import java.util.List;

/**
 * Translates Adzuna's JSON response shape into the provider-agnostic {@link ExternalJobDTO}. All
 * Adzuna-specific quirks (its two-field contract_type/contract_time split, missing salary fields, etc.)
 * are resolved here so nothing downstream needs to know this data came from Adzuna specifically.
 */
public class AdzunaMapper {

    private AdzunaMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(AdzunaSearchResponse response) {
        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }
        return response.getResults().stream()
                .map(AdzunaMapper::toExternalJobDto)
                .toList();
    }

    private static ExternalJobDTO toExternalJobDto(AdzunaSearchResponse.AdzunaJobResult result) {
        return ExternalJobDTO.builder()
                .externalId(result.getId())
                .title(result.getTitle())
                .description(result.getDescription())
                .companyName(result.getCompany() != null ? result.getCompany().getDisplayName() : null)
                .location(result.getLocation() != null ? result.getLocation().getDisplayName() : null)
                .salaryMin(result.getSalaryMin())
                .salaryMax(result.getSalaryMax())
                .jobType(toJobType(result.getContractType(), result.getContractTime()))
                .externalUrl(result.getRedirectUrl())
                .build();
    }

    /**
     * Adzuna reports contract length ("permanent"/"contract") and hours ("full_time"/"part_time") as two
     * independent, often-absent fields. This app's {@code Job.jobType} is a single free-text value
     * (FULL_TIME/PART_TIME/CONTRACT by convention), so the two are collapsed into one with part-time
     * hours taking priority, then contract-type employment, defaulting to full-time.
     */
    static String toJobType(String contractType, String contractTime) {
        if ("part_time".equalsIgnoreCase(contractTime)) {
            return "PART_TIME";
        }
        if ("contract".equalsIgnoreCase(contractType)) {
            return "CONTRACT";
        }
        return "FULL_TIME";
    }

}
