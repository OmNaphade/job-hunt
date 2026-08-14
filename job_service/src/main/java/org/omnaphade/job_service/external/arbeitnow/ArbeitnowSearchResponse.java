package org.omnaphade.job_service.external.arbeitnow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://www.arbeitnow.com/api/job-board-api}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArbeitnowSearchResponse {

    private List<ArbeitnowJob> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArbeitnowJob {
        private String slug;

        @JsonProperty("company_name")
        private String companyName;

        private String title;
        private String description;
        private Boolean remote;
        private String url;
        private List<String> tags;

        @JsonProperty("job_types")
        private List<String> jobTypes;

        private String location;
    }

}
