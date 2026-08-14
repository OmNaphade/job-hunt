package org.omnaphade.job_service.external.jobicy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://jobicy.com/api/v2/remote-jobs}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobicySearchResponse {

    private List<JobicyJob> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobicyJob {

        private Long id;
        private String url;

        @JsonProperty("jobTitle")
        private String jobTitle;

        @JsonProperty("companyName")
        private String companyName;

        @JsonProperty("jobType")
        private List<String> jobType;

        @JsonProperty("jobGeo")
        private String jobGeo;

        @JsonProperty("jobExcerpt")
        private String jobExcerpt;

        @JsonProperty("jobDescription")
        private String jobDescription;

        @JsonProperty("salaryMin")
        private Double salaryMin;

        @JsonProperty("salaryMax")
        private Double salaryMax;
    }

}
