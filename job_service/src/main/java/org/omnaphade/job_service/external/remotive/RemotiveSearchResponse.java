package org.omnaphade.job_service.external.remotive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://remotive.com/api/remote-jobs}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemotiveSearchResponse {

    private List<RemotiveJob> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemotiveJob {

        private Long id;
        private String url;
        private String title;

        @JsonProperty("company_name")
        private String companyName;

        private String category;

        @JsonProperty("job_type")
        private String jobType;

        @JsonProperty("candidate_required_location")
        private String candidateRequiredLocation;

        private String salary;
        private String description;
    }

}
