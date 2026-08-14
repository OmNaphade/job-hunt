package org.omnaphade.job_service.external.arbeitnow;

import org.omnaphade.job_service.external.ExternalJobDTO;
import org.omnaphade.job_service.external.JobTypeNormalizer;
import org.omnaphade.job_service.external.LocationResolver;

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
        return LocationResolver.resolve(job.getLocation(), Boolean.TRUE.equals(job.getRemote()));
    }

    static String toJobType(List<String> jobTypes) {
        String joined = jobTypes == null ? null : String.join(" ", jobTypes);
        return JobTypeNormalizer.normalize(joined);
    }

}
