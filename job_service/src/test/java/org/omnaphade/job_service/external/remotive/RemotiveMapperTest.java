package org.omnaphade.job_service.external.remotive;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RemotiveMapperTest {

    private RemotiveSearchResponse.RemotiveJob job(long id, String title) {
        RemotiveSearchResponse.RemotiveJob job = new RemotiveSearchResponse.RemotiveJob();
        job.setId(id);
        job.setTitle(title);
        job.setCompanyName("Acme Remote");
        job.setDescription("Job description.");
        job.setUrl("https://remotive.com/remote-jobs/" + id);
        return job;
    }

    @Test
    void mapsCoreFieldsUsingIdAsExternalId() {
        RemotiveSearchResponse.RemotiveJob job = job(123, "Backend Engineer");
        job.setCandidateRequiredLocation("USA Only");
        job.setJobType("full_time");
        RemotiveSearchResponse response = new RemotiveSearchResponse(List.of(job));

        ExternalJobDTO dto = RemotiveMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("123");
        assertThat(dto.getTitle()).isEqualTo("Backend Engineer");
        assertThat(dto.getCompanyName()).isEqualTo("Acme Remote");
        assertThat(dto.getLocation()).isEqualTo("USA Only");
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://remotive.com/remote-jobs/123");
        assertThat(dto.getSalaryMin()).isNull();
        assertThat(dto.getSalaryMax()).isNull();
    }

    @Test
    void blankLocationFallsBackToRemoteSinceRemotiveIsRemoteOnly() {
        RemotiveSearchResponse.RemotiveJob job = job(1, "Remote Engineer");
        job.setCandidateRequiredLocation("");
        RemotiveSearchResponse response = new RemotiveSearchResponse(List.of(job));

        assertThat(RemotiveMapper.toExternalJobDtos(response).get(0).getLocation()).isEqualTo("Remote");
    }

    @Test
    void contractJobTypeIsNormalized() {
        RemotiveSearchResponse.RemotiveJob job = job(1, "Freelance Designer");
        job.setJobType("contract");
        RemotiveSearchResponse response = new RemotiveSearchResponse(List.of(job));

        assertThat(RemotiveMapper.toExternalJobDtos(response).get(0).getJobType()).isEqualTo("CONTRACT");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(RemotiveMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(RemotiveMapper.toExternalJobDtos(new RemotiveSearchResponse(null))).isEmpty();
    }

}
