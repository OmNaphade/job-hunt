package org.omnaphade.job_service.external.adzuna;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET /v1/api/jobs/{country}/search/{page}} from the Adzuna Job Search API. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdzunaSearchResponse {

    private List<AdzunaJobResult> results;
    private Integer count;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaJobResult {
        private String id;
        private String title;
        private String description;
        private AdzunaCompany company;
        private AdzunaLocation location;

        @JsonProperty("salary_min")
        private Double salaryMin;

        @JsonProperty("salary_max")
        private Double salaryMax;

        @JsonProperty("contract_type")
        private String contractType;

        @JsonProperty("contract_time")
        private String contractTime;

        @JsonProperty("redirect_url")
        private String redirectUrl;

        private String created;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaCompany {
        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaLocation {
        @JsonProperty("display_name")
        private String displayName;
    }

}
