package org.omnaphade.job_service.external.himalayas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response shape for {@code GET https://himalayas.app/jobs/api/search}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HimalayasSearchResponse {

    private Integer offset;
    private Integer limit;
    private Integer totalCount;
    private List<HimalayasJob> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HimalayasJob {
        private String guid;
        private String title;
        private String excerpt;
        private String description;
        private String companyName;
        private String employmentType;
        private Double minSalary;
        private Double maxSalary;
        private List<String> locationRestrictions;
        private List<String> parentCategories;
        private String applicationLink;
    }

}
