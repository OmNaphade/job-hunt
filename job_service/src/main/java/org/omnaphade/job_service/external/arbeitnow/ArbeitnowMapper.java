package org.omnaphade.job_service.external.arbeitnow;

import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Translates Arbeitnow's JSON response shape into the provider-agnostic {@link ExternalJobDTO}.
 * Arbeitnow has no server-side keyword/category filter, so tech relevance is decided here by checking
 * the job's title and tags against a configured set of tech keywords (case-insensitive substring match).
 */
public class ArbeitnowMapper {

    private ArbeitnowMapper() {
    }

    public static List<ExternalJobDTO> toExternalJobDtos(ArbeitnowSearchResponse response, Set<String> techKeywords) {
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData().stream()
                .filter(job -> isTechRelevant(job, techKeywords))
                .map(ArbeitnowMapper::toExternalJobDto)
                .toList();
    }

    static boolean isTechRelevant(ArbeitnowSearchResponse.ArbeitnowJob job, Set<String> techKeywords) {
        if (techKeywords == null || techKeywords.isEmpty()) {
            return true;
        }
        String haystack = String.join(" ",
                job.getTitle() != null ? job.getTitle() : "",
                job.getTags() != null ? String.join(" ", job.getTags()) : ""
        ).toLowerCase();

        return techKeywords.stream().anyMatch(keyword -> haystack.contains(keyword.toLowerCase()));
    }

    private static ExternalJobDTO toExternalJobDto(ArbeitnowSearchResponse.ArbeitnowJob job) {
        return ExternalJobDTO.builder()
                .externalId(job.getSlug())
                .title(job.getTitle())
                .description(job.getDescription())
                .companyName(job.getCompanyName())
                .location(toLocation(job))
                .jobType(toJobType(job.getJobTypes()))
                .externalUrl(job.getUrl())
                .build();
    }

    private static String toLocation(ArbeitnowSearchResponse.ArbeitnowJob job) {
        if (job.getLocation() != null && !job.getLocation().isBlank()) {
            return job.getLocation();
        }
        return Boolean.TRUE.equals(job.getRemote()) ? "Remote" : "Not specified";
    }

    static String toJobType(List<String> jobTypes) {
        if (jobTypes == null || jobTypes.isEmpty()) {
            return "FULL_TIME";
        }
        String normalized = String.join(" ", jobTypes).toLowerCase();
        if (normalized.contains("part")) {
            return "PART_TIME";
        }
        if (normalized.contains("contract") || normalized.contains("freelance")) {
            return "CONTRACT";
        }
        return "FULL_TIME";
    }

}
