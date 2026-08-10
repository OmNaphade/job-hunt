package org.omnaphade.job_service.external.aijobsco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://artificialintelligencejobs.co/api/jobs}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiJobsCoSearchResponse {

    private List<AiJobsCoJob> jobs;
    private Integer returned;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiJobsCoJob {
        private String title;
        private String company;
        private String location;
        private Boolean remote;
        private String category;
        private String level;
        private String region;
        private String salary;
        private String posted;
        private String url;

        @JsonProperty("apply_url")
        private String applyUrl;
    }

}
