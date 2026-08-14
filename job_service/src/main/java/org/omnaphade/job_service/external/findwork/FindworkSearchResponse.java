package org.omnaphade.job_service.external.findwork;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://findwork.dev/api/jobs/} (Django REST Framework pagination), verified against a live authenticated response. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FindworkSearchResponse {

    private Integer count;
    private String next;
    private List<FindworkJob> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FindworkJob {
        private String id;
        private String role;
        private String text;
        private String location;
        private Boolean remote;
        private String url;

        @JsonProperty("company_name")
        private String companyName;

        @JsonProperty("employment_type")
        private String employmentType;

        @JsonProperty("date_posted")
        private String datePosted;
    }

}
