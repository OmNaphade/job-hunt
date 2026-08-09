package org.omnaphade.job_service.external.himalayas;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HimalayasMapperTest {

    private HimalayasSearchResponse.HimalayasJob baseJob() {
        HimalayasSearchResponse.HimalayasJob job = new HimalayasSearchResponse.HimalayasJob();
        job.setGuid("https://himalayas.app/companies/acme/jobs/backend-engineer");
        job.setTitle("Backend Engineer");
        job.setDescription("Full description here.");
        job.setExcerpt("Short excerpt.");
        job.setCompanyName("Acme Corp");
        job.setApplicationLink("https://himalayas.app/companies/acme/jobs/backend-engineer");
        return job;
    }

    @Test
    void mapsCoreFields() {
        HimalayasSearchResponse.HimalayasJob job = baseJob();
        job.setLocationRestrictions(List.of("United States", "Canada"));
        job.setMinSalary(90000.0);
        job.setMaxSalary(130000.0);
        job.setEmploymentType("Full Time");
        HimalayasSearchResponse response = new HimalayasSearchResponse(0, 20, 1, List.of(job));

        ExternalJobDTO dto = HimalayasMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("https://himalayas.app/companies/acme/jobs/backend-engineer");
        assertThat(dto.getTitle()).isEqualTo("Backend Engineer");
        assertThat(dto.getDescription()).isEqualTo("Full description here.");
        assertThat(dto.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(dto.getLocation()).isEqualTo("United States, Canada");
        assertThat(dto.getSalaryMin()).isEqualTo(90000.0);
        assertThat(dto.getSalaryMax()).isEqualTo(130000.0);
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://himalayas.app/companies/acme/jobs/backend-engineer");
    }

    @Test
    void fallsBackToExcerptWhenDescriptionMissing() {
        HimalayasSearchResponse.HimalayasJob job = baseJob();
        job.setDescription(null);
        HimalayasSearchResponse response = new HimalayasSearchResponse(0, 20, 1, List.of(job));

        ExternalJobDTO dto = HimalayasMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getDescription()).isEqualTo("Short excerpt.");
    }

    @Test
    void defaultsLocationToRemoteWorldwideWhenNoRestrictions() {
        HimalayasSearchResponse.HimalayasJob job = baseJob();
        job.setLocationRestrictions(List.of());
        HimalayasSearchResponse response = new HimalayasSearchResponse(0, 20, 1, List.of(job));

        ExternalJobDTO dto = HimalayasMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getLocation()).isEqualTo("Remote (worldwide)");
    }

    @Test
    void jobTypeDerivation() {
        assertThat(HimalayasMapper.toJobType("Full Time")).isEqualTo("FULL_TIME");
        assertThat(HimalayasMapper.toJobType("Part Time")).isEqualTo("PART_TIME");
        assertThat(HimalayasMapper.toJobType("Contractor")).isEqualTo("CONTRACT");
        assertThat(HimalayasMapper.toJobType("Temporary")).isEqualTo("CONTRACT");
        assertThat(HimalayasMapper.toJobType(null)).isEqualTo("FULL_TIME");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(HimalayasMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(HimalayasMapper.toExternalJobDtos(new HimalayasSearchResponse(0, 20, 0, null))).isEmpty();
    }

}
