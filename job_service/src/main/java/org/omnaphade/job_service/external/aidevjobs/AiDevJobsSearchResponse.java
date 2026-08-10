package org.omnaphade.job_service.external.aidevjobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://aidevboard.com/api/v1/jobs}, per its published OpenAPI spec. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDevJobsSearchResponse {

    private List<AiDevJob> jobs;

    @JsonProperty("has_next")
    private Boolean hasNext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiDevJob {
        private String id;
        private String title;

        @JsonProperty("company_name")
        private String companyName;

        private String description;
        private String location;
        private String workplace;

        @JsonProperty("job_type")
        private String jobType;

        @JsonProperty("salary_min")
        private Integer salaryMin;

        @JsonProperty("salary_max")
        private Integer salaryMax;

        private List<String> tags;

        @JsonProperty("apply_url")
        private String applyUrl;

        private String slug;
    }

}
