package org.omnaphade.job_service.external.aidevjobs;

import org.junit.jupiter.api.Test;
import org.omnaphade.job_service.external.ExternalJobDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiDevJobsMapperTest {

    private AiDevJobsSearchResponse.AiDevJob job(String id, String title) {
        AiDevJobsSearchResponse.AiDevJob job = new AiDevJobsSearchResponse.AiDevJob();
        job.setId(id);
        job.setTitle(title);
        job.setCompanyName("Acme AI");
        job.setDescription("Job description.");
        job.setApplyUrl("https://boards.example.com/apply/" + id);
        return job;
    }

    @Test
    void mapsCoreFields() {
        AiDevJobsSearchResponse.AiDevJob job = job("abc-123", "ML Engineer");
        job.setLocation("Remote (US)");
        job.setSalaryMin(150000);
        job.setSalaryMax(200000);
        job.setJobType("full-time");
        AiDevJobsSearchResponse response = new AiDevJobsSearchResponse(List.of(job), true);

        ExternalJobDTO dto = AiDevJobsMapper.toExternalJobDtos(response).get(0);

        assertThat(dto.getExternalId()).isEqualTo("abc-123");
        assertThat(dto.getTitle()).isEqualTo("ML Engineer");
        assertThat(dto.getCompanyName()).isEqualTo("Acme AI");
        assertThat(dto.getLocation()).isEqualTo("Remote (US)");
        assertThat(dto.getSalaryMin()).isEqualTo(150000.0);
        assertThat(dto.getSalaryMax()).isEqualTo(200000.0);
        assertThat(dto.getJobType()).isEqualTo("FULL_TIME");
        assertThat(dto.getExternalUrl()).isEqualTo("https://boards.example.com/apply/abc-123");
    }

    @Test
    void locationFallsBackToWorkplaceThenNotSpecified() {
        AiDevJobsSearchResponse.AiDevJob remoteJob = job("id-1", "Remote Engineer");
        remoteJob.setWorkplace("remote");
        AiDevJobsSearchResponse.AiDevJob onsiteJob = job("id-2", "Onsite Engineer");
        onsiteJob.setWorkplace("onsite");
        AiDevJobsSearchResponse response = new AiDevJobsSearchResponse(List.of(remoteJob, onsiteJob), false);

        List<ExternalJobDTO> dtos = AiDevJobsMapper.toExternalJobDtos(response);

        assertThat(dtos.get(0).getLocation()).isEqualTo("Remote");
        assertThat(dtos.get(1).getLocation()).isEqualTo("Not specified");
    }

    @Test
    void urlFallsBackToJobPageWhenApplyUrlMissing() {
        AiDevJobsSearchResponse.AiDevJob job = job("id-1", "Engineer");
        job.setApplyUrl(null);
        AiDevJobsSearchResponse response = new AiDevJobsSearchResponse(List.of(job), false);

        assertThat(AiDevJobsMapper.toExternalJobDtos(response).get(0).getExternalUrl())
                .isEqualTo("https://aidevboard.com/job/id-1");
    }

    @Test
    void jobTypeDerivation() {
        assertThat(AiDevJobsMapper.toJobType("full-time")).isEqualTo("FULL_TIME");
        assertThat(AiDevJobsMapper.toJobType("part-time")).isEqualTo("PART_TIME");
        assertThat(AiDevJobsMapper.toJobType("contract")).isEqualTo("CONTRACT");
        assertThat(AiDevJobsMapper.toJobType("freelance")).isEqualTo("CONTRACT");
        assertThat(AiDevJobsMapper.toJobType(null)).isEqualTo("FULL_TIME");
    }

    @Test
    void emptyOrNullResponseYieldsNoJobs() {
        assertThat(AiDevJobsMapper.toExternalJobDtos(null)).isEmpty();
        assertThat(AiDevJobsMapper.toExternalJobDtos(new AiDevJobsSearchResponse(null, false))).isEmpty();
    }

}
