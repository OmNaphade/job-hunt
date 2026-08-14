package org.omnaphade.job_service.external.jobicy;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobicyMapperTest {

    private JobicySearchResponse.JobicyJob job(long id, String title) {
        JobicySearchResponse.JobicyJob job = new JobicySearchResponse.JobicyJob();
        job.setId(id);
        job.setJobTitle(title);
        job.setCompanyName("Jobicy Co");
        job.setJobDescription("Full description.");
        job.setUrl("https://jobicy.com/jobs/" + id);
        return job;
    }

    @Test
    void mapsCoreFieldsUsingIdAsExternalId() {
        JobicySearchResponse.JobicyJob job = job(42, "Frontend Engineer");
        job.setJobGeo("Europe");
        job.setJobType(List.of("Full-time"));
        job.setSalaryMin(80000.0);
        job.setSalaryMax(110000.0);
        JobicySearchResponse response = new JobicySearchResponse(List.of(job));

        ExternalJobDTO dto = JobicyMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("42");
        assertThat(dto.getTitle()).isEqualTo("Frontend Engineer");
        assertThat(dto.getCompanyName()).isEqualTo("Jobicy Co");
        assertThat(dto.getLocation()).isEqualTo("Europe");
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getSalaryMin()).isEqualTo(80000.0);
        assertThat(dto.getSalaryMax()).isEqualTo(110000.0);
        assertThat(dto.getExternalUrl()).isEqualTo("https://jobicy.com/jobs/42");
    }

    @Test
    void blankGeoFallsBackToRemoteSinceJobicyIsRemoteOnly() {
        JobicySearchResponse.JobicyJob job = job(1, "Remote Engineer");
        job.setJobGeo(null);
        JobicySearchResponse response = new JobicySearchResponse(List.of(job));

        assertThat(JobicyMapper.toExternalJobDtos(response).get(0).getLocation()).isEqualTo("Remote");
    }

    @Test
    void descriptionFallsBackToExcerptWhenMissing() {
        JobicySearchResponse.JobicyJob job = job(1, "Engineer");
        job.setJobDescription(null);
        job.setJobExcerpt("Short excerpt.");
        JobicySearchResponse response = new JobicySearchResponse(List.of(job));

        assertThat(JobicyMapper.toExternalJobDtos(response).get(0).getDescription()).isEqualTo("Short excerpt.");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(JobicyMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(JobicyMapper.toExternalJobDtos(new JobicySearchResponse(null))).isEmpty();
    }

}
