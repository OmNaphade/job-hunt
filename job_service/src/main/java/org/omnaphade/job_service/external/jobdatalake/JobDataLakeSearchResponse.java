package org.omnaphade.job_service.external.jobdatalake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://api.jobdatalake.com/v1/jobs}, verified against a live authenticated response. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDataLakeSearchResponse {

    private List<JobDataLakeJob> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobDataLakeJob {
        private String id;
        private String title;

        @JsonProperty("company_name")
        private String companyName;

        private String url;
        private List<String> locations;

        @JsonProperty("remote_type")
        private String remoteType;

        @JsonProperty("employment_type")
        private String employmentType;

        @JsonProperty("salary_min_usd")
        private Double salaryMinUsd;

        @JsonProperty("salary_max_usd")
        private Double salaryMaxUsd;
    }

}
