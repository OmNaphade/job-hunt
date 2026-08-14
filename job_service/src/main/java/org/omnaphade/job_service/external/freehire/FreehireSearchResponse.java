package org.omnaphade.job_service.external.freehire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://freehire.me/api/v1/agent/jobs/search}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreehireSearchResponse {

    private List<FreehireJob> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FreehireJob {

        @JsonProperty("public_slug")
        private String publicSlug;

        @JsonProperty("external_id")
        private String externalId;

        private String source;
        private String url;
        private String title;
        private String company;

        @JsonProperty("company_slug")
        private String companySlug;

        private String location;

        @JsonProperty("work_mode")
        private String workMode;

        private String description;
    }

}
